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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.kustax.opendelivery.data.request.CreateDeviceRequest
import net.kustax.opendelivery.data.response.DeviceResponse
import net.kustax.opendelivery.data.response.OwnerWithStatsResponse
import net.kustax.opendelivery.data.response.PropertyResponse
import net.kustax.opendelivery.web.Screen
import net.kustax.opendelivery.web.api.ApiClient
import net.kustax.opendelivery.web.ui.ActiveBadge
import net.kustax.opendelivery.web.ui.ConfirmDialog
import net.kustax.opendelivery.web.ui.EmptyState
import net.kustax.opendelivery.web.ui.FormDialog
import net.kustax.opendelivery.web.ui.LoadingState
import net.kustax.opendelivery.web.ui.QrCodeDialog
import net.kustax.opendelivery.web.ui.SnackbarController
import net.kustax.opendelivery.web.util.formatTimestamp

@Composable
fun AdminDevicesScreen(client: ApiClient, onNavigate: (Screen) -> Unit) {
    var ownerStats by remember { mutableStateOf<List<OwnerWithStatsResponse>>(emptyList()) }
    var propertiesByOwner by remember { mutableStateOf<Map<String, List<PropertyResponse>>>(emptyMap()) }
    var devicesByProperty by remember { mutableStateOf<Map<String, List<DeviceResponse>>>(emptyMap()) }
    var expandedOwners by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var confirmDeleteDevice by remember { mutableStateOf<Pair<DeviceResponse, String>?>(null) } // device + schemaName
    // Triple: propertyId, propertyName, schemaName
    var createDeviceFor by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var newAdminDevice by remember { mutableStateOf<DeviceResponse?>(null) }

    val scope = rememberCoroutineScope()

    suspend fun loadAll() {
        try {
            val stats = client.getOwnerStats()
            ownerStats = stats
            expandedOwners = stats.map { it.owner.id }.toSet()

            val propMap = mutableMapOf<String, List<PropertyResponse>>()
            val deviceMap = mutableMapOf<String, List<DeviceResponse>>()

            for (os in stats) {
                val props = client.listPropertiesAdmin(os.owner.schemaName)
                propMap[os.owner.id] = props
                for (prop in props) {
                    deviceMap[prop.id] = client.listDevicesAdmin(prop.id, os.owner.schemaName)
                }
            }
            propertiesByOwner = propMap
            devicesByProperty = deviceMap
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    suspend fun reloadDevicesForProperty(propertyId: String, schemaName: String) {
        try {
            val updated = client.listDevicesAdmin(propertyId, schemaName)
            devicesByProperty = devicesByProperty.toMutableMap().also { it[propertyId] = updated }
        } catch (e: Exception) {
            error = e.message
        }
    }

    LaunchedEffect(Unit) { loadAll() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Devices",
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
                        val ownerProps = propertiesByOwner[ownerId] ?: emptyList()
                        val totalDevices = ownerProps.sumOf { (devicesByProperty[it.id] ?: emptyList()).size }
                        val isExpanded = ownerId in expandedOwners

                        item(key = "owner_header_$ownerId") {
                            DeviceOwnerHeader(
                                displayName = os.owner.displayName,
                                deviceCount = totalDevices,
                                isExpanded = isExpanded,
                                onToggle = {
                                    expandedOwners = if (isExpanded)
                                        expandedOwners - ownerId
                                    else
                                        expandedOwners + ownerId
                                }
                            )
                        }

                        if (isExpanded) {
                            ownerProps.forEach { prop ->
                                val propDevices = devicesByProperty[prop.id] ?: emptyList()

                                item(key = "prop_header_${prop.id}") {
                                    PropertySubHeader(
                                        propertyName = prop.name,
                                        deviceCount = propDevices.size,
                                        onAddDevice = {
                                            createDeviceFor = Triple(prop.id, prop.name, schemaName)
                                        },
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }

                                if (propDevices.isEmpty()) {
                                    item(key = "prop_empty_${prop.id}") {
                                        EmptyState(
                                            "No devices for this property",
                                            modifier = Modifier.padding(start = 32.dp)
                                        )
                                    }
                                } else {
                                    items(propDevices, key = { "device_${it.id}" }) { device ->
                                        AdminDeviceCard(
                                            device = device,
                                            onDelete = { confirmDeleteDevice = device to schemaName },
                                            modifier = Modifier.padding(start = 32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDeleteDevice?.let { (device, schemaName) ->
        val prop = propertiesByOwner.values.flatten().find { it.id == device.propertyId }
        ConfirmDialog(
            title = "Delete Device",
            message = "Delete \"${device.name}\"? This action is permanent and cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                scope.launch {
                    try {
                        client.deleteDevice(device.id)
                        confirmDeleteDevice = null
                        prop?.let { reloadDevicesForProperty(it.id, schemaName) }
                        SnackbarController.show("Device deleted")
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            },
            onDismiss = { confirmDeleteDevice = null }
        )
    }

    createDeviceFor?.let { (propertyId, propertyName, schemaName) ->
        AdminCreateDeviceDialog(
            propertyName = propertyName,
            onConfirm = { deviceName ->
                scope.launch {
                    runCatching {
                        client.createDeviceAdmin(
                            propertyId,
                            schemaName,
                            CreateDeviceRequest(name = deviceName)
                        )
                    }
                    .onSuccess { device ->
                        newAdminDevice = device
                        createDeviceFor = null
                        val ownerStat = ownerStats.find { it.owner.schemaName == schemaName }
                        ownerStat?.let { reloadDevicesForProperty(propertyId, schemaName) }
                        SnackbarController.show("Device created")
                    }
                    .onFailure {
                        error = it.message
                        createDeviceFor = null
                    }
                }
            },
            onDismiss = { createDeviceFor = null }
        )
    }

    newAdminDevice?.let { device ->
        if (device.activationCode != null) {
            QrCodeDialog(
                activationCode = device.activationCode!!,
                qrBase64 = device.qrCodeBase64,
                onDismiss = { newAdminDevice = null }
            )
        }
        // No QR dialog when activationCode is absent — state stays until next navigation
    }
}

@Composable
private fun DeviceOwnerHeader(
    displayName: String,
    deviceCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
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
                "$deviceCount device${if (deviceCount != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PropertySubHeader(
    propertyName: String,
    deviceCount: Int,
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            propertyName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "($deviceCount)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onAddDevice,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add device",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AdminCreateDeviceDialog(
    propertyName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var deviceName by remember { mutableStateOf("") }
    FormDialog(
        title = "New Device — $propertyName",
        onConfirm = { onConfirm(deviceName) },
        onDismiss = onDismiss,
        confirmEnabled = deviceName.isNotBlank(),
        confirmLabel = "Create Device"
    ) {
        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text("Device Name") },
            placeholder = { Text("e.g. Front Door Kiosk") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun AdminDeviceCard(
    device: DeviceResponse,
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
                Text(device.name, style = MaterialTheme.typography.titleSmall)

                if (device.activatedAt != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.extraSmall,
                            contentColor = Color(0xFF4CAF50)
                        ) {
                            Text(
                                "Activated",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            formatTimestamp(device.activatedAt ?: 0L),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFFFC107).copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.extraSmall,
                        contentColor = Color(0xFFE65100)
                    ) {
                        Text(
                            "Awaiting Activation",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                device.androidDeviceId?.let { id ->
                    Text(
                        "ID: ...${id.takeLast(8)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ActiveBadge(device.isActive)
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
