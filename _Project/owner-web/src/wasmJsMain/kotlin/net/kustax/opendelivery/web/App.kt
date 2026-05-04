package net.kustax.opendelivery.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.kustax.opendelivery.web.api.ApiClient
import net.kustax.opendelivery.web.auth.AuthSession
import net.kustax.opendelivery.web.auth.LoginScreen
import net.kustax.opendelivery.web.auth.SessionController
import net.kustax.opendelivery.web.screen.*
import net.kustax.opendelivery.web.ui.AppScaffold
import net.kustax.opendelivery.web.ui.OpenDeliveryTheme
import net.kustax.opendelivery.web.ui.SnackbarDisplay

@Composable
fun App() {
    OpenDeliveryTheme {
        var authSession by remember { mutableStateOf(AuthSession.load()) }
        
        val initialScreen = when {
            authSession == null -> Screen.Login
            authSession?.role == "PLATFORM_ADMIN" -> Screen.AdminDashboard
            else -> Screen.Dashboard
        }
        
        var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }
        val client = remember(authSession) { authSession?.let { ApiClient(ApiClient.BASE_URL, it.accessToken) } }

        fun onLogout() {
            AuthSession.clear()
            authSession = null
            currentScreen = Screen.Login
        }

        fun navigate(screen: Screen) {
            currentScreen = screen
        }

        val sessionExpired by SessionController.expired
        LaunchedEffect(sessionExpired) {
            if (sessionExpired) {
                AuthSession.clear()
                authSession = null
                currentScreen = Screen.Login
                SessionController.reset()
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (val screen = currentScreen) {
                Screen.Login -> LoginScreen(
                    onSuccess = { session ->
                        session.save()
                        authSession = session
                        currentScreen = if (session.role == "PLATFORM_ADMIN") Screen.AdminDashboard else Screen.Dashboard
                    }
                )
                Screen.Dashboard -> {
                    // Placeholder for Owner Dashboard
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("Owner Dashboard coming soon.")
                        Button(onClick = { onLogout() }, modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)) {
                            Text("Logout")
                        }
                    }
                }
                Screen.AdminDashboard -> AppScaffold(authSession!!, currentScreen, ::navigate, ::onLogout) {
                    AdminDashboardScreen(client!!)
                }
                Screen.OwnerList -> AppScaffold(authSession!!, currentScreen, ::navigate, ::onLogout) {
                    OwnerListScreen(client!!, onNavigate = ::navigate)
                }
                Screen.AdminProperties -> AppScaffold(authSession!!, currentScreen, ::navigate, ::onLogout) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("Properties Management coming soon.")
                    }
                }
                Screen.AdminProducts -> AppScaffold(authSession!!, currentScreen, ::navigate, ::onLogout) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("Products Management coming soon.")
                    }
                }
            }
            SnackbarDisplay()
        }
    }
}
