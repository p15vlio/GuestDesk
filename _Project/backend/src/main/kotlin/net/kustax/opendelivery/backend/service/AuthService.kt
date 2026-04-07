package net.kustax.opendelivery.backend.service

import net.kustax.opendelivery.backend.repository.platform.ExposedAuthRepository
import net.kustax.opendelivery.backend.repository.platform.RefreshTokenRecord
import net.kustax.opendelivery.backend.security.JwtConfig
import net.kustax.opendelivery.backend.security.PasswordHasher
import net.kustax.opendelivery.data.request.LoginRequest
import net.kustax.opendelivery.data.request.RefreshTokenRequest
import net.kustax.opendelivery.data.response.TokenResponse
import net.kustax.opendelivery.domain.enum.UserRole
import org.slf4j.LoggerFactory
import java.util.UUID

class AuthenticationException(message: String) : Exception(message)

class AuthService(
    private val authRepository: ExposedAuthRepository
) {
    private val log = LoggerFactory.getLogger(AuthService::class.java)

    suspend fun login(request: LoginRequest): TokenResponse {
        log.debug("Login attempt: email='{}' role='{}'", request.email, request.role)
        val role = parseRole(request.role)
        val authRecord = findUser(role, request.email, request.schemaName)
        log.debug("User lookup result for '{}': {}", request.email, if (authRecord != null) "found" else "not found")
        if (authRecord == null) throw AuthenticationException("Invalid credentials")

        if (!PasswordHasher.verify(request.password, authRecord.passwordHash)) {
            throw AuthenticationException("Invalid credentials")
        }

        return issueTokenPair(authRecord.id, authRecord.role, authRecord.schemaName)
    }

    suspend fun refresh(request: RefreshTokenRequest): TokenResponse {
        val tokenHash = PasswordHasher.hashForLookup(request.refreshToken)
        val record = authRepository.findRefreshToken(tokenHash)
            ?: throw AuthenticationException("Invalid or expired refresh token")

        if (record.expiresAt < System.currentTimeMillis()) {
            authRepository.deleteRefreshToken(tokenHash)
            throw AuthenticationException("Invalid or expired refresh token")
        }

        // Rotate: delete old token before issuing a new pair
        authRepository.deleteRefreshToken(tokenHash)

        val role = UserRole.valueOf(record.role)
        return issueTokenPair(record.userId, role, record.schemaName)
    }

    private fun parseRole(roleString: String): UserRole =
        runCatching { UserRole.valueOf(roleString) }
            .getOrElse { throw AuthenticationException("Invalid credentials") }

    private suspend fun findUser(role: UserRole, email: String, schemaName: String?) =
        when (role) {
            UserRole.PLATFORM_ADMIN -> authRepository.findPlatformAdminByEmail(email)
            UserRole.OWNER -> authRepository.findOwnerByEmail(email)
            UserRole.STORE_PARTNER -> {
                val schema = schemaName ?: throw AuthenticationException("Invalid credentials")
                authRepository.findPartnerStoreByEmail(email, schema)
            }
            UserRole.RIDER -> {
                val schema = schemaName ?: throw AuthenticationException("Invalid credentials")
                authRepository.findRiderByEmail(email, schema)
            }
        }

    private suspend fun issueTokenPair(userId: String, role: UserRole, schemaName: String?): TokenResponse {
        val accessToken = JwtConfig.generateAccessToken(userId, role, schemaName)
        val expiresAt = System.currentTimeMillis() + JwtConfig.ACCESS_TOKEN_TTL_MS

        val rawRefreshToken = UUID.randomUUID().toString()
        val refreshTokenHash = PasswordHasher.hashForLookup(rawRefreshToken)
        val refreshExpiresAt = System.currentTimeMillis() + REFRESH_TOKEN_TTL_MS

        authRepository.saveRefreshToken(
            RefreshTokenRecord(
                id = UUID.randomUUID().toString(),
                userId = userId,
                role = role.name,
                tokenHash = refreshTokenHash,
                schemaName = schemaName,
                expiresAt = refreshExpiresAt,
                createdAt = System.currentTimeMillis()
            )
        )

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = rawRefreshToken,
            role = role.name,
            expiresAt = expiresAt
        )
    }

    companion object {
        private const val REFRESH_TOKEN_TTL_MS = 30L * 24 * 60 * 60 * 1000
    }
}
