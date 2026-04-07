package net.kustax.opendelivery.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import net.kustax.opendelivery.domain.enum.UserRole
import java.util.Date

object JwtConfig {

    private val secret = System.getenv("JWT_SECRET") ?: "dev-secret-change-in-production"

    const val ISSUER = "opendelivery"
    const val AUDIENCE = "opendelivery-api"
    const val ACCESS_TOKEN_TTL_MS = 15 * 60 * 1000L

    const val CLAIM_ROLE = "role"
    const val CLAIM_SCHEMA = "schemaName"

    fun generateAccessToken(userId: String, role: UserRole, schemaName: String?): String =
        JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withSubject(userId)
            .withClaim(CLAIM_ROLE, role.name)
            .withClaim(CLAIM_SCHEMA, schemaName)
            .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS))
            .sign(Algorithm.HMAC256(secret))

    fun verifier(): JWTVerifier =
        JWT.require(Algorithm.HMAC256(secret))
            .withIssuer(ISSUER)
            .build()
}
