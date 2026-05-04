package net.kustax.opendelivery.web.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.kustax.opendelivery.web.Screen
import net.kustax.opendelivery.web.auth.AuthSession

@Composable
fun AppScaffold(
    authSession: AuthSession,
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "GuestDesk Admin",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp, start = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )

            NavigationItem(
                label = "Dashboard",
                icon = Icons.Default.Dashboard,
                selected = currentScreen is Screen.AdminDashboard,
                onClick = { onNavigate(Screen.AdminDashboard) }
            )
            NavigationItem(
                label = "Owners",
                icon = Icons.Default.Business,
                selected = currentScreen is Screen.OwnerList,
                onClick = { onNavigate(Screen.OwnerList) }
            )
            NavigationItem(
                label = "Properties",
                icon = Icons.Default.Home,
                selected = currentScreen is Screen.AdminProperties,
                onClick = { onNavigate(Screen.AdminProperties) }
            )
            NavigationItem(
                label = "Products",
                icon = Icons.Default.Inventory,
                selected = currentScreen is Screen.AdminProducts,
                onClick = { onNavigate(Screen.AdminProducts) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                authSession.role,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 12.dp)
            )
            
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Logout")
            }
        }

        // Main Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun NavigationItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
