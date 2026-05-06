package net.kustax.opendelivery.web.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.kustax.opendelivery.data.request.CreatePropertyRequest
import net.kustax.opendelivery.data.request.UpdatePropertyRequest
import net.kustax.opendelivery.data.response.PropertyResponse
import net.kustax.opendelivery.web.api.ApiClient
import net.kustax.opendelivery.web.ui.ActiveBadge
import net.kustax.opendelivery.web.ui.ConfirmDialog
import net.kustax.opendelivery.web.ui.FormDialog
import net.kustax.opendelivery.web.ui.SnackbarController
import net.kustax.opendelivery.web.util.toLabel

private val FULFILLMENT_MODELS = listOf("OWN_INFRASTRUCTURE", "EXTERNAL_SERVICE", "HYBRID")

@Composable
fun PropertyListScreen(
    client: ApiClient,
    onNavigateToDevices: (propertyId: String, propertyName: String) -> Unit,
    onNavigateToOrders: (propertyId: String, propertyName: String) -> Unit
) {
    var properties by remember { mutableStateOf(emptyList<PropertyResponse>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingProperty by remember { mutableStateOf<PropertyResponse?>(null) }
    var propertyToDelete by remember { mutableStateOf<PropertyResponse?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        try {
            properties = client.listProperties()
        } catch (e: Exception) {
            errorMessage = e.message
        }
    }

    LaunchedEffect(Unit) { reload() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Text(
                "Properties",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(properties) { property ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(0.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(property.name, style = MaterialTheme.typography.titleMedium)
                                        ActiveBadge(property.isActive)
                                    }
                                    Text(
                                        "${property.streetName} ${property.streetNo}, ${property.postalCode} ${property.area ?: property.city}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    // Fulfillment model pill badge
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE0F2FE), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            property.fulfillmentModel.toLabel(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF0C4A6E)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider()
                            // Action buttons row at the bottom
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onNavigateToDevices(property.id, property.name) },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) { Text("Devices", style = MaterialTheme.typography.labelMedium) }
                                OutlinedButton(
                                    onClick = { onNavigateToOrders(property.id, property.name) },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) { Text("Orders", style = MaterialTheme.typography.labelMedium) }
                                OutlinedButton(
                                    onClick = { editingProperty = property },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) { Text("Edit", style = MaterialTheme.typography.labelMedium) }
                                OutlinedButton(
                                    onClick = { propertyToDelete = property },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                                ) { Text("Delete", style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Property")
        }
    }

    if (showCreateDialog) {
        PropertyFormDialog(
            title = "Create Property",
            onConfirm = { request ->
                scope.launch {
                    try {
                        client.createProperty(request)
                        showCreateDialog = false
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

    editingProperty?.let { existing ->
        PropertyFormDialog(
            title = "Edit Property",
            initialName = existing.name,
            initialStreetName = existing.streetName,
            initialStreetNo = existing.streetNo,
            initialPostalCode = existing.postalCode,
            initialArea = existing.area ?: "",
            initialLevel = existing.level,
            initialNameOnDoorbell = existing.nameOnDoorbell,
            initialContactPhone = existing.contactPhone,
            initialFulfillmentModel = existing.fulfillmentModel,
            initialNotes = existing.notes ?: "",
            onConfirm = { req ->
                scope.launch {
                    try {
                        client.updateProperty(
                            existing.id,
                            UpdatePropertyRequest(
                                name = req.name,
                                streetName = req.streetName,
                                streetNo = req.streetNo,
                                postalCode = req.postalCode,
                                area = req.area,
                                level = req.level,
                                nameOnDoorbell = req.nameOnDoorbell,
                                contactPhone = req.contactPhone,
                                fulfillmentModel = req.fulfillmentModel,
                                notes = req.notes
                            )
                        )
                        editingProperty = null
                        reload()
                    } catch (e: Exception) {
                        errorMessage = e.message
                    }
                }
            },
            onDismiss = { editingProperty = null },
            confirmLabel = "Save"
        )
    }

    propertyToDelete?.let { property ->
        ConfirmDialog(
            title = "Delete Property",
            message = "Delete \"${property.name}\"? This action is permanent and cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                scope.launch {
                    runCatching { client.deleteProperty(property.id) }
                        .onSuccess {
                            SnackbarController.show("Property deleted")
                            reload()
                        }
                        .onFailure { errorMessage = it.message }
                    propertyToDelete = null
                }
            },
            onDismiss = { propertyToDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertyFormDialog(
    title: String,
    initialName: String = "",
    initialStreetName: String = "",
    initialStreetNo: String = "",
    initialPostalCode: String = "",
    initialArea: String = "",
    initialLevel: Int = 0,
    initialNameOnDoorbell: String = "",
    initialContactPhone: String = "",
    initialFulfillmentModel: String = "OWN_INFRASTRUCTURE",
    initialNotes: String = "",
    onConfirm: (CreatePropertyRequest) -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Confirm"
) {
    var name by remember { mutableStateOf(initialName) }
    var streetName by remember { mutableStateOf(initialStreetName) }
    var streetNo by remember { mutableStateOf(initialStreetNo) }
    var postalCode by remember { mutableStateOf(initialPostalCode) }
    var area by remember { mutableStateOf(initialArea) }
    var levelText by remember { mutableStateOf(if (initialLevel == 0) "" else initialLevel.toString()) }
    var nameOnDoorbell by remember { mutableStateOf(initialNameOnDoorbell) }
    var contactPhone by remember { mutableStateOf(initialContactPhone) }
    var fulfillmentModel by remember { mutableStateOf(initialFulfillmentModel) }
    var notes by remember { mutableStateOf(initialNotes) }
    var modelMenuExpanded by remember { mutableStateOf(false) }

    FormDialog(
        title = title,
        onConfirm = {
            onConfirm(
                CreatePropertyRequest(
                    name = name,
                    streetName = streetName,
                    streetNo = streetNo,
                    postalCode = postalCode,
                    area = area.takeIf { it.isNotBlank() },
                    level = levelText.toIntOrNull() ?: 0,
                    nameOnDoorbell = nameOnDoorbell,
                    contactPhone = contactPhone,
                    fulfillmentModel = fulfillmentModel,
                    notes = notes.takeIf { it.isNotBlank() }
                )
            )
        },
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank() && streetName.isNotBlank() && streetNo.isNotBlank()
            && postalCode.isNotBlank() && nameOnDoorbell.isNotBlank() && contactPhone.isNotBlank(),
        confirmLabel = confirmLabel
    ) {
        // Row 1: Property Name — full width
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Property Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Row 2: Street (weight 2) + No. (weight 1)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = streetName,
                onValueChange = { streetName = it },
                label = { Text("Street") },
                modifier = Modifier.weight(2f),
                singleLine = true
            )
            OutlinedTextField(
                value = streetNo,
                onValueChange = { streetNo = it },
                label = { Text("No.") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        // Row 3: Postal Code + Area
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = postalCode,
                onValueChange = { postalCode = it },
                label = { Text("Postal Code") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = area,
                onValueChange = { area = it },
                label = { Text("Area (optional)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        // Row 4: Floor (weight 1) + Name on Doorbell (weight 2)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = levelText,
                onValueChange = { levelText = it },
                label = { Text("Floor") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = nameOnDoorbell,
                onValueChange = { nameOnDoorbell = it },
                label = { Text("Name on Doorbell") },
                modifier = Modifier.weight(2f),
                singleLine = true
            )
        }

        // Row 5: Contact Phone — full width
        OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            label = { Text("Contact Phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Section divider
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                "FULFILLMENT & NOTES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        // Row 6: Fulfillment Model dropdown — full width
        ExposedDropdownMenuBox(
            expanded = modelMenuExpanded,
            onExpandedChange = { modelMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = fulfillmentModel.toLabel(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fulfillment Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = modelMenuExpanded,
                onDismissRequest = { modelMenuExpanded = false }
            ) {
                FULFILLMENT_MODELS.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.toLabel()) },
                        onClick = { fulfillmentModel = model; modelMenuExpanded = false }
                    )
                }
            }
        }

        // Row 7: Notes — full width, multi-line
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
    }
}
