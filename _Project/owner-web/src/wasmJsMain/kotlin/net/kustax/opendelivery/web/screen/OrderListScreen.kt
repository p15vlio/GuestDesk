package net.kustax.opendelivery.web.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.kustax.opendelivery.data.response.OrderResponse
import net.kustax.opendelivery.data.response.PaymentResponse
import net.kustax.opendelivery.web.api.ApiClient
import net.kustax.opendelivery.web.ui.BreadcrumbBar
import net.kustax.opendelivery.web.ui.EmptyState
import net.kustax.opendelivery.web.ui.LoadingState
import net.kustax.opendelivery.web.ui.StatusChip
import net.kustax.opendelivery.web.util.formatEuros
import net.kustax.opendelivery.web.util.formatTimestamp
import net.kustax.opendelivery.web.util.toLabel

private val ALL_STATUSES = listOf("All", "PENDING", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED")

private fun validNextStatuses(current: String): List<String> = when (current) {
    "PENDING" -> listOf("CONFIRMED", "CANCELLED")
    "CONFIRMED" -> listOf("PREPARING", "CANCELLED")
    "PREPARING" -> listOf("OUT_FOR_DELIVERY", "CANCELLED")
    "OUT_FOR_DELIVERY" -> listOf("DELIVERED", "CANCELLED")
    else -> emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    client: ApiClient,
    propertyId: String,
    propertyName: String,
    onBack: () -> Unit = {}
) {
    var orders by remember { mutableStateOf<List<OrderResponse>?>(null) }
    var selectedStatus by remember { mutableStateOf("All") }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var orderForUpdate by remember { mutableStateOf<OrderResponse?>(null) }
    // Keyed by orderId; present-but-null means "fetched, no payment recorded"
    var paymentByOrderId by remember { mutableStateOf<Map<String, PaymentResponse?>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        try {
            orders = client.listOrders(
                propertyId = propertyId,
                status = selectedStatus.takeIf { it != "All" }
            )
        } catch (e: Exception) {
            errorMessage = e.message
        }
    }

    LaunchedEffect(selectedStatus) { reload() }

    Column(modifier = Modifier.fillMaxSize()) {
        BreadcrumbBar(listOf(
            "Properties" to onBack,
            propertyName to null
        ))

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Orders", style = MaterialTheme.typography.headlineSmall)
                Text(
                    propertyName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ExposedDropdownMenuBox(
                expanded = statusMenuExpanded,
                onExpandedChange = { statusMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (selectedStatus == "All") "All" else selectedStatus.toLabel(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filter") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusMenuExpanded) },
                    modifier = Modifier.width(200.dp).menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = statusMenuExpanded,
                    onDismissRequest = { statusMenuExpanded = false }
                ) {
                    ALL_STATUSES.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(if (s == "All") "All" else s.toLabel()) },
                            onClick = { selectedStatus = s; statusMenuExpanded = false }
                        )
                    }
                }
            }
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        when {
            orders == null -> LoadingState()
            orders!!.isEmpty() -> EmptyState("No orders found")
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(orders!!) { order ->
                        var expanded by remember { mutableStateOf(false) }

                        LaunchedEffect(expanded) {
                            if (expanded && !paymentByOrderId.containsKey(order.id)) {
                                val payment = try {
                                    client.getPayment(order.id)
                                } catch (_: Exception) {
                                    null
                                }
                                paymentByOrderId = paymentByOrderId + (order.id to payment)
                            }
                        }

                        OrderCard(
                            order = order,
                            expanded = expanded,
                            payment = paymentByOrderId[order.id],
                            paymentFetched = paymentByOrderId.containsKey(order.id),
                            onToggleExpand = { expanded = !expanded },
                            onUpdateClick = { orderForUpdate = order }
                        )
                    }
                }
            }
        }
    }

    orderForUpdate?.let { order ->
        val nextStatuses = validNextStatuses(order.status)
        AlertDialog(
            onDismissRequest = { orderForUpdate = null },
            title = { Text("Update Order Status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusChip(order.status)
                    Spacer(Modifier.height(8.dp))
                    nextStatuses.forEach { next ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        client.updateOrderStatus(order.id, next)
                                        orderForUpdate = null
                                        reload()
                                    } catch (e: Exception) {
                                        errorMessage = e.message
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Set to ${next.toLabel()}") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { orderForUpdate = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun OrderCard(
    order: OrderResponse,
    expanded: Boolean,
    payment: PaymentResponse?,
    paymentFetched: Boolean,
    onToggleExpand: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "#${order.id.takeLast(8).uppercase()}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        StatusChip(order.status)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(order.type.toLabel(), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        order.guestEmail?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(formatTimestamp(order.createdAt), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        formatEuros(order.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (validNextStatuses(order.status).isNotEmpty()) {
                        TextButton(
                            onClick = onUpdateClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) { Text("Update", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }

            if (expanded) {
                if (order.items.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    item.productName,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${item.quantity} × ${formatEuros(item.unitPrice)} = ${formatEuros(item.subtotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
                            "Payment · ${payment.method.toLabel()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            payment.status.toLabel(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = when (payment.status) {
                                "COMPLETED" -> MaterialTheme.colorScheme.tertiary
                                "FAILED" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    else -> Text(
                        "Loading payment…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
