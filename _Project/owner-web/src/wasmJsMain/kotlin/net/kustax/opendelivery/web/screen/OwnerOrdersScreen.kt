package net.kustax.opendelivery.web.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.kustax.opendelivery.data.response.OrderResponse
import net.kustax.opendelivery.data.response.PaymentResponse
import net.kustax.opendelivery.data.response.PropertyResponse
import net.kustax.opendelivery.web.api.ApiClient
import net.kustax.opendelivery.web.ui.EmptyState
import net.kustax.opendelivery.web.ui.LoadingState
import net.kustax.opendelivery.web.ui.SnackbarController
import net.kustax.opendelivery.web.ui.StatusChip
import net.kustax.opendelivery.web.util.formatEuros
import net.kustax.opendelivery.web.util.formatTimestamp
import net.kustax.opendelivery.web.util.toLabel

private val ORDER_STATUSES = listOf(
    "PENDING", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"
)

private val NEXT_STATUSES = mapOf(
    "PENDING" to listOf("CONFIRMED", "CANCELLED"),
    "CONFIRMED" to listOf("PREPARING", "CANCELLED"),
    "PREPARING" to listOf("OUT_FOR_DELIVERY", "CANCELLED"),
    "OUT_FOR_DELIVERY" to listOf("DELIVERED", "CANCELLED"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerOrdersScreen(client: ApiClient) {
    var properties by remember { mutableStateOf<List<PropertyResponse>?>(null) }
    var allOrders by remember { mutableStateOf<List<Pair<OrderResponse, String>>?>(null) }
    var selectedPropertyId by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var propertyMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var expandedOrderId by remember { mutableStateOf<String?>(null) }
    var orderForUpdate by remember { mutableStateOf<OrderResponse?>(null) }
    // Keyed by orderId; present-but-null means "fetched, no payment recorded"
    var paymentByOrderId by remember { mutableStateOf<Map<String, PaymentResponse?>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun loadAll() {
        try {
            val props = client.listProperties()
            properties = props
            val pairs = coroutineScope {
                props.map { prop ->
                    async {
                        client.listOrders(prop.id, null).map { it to prop.name }
                    }
                }.awaitAll().flatten()
            }
            allOrders = pairs.sortedByDescending { it.first.createdAt }
        } catch (e: Exception) {
            error = e.message
        }
    }

    LaunchedEffect(Unit) { loadAll() }

    Column(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Orders", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = {
                scope.launch {
                    allOrders = null
                    paymentByOrderId = emptyMap()
                    loadAll()
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = propertyMenuExpanded,
                onExpandedChange = { propertyMenuExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = properties?.find { it.id == selectedPropertyId }?.name ?: "All Properties",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Property") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(propertyMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = propertyMenuExpanded,
                    onDismissRequest = { propertyMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Properties") },
                        onClick = { selectedPropertyId = null; propertyMenuExpanded = false }
                    )
                    properties?.forEach { prop ->
                        DropdownMenuItem(
                            text = { Text(prop.name) },
                            onClick = { selectedPropertyId = prop.id; propertyMenuExpanded = false }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = statusMenuExpanded,
                onExpandedChange = { statusMenuExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedStatus?.toLabel() ?: "All Statuses",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = statusMenuExpanded,
                    onDismissRequest = { statusMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Statuses") },
                        onClick = { selectedStatus = null; statusMenuExpanded = false }
                    )
                    ORDER_STATUSES.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.toLabel()) },
                            onClick = { selectedStatus = s; statusMenuExpanded = false }
                        )
                    }
                }
            }
        }

        val filtered = allOrders?.filter { (order, _) ->
            (selectedPropertyId == null || order.propertyId == selectedPropertyId) &&
                (selectedStatus == null || order.status == selectedStatus)
        }

        when {
            allOrders == null -> LoadingState()
            filtered.isNullOrEmpty() -> EmptyState("No orders found")
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.first.id }) { (order, propertyName) ->
                        val isExpanded = expandedOrderId == order.id
                        // Fetch payment the first time this card is expanded
                        LaunchedEffect(isExpanded) {
                            if (isExpanded && !paymentByOrderId.containsKey(order.id)) {
                                val payment = try {
                                    client.getPayment(order.id)
                                } catch (_: Exception) {
                                    null
                                }
                                paymentByOrderId = paymentByOrderId + (order.id to payment)
                            }
                        }
                        OwnerOrderCard(
                            order = order,
                            propertyName = propertyName,
                            isExpanded = isExpanded,
                            payment = paymentByOrderId[order.id],
                            paymentFetched = paymentByOrderId.containsKey(order.id),
                            onToggleExpand = {
                                expandedOrderId = if (expandedOrderId == order.id) null else order.id
                            },
                            onUpdateStatus = { orderForUpdate = order }
                        )
                    }
                }
            }
        }
    }

    orderForUpdate?.let { order ->
        val nextStatuses = NEXT_STATUSES[order.status] ?: emptyList()
        AlertDialog(
            onDismissRequest = { orderForUpdate = null },
            title = { Text("Update Order Status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current status:", style = MaterialTheme.typography.bodyMedium)
                    StatusChip(order.status)
                    Spacer(Modifier.height(8.dp))
                    if (nextStatuses.isEmpty()) {
                        Text(
                            "No further transitions available.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        nextStatuses.forEach { next ->
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { client.updateOrderStatus(order.id, next) }
                                            .onSuccess {
                                                SnackbarController.show("Status updated to ${next.toLabel()}")
                                                allOrders = null
                                                paymentByOrderId = emptyMap()
                                                loadAll()
                                            }
                                            .onFailure { SnackbarController.show("Failed: ${it.message}") }
                                        orderForUpdate = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("-> ${next.toLabel()}") }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { orderForUpdate = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun OwnerOrderCard(
    order: OrderResponse,
    propertyName: String,
    isExpanded: Boolean,
    payment: PaymentResponse?,
    paymentFetched: Boolean,
    onToggleExpand: () -> Unit,
    onUpdateStatus: () -> Unit
) {
    val hasNextStatus = NEXT_STATUSES.containsKey(order.status)
    Card(
        onClick = onToggleExpand,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "#${order.id.takeLast(8).uppercase()}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        propertyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        order.type.toLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    order.guestEmail?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        formatTimestamp(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(order.status)
                    Text(
                        formatEuros(order.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (hasNextStatus) {
                        FilledTonalButton(
                            onClick = onUpdateStatus,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Update", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (isExpanded) {
                if (order.items.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${item.quantity}x ${item.productName}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                formatEuros(item.subtotal),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                when {
                    payment == null && paymentFetched -> Text(
                        "No payment recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    payment != null -> Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Payment: ${payment.method.toLabel()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            payment.status.toLabel(),
                            style = MaterialTheme.typography.bodySmall,
                            color = when (payment.status) {
                                "COMPLETED" -> MaterialTheme.colorScheme.tertiary
                                "FAILED" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    else -> Text(
                        "Loading payment...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
