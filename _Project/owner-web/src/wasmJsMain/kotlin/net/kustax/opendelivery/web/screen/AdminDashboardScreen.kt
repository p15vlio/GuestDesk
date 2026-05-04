package net.kustax.opendelivery.web.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kustax.opendelivery.data.response.OwnerWithStatsResponse
import net.kustax.opendelivery.web.api.ApiClient
import net.kustax.opendelivery.web.ui.ActiveBadge
import net.kustax.opendelivery.web.util.formatEuros
import net.kustax.opendelivery.web.util.formatTimestamp

@Composable
fun AdminDashboardScreen(client: ApiClient) {
    var ownerStats by remember { mutableStateOf(emptyList<OwnerWithStatsResponse>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try { ownerStats = client.getOwnerStats() } catch (e: Exception) { errorMessage = e.message }
    }

    val totalOwners = ownerStats.size
    val activeOwners = ownerStats.count { it.owner.isActive }
    val totalProperties = ownerStats.sumOf { it.propertyCount }
    val totalProducts = ownerStats.sumOf { it.productCount }
    val totalRevenueCents = ownerStats.sumOf { it.owner.subscriptionPriceCents }
    val recentOwners = ownerStats.sortedByDescending { it.owner.createdAt }.take(5)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Admin Dashboard", style = MaterialTheme.typography.headlineSmall)
        }

        errorMessage?.let {
            item {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Total Owners", totalOwners.toString(), Modifier.weight(1f))
                StatCard("Active Owners", activeOwners.toString(), Modifier.weight(1f), accent = Color(0xFF10B981))
                StatCard("Properties", totalProperties.toString(), Modifier.weight(1f))
                StatCard("Products", totalProducts.toString(), Modifier.weight(1f))
                StatCard("Annual Revenue", formatEuros(totalRevenueCents), Modifier.weight(1f), accent = MaterialTheme.colorScheme.secondary)
            }
        }

        item {
            Text("Recent Owners", style = MaterialTheme.typography.titleMedium)
        }

        if (recentOwners.isEmpty()) {
            item {
                Text(
                    "No owners yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(recentOwners) { item ->
                val owner = item.owner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(0.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(owner.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                owner.contactEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${item.propertyCount} properties · ${item.productCount} products",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ActiveBadge(owner.isActive)
                            Text(
                                formatTimestamp(owner.createdAt).take(10),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, accent: Color? = null) {
    val accentColor = accent ?: MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = accentColor
                )
            }
        }
    }
}
