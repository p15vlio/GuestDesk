package net.kustax.opendelivery.web.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.kustax.opendelivery.data.response.ProductResponse
import net.kustax.opendelivery.data.response.PropertyResponse
import net.kustax.opendelivery.web.api.ApiClient
import net.kustax.opendelivery.web.ui.ActiveBadge
import net.kustax.opendelivery.web.ui.BreadcrumbBar
import net.kustax.opendelivery.web.ui.EmptyState
import net.kustax.opendelivery.web.ui.LoadingState
import net.kustax.opendelivery.web.util.formatEuros
import net.kustax.opendelivery.web.util.toLabel

private val OWNER_VIEW_TABS = listOf("Dashboard", "Properties", "Products", "Orders")

@Composable
fun AdminOwnerViewScreen(
    client: ApiClient,
    ownerId: String,
    schemaName: String,
    displayName: String,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        BreadcrumbBar(listOf(
            "Owners" to onBack,
            displayName to null
        ))

        // Sticky tenant banner
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column {
                    Text(
                        "Viewing tenant: $displayName",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Schema: $schemaName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            OWNER_VIEW_TABS.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (selectedTab) {
                0 -> OwnerViewDashboardTab(client = client, schemaName = schemaName)
                1 -> OwnerViewPropertiesTab(client = client, schemaName = schemaName)
                2 -> OwnerViewProductsTab(client = client, schemaName = schemaName)
                3 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.widthIn(max = 480.dp).padding(24.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                "Order access is restricted to the owner",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "The backend restricts GET /orders to the OWNER role only. " +
                                "To view and manage orders for this tenant, the owner must log in with their own credentials.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerViewDashboardTab(client: ApiClient, schemaName: String) {
    var properties by remember { mutableStateOf<List<PropertyResponse>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(schemaName) {
        try {
            properties = client.listPropertiesAdmin(schemaName)
        } catch (e: Exception) {
            error = e.message
        }
    }

    when {
        properties == null && error == null -> LoadingState()
        error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
        else -> {
            val props = properties!!
            val active = props.count { it.isActive }
            val inactive = props.size - active

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Tenant Summary", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TenantStatCard("Total Properties", props.size.toString(), Modifier.weight(1f))
                    TenantStatCard("Active", active.toString(), Modifier.weight(1f))
                    TenantStatCard("Inactive", inactive.toString(), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OwnerViewPropertiesTab(client: ApiClient, schemaName: String) {
    var properties by remember { mutableStateOf<List<PropertyResponse>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(schemaName) {
        try {
            properties = client.listPropertiesAdmin(schemaName)
        } catch (e: Exception) {
            error = e.message
        }
    }

    when {
        properties == null && error == null -> LoadingState()
        error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
        properties!!.isEmpty() -> EmptyState("No properties for this tenant")
        else -> {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(properties!!, key = { it.id }) { property ->
                    ReadOnlyPropertyCard(property)
                }
            }
        }
    }
}

@Composable
private fun OwnerViewProductsTab(client: ApiClient, schemaName: String) {
    var products by remember { mutableStateOf<List<ProductResponse>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(schemaName) {
        try {
            products = client.listProductsAdmin(schemaName)
        } catch (e: Exception) {
            error = e.message
        }
    }

    when {
        products == null && error == null -> LoadingState()
        error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
        products!!.isEmpty() -> EmptyState("No products for this tenant")
        else -> {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(products!!, key = { it.id }) { product ->
                    ReadOnlyProductCard(product)
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyPropertyCard(property: PropertyResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(property.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "${property.streetName} ${property.streetNo}, ${property.postalCode} ${property.area ?: property.city}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ActiveBadge(property.isActive)
                Text(
                    property.fulfillmentModel.toLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReadOnlyProductCard(product: ProductResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${product.category.toLabel()} · ${product.source.toLabel()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(formatEuros(product.basePrice), style = MaterialTheme.typography.bodyMedium)
            }
            ActiveBadge(product.isAvailable)
        }
    }
}

@Composable
private fun TenantStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
