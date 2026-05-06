package net.kustax.opendelivery.web.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.kustax.opendelivery.data.request.CreateProductRequest
import net.kustax.opendelivery.data.request.UpdateProductRequest
import net.kustax.opendelivery.data.response.ProductResponse
import net.kustax.opendelivery.web.api.ApiClient
import net.kustax.opendelivery.web.ui.ConfirmDialog
import net.kustax.opendelivery.web.ui.EmptyState
import net.kustax.opendelivery.web.ui.FormDialog
import net.kustax.opendelivery.web.ui.LoadingState
import net.kustax.opendelivery.web.ui.SnackbarController
import net.kustax.opendelivery.web.util.centsToInputString
import net.kustax.opendelivery.web.util.formatEuros
import net.kustax.opendelivery.web.util.toLabel

private val PRODUCT_CATEGORIES = listOf(
    "BEVERAGE", "HOT_DRINK", "FOOD", "SNACK", "ALCOHOL", "PERSONAL_CARE", "PHARMACY", "OTHER"
)
private val PRODUCT_SOURCES = listOf("OWN_STOCK", "PARTNER_STORE", "EXTERNAL_PLATFORM")

@Composable
fun ProductListScreen(client: ApiClient) {
    var products by remember { mutableStateOf<List<ProductResponse>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductResponse?>(null) }
    var productToDelete by remember { mutableStateOf<ProductResponse?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        try {
            products = client.listProducts()
        } catch (e: Exception) {
            errorMessage = e.message
        }
    }

    LaunchedEffect(Unit) { reload() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Text(
                "Products",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }

            when {
                products == null -> LoadingState()
                products!!.isEmpty() -> EmptyState("No products yet")
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(products!!) { product ->
                            ProductCard(
                                product = product,
                                onToggleAvailability = { isAvailable ->
                                    // Optimistic update: flip locally first
                                    products = products?.map {
                                        if (it.id == product.id) it.copy(isAvailable = isAvailable) else it
                                    }
                                    scope.launch {
                                        try {
                                            client.toggleAvailability(product, isAvailable)
                                        } catch (e: Exception) {
                                            // Revert on failure
                                            products = products?.map {
                                                if (it.id == product.id) it.copy(isAvailable = !isAvailable) else it
                                            }
                                            errorMessage = e.message
                                        }
                                    }
                                },
                                onEdit = { editingProduct = product },
                                onDelete = { productToDelete = product }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Product")
        }
    }

    if (showCreateDialog) {
        ProductFormDialog(
            title = "Create Product",
            onConfirm = { request ->
                scope.launch {
                    try {
                        client.createProduct(request)
                        showCreateDialog = false
                        SnackbarController.show("Product created")
                        reload()
                    } catch (e: Exception) {
                        errorMessage = e.message
                    }
                }
            },
            onDismiss = { showCreateDialog = false },
            confirmLabel = "Create"
        )
    }

    editingProduct?.let { existing ->
        ProductFormDialog(
            title = "Edit Product",
            initialName = existing.name,
            initialDescription = existing.description,
            initialCategory = existing.category,
            initialSource = existing.source,
            initialPrice = centsToInputString(existing.basePrice),
            onConfirm = { req ->
                scope.launch {
                    try {
                        client.updateProduct(
                            existing.id,
                            UpdateProductRequest(
                                name = req.name,
                                description = req.description,
                                basePrice = req.basePrice,
                                imageUrl = req.imageUrl,
                                isAvailable = existing.isAvailable
                            )
                        )
                        editingProduct = null
                        SnackbarController.show("Product updated")
                        reload()
                    } catch (e: Exception) {
                        errorMessage = e.message
                    }
                }
            },
            onDismiss = { editingProduct = null },
            confirmLabel = "Save"
        )
    }

    productToDelete?.let { product ->
        ConfirmDialog(
            title = "Delete Product",
            message = "Delete \"${product.name}\"? This action is permanent and cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                scope.launch {
                    runCatching { client.deleteProduct(product.id) }
                        .onSuccess {
                            SnackbarController.show("Product deleted")
                            products = client.listProducts()
                        }
                        .onFailure { SnackbarController.show("Failed: ${it.message}") }
                    productToDelete = null
                }
            },
            onDismiss = { productToDelete = null }
        )
    }
}

@Composable
private fun ProductCard(
    product: ProductResponse,
    onToggleAvailability: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    product.category.toLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    product.source.toLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    formatEuros(product.basePrice),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Available", style = MaterialTheme.typography.labelSmall)
                Switch(
                    checked = product.isAvailable,
                    onCheckedChange = { onToggleAvailability(it) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onEdit) { Text("Edit") }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete product",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormDialog(
    title: String,
    initialName: String = "",
    initialDescription: String = "",
    initialCategory: String = "FOOD",
    initialSource: String = "OWN_STOCK",
    initialPrice: String = "",
    onConfirm: (CreateProductRequest) -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Confirm"
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var category by remember { mutableStateOf(initialCategory) }
    var source by remember { mutableStateOf(initialSource) }
    var priceText by remember { mutableStateOf(initialPrice) }
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
        confirmLabel = confirmLabel
    ) {
        // Row 1: Name — full width
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Product Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Row 2: Price + Category side by side
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Price (€)") },
                modifier = Modifier.weight(1f),
                isError = priceText.isNotEmpty() && !priceValid,
                singleLine = true
            )
            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it },
                modifier = Modifier.weight(1f)
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
                    PRODUCT_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.toLabel()) },
                            onClick = { category = cat; categoryMenuExpanded = false }
                        )
                    }
                }
            }
        }

        // Row 3: Source — full width
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
                PRODUCT_SOURCES.forEach { src ->
                    DropdownMenuItem(
                        text = { Text(src.toLabel()) },
                        onClick = { source = src; sourceMenuExpanded = false }
                    )
                }
            }
        }

        // Row 4: Description — full width, multi-line
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
    }
}
