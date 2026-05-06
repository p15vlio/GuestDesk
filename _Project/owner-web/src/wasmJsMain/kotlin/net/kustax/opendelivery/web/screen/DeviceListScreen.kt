package net.kustax.opendelivery.web.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.browser.window
import kotlinx.coroutines.launch
import net.kustax.opendelivery.data.request.CreateDeviceRequest
import net.kustax.opendelivery.data.response.DeviceResponse
import net.kustax.opendelivery.web.api.ApiClient
import net.kustax.opendelivery.web.ui.ActiveBadge
import net.kustax.opendelivery.web.ui.BreadcrumbBar
import net.kustax.opendelivery.web.ui.ConfirmDialog
import net.kustax.opendelivery.web.ui.FormDialog
import net.kustax.opendelivery.web.ui.QrCodeDialog
import net.kustax.opendelivery.web.ui.SnackbarController
import net.kustax.opendelivery.web.util.formatTimestamp

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double
private fun jsNow(): Long = jsDateNow().toLong()

private enum class OnlineStatus { ONLINE, RECENTLY, OFFLINE, NEVER }

private fun deviceOnlineStatus(lastSeenAt: Long?): OnlineStatus {
    if (lastSeenAt == null) return OnlineStatus.NEVER
    val diffMs = jsNow() - lastSeenAt
    return when {
        diffMs < 5 * 60 * 1000L -> OnlineStatus.ONLINE
        diffMs < 60 * 60 * 1000L -> OnlineStatus.RECENTLY
        else -> OnlineStatus.OFFLINE
    }
}

@Composable
private fun OnlineIndicator(lastSeenAt: Long?) {
    val status = deviceOnlineStatus(lastSeenAt)
    val (color, label) = when (status) {
        OnlineStatus.ONLINE -> MaterialTheme.colorScheme.tertiary to "Online"
        OnlineStatus.RECENTLY -> Color(0xFFF59E0B) to "Recently seen"
        OnlineStatus.OFFLINE -> MaterialTheme.colorScheme.error to "Offline"
        OnlineStatus.NEVER -> MaterialTheme.colorScheme.onSurfaceVariant to "Never connected"
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, shape = CircleShape)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun DeviceListScreen(client: ApiClient, propertyId: String, propertyName: String, onBack: () -> Unit = {}) {
    var devices by remember { mutableStateOf(emptyList<DeviceResponse>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deviceToDelete by remember { mutableStateOf<DeviceResponse?>(null) }
    var newlyCreatedDevice by remember { mutableStateOf<DeviceResponse?>(null) }
    var showCodeDevice by remember { mutableStateOf<DeviceResponse?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        try { devices = client.listDevices(propertyId) } catch (e: Exception) { errorMessage = e.message }
    }

    LaunchedEffect(Unit) { reload() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            BreadcrumbBar(listOf(
                "Properties" to onBack,
                propertyName to null
            ))
            Spacer(Modifier.height(8.dp))
            Text("Devices", style = MaterialTheme.typography.headlineSmall)
            Text(
                propertyName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices) { device ->
                    DeviceCard(
                        device = device,
                        onDelete = { deviceToDelete = device },
                        onShowCode = { showCodeDevice = device }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Device")
        }
    }

    if (showCreateDialog) {
        CreateDeviceDialog(
            onConfirm = { request ->
                scope.launch {
                    try {
                        val response = client.createDevice(propertyId, request)
                        newlyCreatedDevice = response
                        showCreateDialog = false
                        reload()
                    } catch (e: Exception) {
                        errorMessage = e.message
                    }
                }
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    newlyCreatedDevice?.let { device ->
        if (device.activationCode != null) {
            QrCodeDialog(
                activationCode = device.activationCode!!,
                qrBase64 = device.qrCodeBase64,
                onDismiss = { newlyCreatedDevice = null }
            )
        }
    }

    showCodeDevice?.let { device ->
        if (device.activationCode != null) {
            QrCodeDialog(
                activationCode = device.activationCode!!,
                qrBase64 = device.qrCodeBase64,
                onDismiss = { showCodeDevice = null }
            )
        }
    }

    deviceToDelete?.let { device ->
        ConfirmDialog(
            title = "Delete Device",
            message = "Delete \"${device.name}\"? This action is permanent and cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                deviceToDelete = null
                scope.launch {
                    try {
                        client.deleteDevice(device.id)
                        SnackbarController.show("Device deleted")
                        reload()
                    } catch (e: Exception) { errorMessage = e.message }
                }
            },
            onDismiss = { deviceToDelete = null }
        )
    }
}

// amber color for "not yet activated" label
private val AmberWarning = Color(0xFFF9A825)

@Composable
private fun DeviceCard(device: DeviceResponse, onDelete: () -> Unit, onShowCode: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete device",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            OnlineIndicator(device.lastSeenAt)

            // Activation code shown prominently while device is not yet activated
            device.activationCode?.let { code ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Activation Code",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                code,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        IconButton(
                            onClick = { window.navigator.clipboard.writeText(code) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy activation code",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Android ID — show copy button only when non-null; otherwise show warning
            val androidId = device.androidDeviceId
            if (androidId != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Android ID: $androidId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { window.navigator.clipboard.writeText(androidId) },
                        modifier = Modifier.size(20.dp).padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Android ID",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Not yet activated",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning
                    )
                    // Show Code only relevant while activationCode is still present
                    if (device.activationCode != null) {
                        OutlinedButton(
                            onClick = onShowCode,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Show Code", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Activation timestamp — only shown after activation
            val activatedAt = device.activatedAt
            if (activatedAt != null) {
                Text(
                    "Activated: ${formatTimestamp(activatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ActiveBadge(device.isActive)
        }
    }
}

@Composable
private fun CreateDeviceDialog(
    onConfirm: (CreateDeviceRequest) -> Unit,
    onDismiss: () -> Unit
) {
    var deviceName by remember { mutableStateOf("") }

    FormDialog(
        title = "Add Device",
        onConfirm = {
            // androidDeviceId is null — will be set during QR activation
            onConfirm(CreateDeviceRequest(name = deviceName))
        },
        onDismiss = onDismiss,
        confirmEnabled = deviceName.isNotBlank(),
        confirmLabel = "Add"
    ) {
        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text("Device Name") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
