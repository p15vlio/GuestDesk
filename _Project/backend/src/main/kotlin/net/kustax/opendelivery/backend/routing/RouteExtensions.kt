package net.kustax.opendelivery.backend.routing

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.Payload
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.header
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import net.kustax.opendelivery.backend.database.TenantContext
import net.kustax.opendelivery.backend.security.JwtConfig
import net.kustax.opendelivery.backend.service.AuthenticationException
import net.kustax.opendelivery.domain.enum.UserRole

// Resolves JWT payload from either the Ktor principal (inside authenticate("jwt"))
// or by decoding the Authorization header manually (outside authenticate("jwt")).
// Note: manual decode skips signature verification — acceptable for MVP dev server.
private fun ApplicationCall.jwtPayload(): Payload {
    principal<JWTPrincipal>()?.payload?.let { return it }
    val token = request.header("Authorization")?.removePrefix("Bearer ")?.trim()
        ?: throw AuthenticationException("Missing authorization token")
    return try {
        JWT.decode(token)
    } catch (e: Exception) {
        throw AuthenticationException("Invalid authorization token")
    }
}

fun ApplicationCall.userRole(): UserRole {
    val raw = jwtPayload().getClaim(JwtConfig.CLAIM_ROLE).asString()
        ?: throw AuthenticationException("Missing role claim in token")
    return enumValueOf(raw)
}

fun ApplicationCall.tenantSchema(): String? {
    return try {
        val claimSchema = jwtPayload().getClaim(JwtConfig.CLAIM_SCHEMA).asString()?.ifBlank { null }
        // PLATFORM_ADMIN JWTs carry no schemaName claim — allow override via query param so
        // admins can browse any tenant's data from the panel.
        if (claimSchema == null && userRole() == UserRole.PLATFORM_ADMIN) {
            request.queryParameters["schemaName"]?.ifBlank { null }
        } else {
            claimSchema
        }
    } catch (_: AuthenticationException) {
        null
    }
}

suspend fun ApplicationCall.withTenant(block: suspend () -> Unit) {
    val schema = tenantSchema() ?: throw AuthenticationException("No tenant schema in token")
    withContext(TenantContext(schema) + currentCoroutineContext()) { block() }
}
