package net.kustax.opendelivery.web.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.kustax.opendelivery.data.request.CreateProductRequest
import net.kustax.opendelivery.data.response.OwnerWithStatsResponse
import net.kustax.opendelivery.data.response.ProductResponse
import net.kustax.opendelivery.web.Screen
import net.kustax.opendelivery.web.api.ApiClient
import net.kustax.opendelivery.web.ui.ActiveBadge
import net.kustax.opendelivery.web.ui.ConfirmDialog
import net.kustax.opendelivery.web.ui.EmptyState
import net.kustax.opendelivery.web.ui.FormDialog
import net.kustax.opendelivery.web.ui.LoadingState
import net.kustax.opendelivery.web.ui.SnackbarController
import net.kustax.opendelivery.web.util.formatEuros
import net.kustax.opendelivery.web.util.toLabel

@Composable
fun AdminProductsScreen(client: ApiClient, onNavigate: (Screen) -> Unit) {
    var ownerStats by remember { mutableStateOf<List<OwnerWithStatsResponse>>(emptyList()) }
    var productsByOwner by remember { mutableStateOf<Map<String, List<ProductResponse>>>(emptyMap()) }
    var expandedOwners by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var confirmDeleteProduct by remember { mutableStateOf<Pair<ProductResponse, String>?>(null) } // product + schemaName
    var createForOwner by remember { mutableStateOf<OwnerWithStatsResponse?>(null) }

    val scope = rememberCoroutineScope()

    suspend fun loadAll() {
        try {
            val stats = client.getOwnerStats()
            ownerStats = stats
            expandedOwners = stats.map { it.owner.id }.toSet()
            val prodMap = mutableMapOf<String, List<ProductResponse>>()
            for (os in stats) {
                prodMap[os.owner.id] = client.listProductsAdmin(os.owner.schemaName)
            }
            productsByOwner = prodMap
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    suspend fun reloadProductsForOwner(ownerId: String, schemaName: String) {
        try {
            val updated = client.listProductsAdmin(schemaName)
            productsByOwner = productsByOwner.toMutableMap().also { it[ownerId] = updated }
        } catch (e: Exception) {
            error = e.message
        }
    }

    LaunchedEffect(Unit) { loadAll() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Products",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        when {
            loading -> LoadingState()
            ownerStats.isEmpty() -> EmptyState("No owners found")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ownerStats.forEach { os ->
                        val ownerId = os.owner.id
                        val schemaName = os.owner.schemaName
                        val products = productsByOwner[ownerId] ?: emptyList()
                        val isExpanded = ownerId in expandedOwners

                        item(key = "owner_header_$ownerId") {
                            ProductOwnerHeader(
                                displayName = os.owner.displayName,
                                productCount = products.size,
                                isExpanded = isExpanded,
                                onToggle = {
                                    expandedOwners = if (isExpanded)
                                        expandedOwners - ownerId
                                    else
                                        expandedOwners + ownerId
                                },
                                onAddProduct = { createForOwner = os }
                            )
                        }

                        if (isExpanded) {
                            if (products.isEmpty()) {
                                item(key = "empty_$ownerId") {
                                    EmptyState(
                                        "No products for this owner",
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            } else {
                                items(products, key = { "product_${it.id}" }) { product ->
                                    AdminProductCard(
                                        product = product,
                                        onToggleAvailability = { newValue ->
                                            scope.launch {
                                                // optimistic update
                                                val optimisticList = (productsByOwner[ownerId] ?: emptyList())
                                                    .map { if (it.id == product.id) it.copy(isAvailable = newValue) else it }
                                                productsByOwner = productsByOwner.toMutableMap()
                                                    .also { it[ownerId] = optimisticList }
                                                try {
                                                    client.toggleAvailabilityAdmin(schemaName, product, newValue)
                                                } catch (e: Exception) {
                                                    // revert on error
                                                    reloadProductsForOwner(ownerId, schemaName)
                                                    error = e.message
                                                }
                                            }
                                        },
                                        onDelete = { confirmDeleteProduct = product to schemaName },
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDeleteProduct?.let { (product, schemaName) ->
        val ownerStat = ownerStats.find { it.owner.schemaName == schemaName }
        ConfirmDialog(
            title = "Delete Product",
            message = "Delete \"${product.name}\"? This action is permanent and cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                scope.launch {
                    try {
                        client.deleteProductAdmin(product.id, schemaName)
                        confirmDeleteProduct = null
                        ownerStat?.let { reloadProductsForOwner(it.owner.id, schemaName) }
                        SnackbarController.show("Product deleted")
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            },
            onDismiss = { confirmDeleteProduct = null }
        )
    }

    createForOwner?.let { os ->
        AdminProductFormDialog(
            title = "New Product — ${os.owner.displayName}",
            onConfirm = { request ->
                scope.launch {
                    runCatching { client.createProductAdmin(os.owner.schemaName, request) }
                        .onSuccess {
                            SnackbarController.show("Product created")
                            reloadProductsForOwner(os.owner.id, os.owner.schemaName)
                            createForOwner = null
                        }
                        .onFailure {
                            error = it.message
                            createForOwner = null
                        }
                }
            },
            onDismiss = { createForOwner = null }
        )
    }
}

@Composable
private fun ProductOwnerHeader(
    displayName: String,
    productCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddProduct: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(displayName, style = MaterialTheme.typography.titleSmall)
            Text(
                "($productCount)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onAddProduct,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add product",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private val ADMIN_PRODUCT_CATEGORIES = listOf(
    "BEVERAGE", "HOT_DRINK", "FOOD", "SNACK", "ALCOHOL", "PERSONAL_CARE", "PHARMACY", "OTHER"
)
private val ADMIN_PRODUCT_SOURCES = listOf("OWN_STOCK", "PARTNER_STORE", "EXTERNAL_PLATFORM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminProductFormDialog(
    title: String,
    onConfirm: (CreateProductRequest) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("FOOD") }
    var source by remember { mutableStateOf("OWN_STOCK") }
    var priceText by remember { mutableStateOf("") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var sourceMenuExpanded by remember { mutableStateOf(false) }

    val priceValid = priceText.toDoubleOrNull() != null && priceText.toDouble() >= 0

    FormDialog(
        title = title,
        onConfirm = {
            val priceInCents = (priceText.toDouble() * 100).toLong()
            onConfirm(CreateProductRequest(name, description, category, source, priceInCents, null))
        },
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank() && priceValid,
        confirmLabel = "Create Product"
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Price (€)") },
            modifier = Modifier.fillMaxWidth(),
            isError = priceText.isNotEmpty() && !priceValid
        )
        ExposedDropdownMenuBox(
            expanded = categoryMenuExpanded,
            onExpandedChange = { categoryMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = category.toLabel(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = categoryMenuExpanded,
                onDismissRequest = { categoryMenuExpanded = false }
            ) {
                ADMIN_PRODUCT_CATEGORIES.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.toLabel()) },
                        onClick = { category = cat; categoryMenuExpanded = false }
                    )
                }
            }
        }
        ExposedDropdownMenuBox(
            expanded = sourceMenuExpanded,
            onExpandedChange = { sourceMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = source.toLabel(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Source") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceMenuExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = sourceMenuExpanded,
                onDismissRequest = { sourceMenuExpanded = false }
            ) {
                ADMIN_PRODUCT_SOURCES.forEach { src ->
                    DropdownMenuItem(
                        text = { Text(src.toLabel()) },
                        onClick = { source = src; sourceMenuExpanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminProductCard(
    product: ProductResponse,
    onToggleAvailability: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(product.category.toLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(product.source.toLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                Text(formatEuros(product.basePrice), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                ActiveBadge(product.isAvailable)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Available", style = MaterialTheme.typography.labelSmall)
                Switch(
                    checked = product.isAvailable,
                    onCheckedChange = onToggleAvailability
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
