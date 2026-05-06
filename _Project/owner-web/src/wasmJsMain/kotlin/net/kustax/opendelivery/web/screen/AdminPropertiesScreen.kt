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
import net.kustax.opendelivery.data.request.CreatePropertyRequest
import net.kustax.opendelivery.data.request.UpdatePropertyRequest
import net.kustax.opendelivery.data.response.OwnerWithStatsResponse
import net.kustax.opendelivery.data.response.PropertyResponse
import net.kustax.opendelivery.web.Screen
import net.kustax.opendelivery.web.api.ApiClient
import net.kustax.opendelivery.web.ui.ActiveBadge
import net.kustax.opendelivery.web.ui.ConfirmDialog
import net.kustax.opendelivery.web.ui.EmptyState
import net.kustax.opendelivery.web.ui.FormDialog
import net.kustax.opendelivery.web.ui.LoadingState
import net.kustax.opendelivery.web.ui.SnackbarController
import net.kustax.opendelivery.web.util.toLabel

private val FULFILLMENT_MODELS = listOf("OWN_INFRASTRUCTURE", "EXTERNAL_SERVICE", "HYBRID")

@Composable
fun AdminPropertiesScreen(client: ApiClient, onNavigate: (Screen) -> Unit) {
    var ownerStats by remember { mutableStateOf<List<OwnerWithStatsResponse>>(emptyList()) }
    var propertiesByOwner by remember { mutableStateOf<Map<String, List<PropertyResponse>>>(emptyMap()) }
    var expandedOwners by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    var addingPropertyForOwner by remember { mutableStateOf<OwnerWithStatsResponse?>(null) }
    var editingProperty by remember { mutableStateOf<Pair<PropertyResponse, String>?>(null) } // property + schemaName
    var confirmDeleteProperty by remember { mutableStateOf<Pair<PropertyResponse, String>?>(null) } // property + schemaName

    val scope = rememberCoroutineScope()

    suspend fun loadAll() {
        try {
            val stats = client.getOwnerStats()
            ownerStats = stats
            expandedOwners = stats.map { it.owner.id }.toSet()
            val propMap = mutableMapOf<String, List<PropertyResponse>>()
            for (os in stats) {
                propMap[os.owner.id] = client.listPropertiesAdmin(os.owner.schemaName)
            }
            propertiesByOwner = propMap
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    suspend fun reloadPropertiesForOwner(ownerId: String, schemaName: String) {
        try {
            val updated = client.listPropertiesAdmin(schemaName)
            propertiesByOwner = propertiesByOwner.toMutableMap().also { it[ownerId] = updated }
        } catch (e: Exception) {
            error = e.message
        }
    }

    LaunchedEffect(Unit) { loadAll() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Properties", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search properties...") },
                singleLine = true,
                modifier = Modifier.width(240.dp)
            )
        }

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
                        val ownerProps = propertiesByOwner[ownerId] ?: emptyList()
                        val filteredProps = if (search.isBlank()) ownerProps
                            else ownerProps.filter {
                                it.name.contains(search, ignoreCase = true) ||
                                "${it.streetName} ${it.streetNo}".contains(search, ignoreCase = true)
                            }
                        val isExpanded = ownerId in expandedOwners

                        item(key = "header_$ownerId") {
                            OwnerSectionHeader(
                                displayName = os.owner.displayName,
                                propertyCount = ownerProps.size,
                                isExpanded = isExpanded,
                                onToggle = {
                                    expandedOwners = if (isExpanded)
                                        expandedOwners - ownerId
                                    else
                                        expandedOwners + ownerId
                                },
                                onAddProperty = { addingPropertyForOwner = os }
                            )
                        }

                        if (isExpanded) {
                            if (filteredProps.isEmpty()) {
                                item(key = "empty_$ownerId") {
                                    EmptyState(
                                        if (search.isBlank()) "No properties for this owner"
                                        else "No matching properties",
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            } else {
                                items(filteredProps, key = { "prop_${it.id}" }) { property ->
                                    AdminPropertyCard(
                                        property = property,
                                        onEdit = { editingProperty = property to schemaName },
                                        onDelete = { confirmDeleteProperty = property to schemaName },
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

    addingPropertyForOwner?.let { os ->
        AdminPropertyFormDialog(
            title = "Create Property for ${os.owner.displayName}",
            confirmLabel = "Create",
            onConfirm = { request ->
                scope.launch {
                    try {
                        client.createPropertyAdmin(os.owner.schemaName, request)
                        addingPropertyForOwner = null
                        reloadPropertiesForOwner(os.owner.id, os.owner.schemaName)
                        SnackbarController.show("Property created")
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            },
            onDismiss = { addingPropertyForOwner = null }
        )
    }

    editingProperty?.let { (property, schemaName) ->
        val ownerStat = ownerStats.find { it.owner.schemaName == schemaName }
        AdminPropertyFormDialog(
            title = "Edit Property",
            confirmLabel = "Save",
            initialName = property.name,
            initialStreetName = property.streetName,
            initialStreetNo = property.streetNo,
            initialPostalCode = property.postalCode,
            initialArea = property.area ?: "",
            initialLevel = property.level,
            initialNameOnDoorbell = property.nameOnDoorbell,
            initialContactPhone = property.contactPhone,
            initialFulfillmentModel = property.fulfillmentModel,
            initialNotes = property.notes ?: "",
            onConfirm = { request ->
                scope.launch {
                    try {
                        client.updatePropertyAdmin(
                            property.id,
                            schemaName,
                            UpdatePropertyRequest(
                                name = request.name,
                                streetName = request.streetName,
                                streetNo = request.streetNo,
                                postalCode = request.postalCode,
                                area = request.area,
                                level = request.level,
                                nameOnDoorbell = request.nameOnDoorbell,
                                contactPhone = request.contactPhone,
                                fulfillmentModel = request.fulfillmentModel,
                                notes = request.notes
                            )
                        )
                        editingProperty = null
                        ownerStat?.let { reloadPropertiesForOwner(it.owner.id, schemaName) }
                        SnackbarController.show("Property updated")
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            },
            onDismiss = { editingProperty = null }
        )
    }

    confirmDeleteProperty?.let { (property, schemaName) ->
        val ownerStat = ownerStats.find { it.owner.schemaName == schemaName }
        ConfirmDialog(
            title = "Delete Property",
            message = "Delete \"${property.name}\"? This action is permanent and cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                scope.launch {
                    try {
                        client.deleteProperty(property.id)
                        confirmDeleteProperty = null
                        ownerStat?.let { reloadPropertiesForOwner(it.owner.id, schemaName) }
                        SnackbarController.show("Property deleted")
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            },
            onDismiss = { confirmDeleteProperty = null }
        )
    }
}

@Composable
private fun OwnerSectionHeader(
    displayName: String,
    propertyCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddProperty: () -> Unit
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    "($propertyCount)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onAddProperty,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Property", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun AdminPropertyCard(
    property: PropertyResponse,
    onEdit: () -> Unit,
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
                Text(property.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${property.streetName} ${property.streetNo}, ${property.postalCode} ${property.area ?: property.city}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    property.fulfillmentModel.toLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                ActiveBadge(property.isActive)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminPropertyFormDialog(
    title: String,
    confirmLabel: String,
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
    onDismiss: () -> Unit
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
        confirmLabel = confirmLabel,
        confirmEnabled = name.isNotBlank() && streetName.isNotBlank() && streetNo.isNotBlank()
            && postalCode.isNotBlank() && nameOnDoorbell.isNotBlank() && contactPhone.isNotBlank(),
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
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Property Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = streetName,
            onValueChange = { streetName = it },
            label = { Text("Street Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = streetNo,
                onValueChange = { streetNo = it },
                label = { Text("Street No.") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = postalCode,
                onValueChange = { postalCode = it },
                label = { Text("Postal Code") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = area,
                onValueChange = { area = it },
                label = { Text("Area (optional)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = levelText,
                onValueChange = { levelText = it },
                label = { Text("Floor") },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = nameOnDoorbell,
            onValueChange = { nameOnDoorbell = it },
            label = { Text("Name on Doorbell") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            label = { Text("Contact Phone") },
            modifier = Modifier.fillMaxWidth()
        )
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
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
