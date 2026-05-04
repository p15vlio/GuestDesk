package net.kustax.opendelivery.web.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.kustax.opendelivery.data.request.LoginRequest
import net.kustax.opendelivery.web.api.ApiClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onSuccess: (AuthSession) -> Unit) {
    var role by remember { mutableStateOf("OWNER") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var roleMenuExpanded by remember { mutableStateOf(false) }
    var backendOnline by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        backendOnline = ApiClient.unauthenticated().checkHealth()
    }

    fun doLogin() {
        if (loading || email.isBlank() || password.isBlank()) return
        scope.launch {
            loading = true
            errorMessage = null
            try {
                val response = ApiClient.unauthenticated().login(
                    LoginRequest(
                        email = email,
                        password = password,
                        role = role,
                        schemaName = null
                    )
                )
                onSuccess(
                    AuthSession(
                        accessToken = response.accessToken,
                        refreshToken = response.refreshToken,
                        role = response.role,
                        schemaName = null
                    )
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Login failed"
            } finally {
                loading = false
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                when {
                    event.key == Key.Tab && event.type == KeyEventType.KeyDown -> {
                        if (event.isShiftPressed) focusManager.moveFocus(FocusDirection.Previous)
                        else focusManager.moveFocus(FocusDirection.Next)
                        true
                    }
                    else -> false
                }
            }
    ) {
        // Left branding panel (40% width)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.4f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            Color(0xFF0D9488)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(40.dp)
            ) {
                Text(
                    "Guest Desk",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Digital Ordering & Services\nfor Short-Term Rentals",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(40.dp))
                Text(
                    "© 2026 GuestDesk S.A.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Right form panel (60% width)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.6f)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 400.dp).padding(40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Page heading
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sign in to your account", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Enter your credentials to access the management console.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Backend health indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(8.dp).background(
                            color = if (backendOnline == true) Color(0xFF10B981) else Color(0xFFEF4444),
                            shape = CircleShape
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when (backendOnline) {
                            true -> "Backend online"
                            false -> "Backend offline"
                            null -> "Checking…"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Role dropdown
                ExposedDropdownMenuBox(
                    expanded = roleMenuExpanded,
                    onExpandedChange = { roleMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (role == "PLATFORM_ADMIN") "Platform Admin" else "Owner",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = roleMenuExpanded,
                        onDismissRequest = { roleMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Owner") },
                            onClick = { role = "OWNER"; roleMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Platform Admin") },
                            onClick = { role = "PLATFORM_ADMIN"; roleMenuExpanded = false }
                        )
                    }
                }

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Password field with visibility toggle
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { doLogin() }),
                    modifier = Modifier.fillMaxWidth()
                )

                // Error message
                errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Sign in button
                Button(
                    onClick = { doLogin() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(if (loading) "Signing in…" else "Sign In")
                }

                // Dev shortcut — fills credentials for quick testing
                TextButton(
                    onClick = {
                        email = "admin@guestdesk.io"
                        password = "Admin@GuestDesk1"
                        role = "PLATFORM_ADMIN"
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        "Dev > Admin",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
