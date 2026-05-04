package net.kustax.opendelivery.web.screen

import androidx.compose.foundation.BorderStroke
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
import net.kustax.opendelivery.web.Screen
import net.kustax.opendelivery.data.request.CreateOwnerRequest
import net.kustax.opendelivery.data.request.UpdateOwnerRequest
import net.kustax.opendelivery.data.response.OwnerResponse
import net.kustax.opendelivery.data.response.OwnerWithStatsResponse
import net.kustax.opendelivery.web.api.ApiClient
import net.kustax.opendelivery.web.ui.ActiveBadge
import net.kustax.opendelivery.web.ui.ConfirmDialog
import net.kustax.opendelivery.web.ui.FormDialog
import net.kustax.opendelivery.web.ui.SnackbarController
import net.kustax.opendelivery.web.util.centsToInputString
import net.kustax.opendelivery.web.util.formatTimestamp

@Composable
fun OwnerListScreen(client: ApiClient, onNavigate: (Screen) -> Unit = {}) {
    var ownerStats by remember { mutableStateOf(emptyList<OwnerWithStatsResponse>()) }
    var search by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingOwner by remember { mutableStateOf<OwnerResponse?>(null) }
    var ownerToDelete by remember { mutableStateOf<OwnerResponse?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        try { ownerStats = client.getOwnerStats() } catch (e: Exception) { errorMessage = e.message }
    }

    LaunchedEffect(Unit) { reload() }

    val filtered = if (search.isBlank()) ownerStats
        else ownerStats.filter {
            it.owner.displayName.contains(search, ignoreCase = true) ||
            it.owner.contactEmail.contains(search, ignoreCase = true)
        }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Owners", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search owners...") },
                    singleLine = true,
                    modifier = Modifier.width(220.dp)
                )
                Button(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Owner")
                }
            }
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { item ->
                val owner = item.owner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(0.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(owner.displayName, style = MaterialTheme.typography.titleMedium)
                                ActiveBadge(owner.isActive)
                            }
                            Text(
                                owner.contactEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "VAT: ${owner.vatId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp)) {
                                Text("${item.propertyCount} Properties", style = MaterialTheme.typography.labelMedium)
                                Text("${item.productCount} Products", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { editingOwner = owner }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { ownerToDelete = owner }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateOwnerDialog(
            onConfirm = { request ->
                scope.launch {
                    try {
                        client.createOwner(request)
                        showCreateDialog = false
                        SnackbarController.show("Owner created")
                        reload()
                    }
                    catch (e: Exception) { errorMessage = e.message }
                }
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    editingOwner?.let { owner ->
        EditOwnerDialog(
            owner = owner,
            onConfirm = { request ->
                scope.launch {
                    try {
                        client.updateOwner(owner.id, request)
                        editingOwner = null
                        SnackbarController.show("Owner updated")
                        reload()
                    }
                    catch (e: Exception) { errorMessage = e.message }
                }
            },
            onDismiss = { editingOwner = null }
        )
    }

    ownerToDelete?.let { owner ->
        ConfirmDialog(
            title = "Delete Owner",
            message = "Permanently delete ${owner.displayName}? This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                ownerToDelete = null
                scope.launch {
                    try {
                        client.deleteOwner(owner.id)
                        SnackbarController.show("Owner deleted")
                        reload()
                    } catch (e: Exception) { errorMessage = e.message }
                }
            },
            onDismiss = { ownerToDelete = null }
        )
    }
}

@Composable
private fun CreateOwnerDialog(onConfirm: (CreateOwnerRequest) -> Unit, onDismiss: () -> Unit) {
    var ownerType by remember { mutableStateOf("COMPANY") }
    var companyName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var vatId by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var subscriptionPriceInput by remember { mutableStateOf("49.00") }

    val isCompany = ownerType == "COMPANY"

    FormDialog(
        title = "Create Owner",
        onConfirm = {
            val priceCents = ((subscriptionPriceInput.toDoubleOrNull() ?: 0.0) * 100).toLong()
            onConfirm(
                CreateOwnerRequest(
                    ownerType = ownerType,
                    firstName = firstName.takeIf { !isCompany },
                    lastName = lastName.takeIf { !isCompany },
                    companyName = companyName.takeIf { isCompany },
                    vatId = vatId,
                    contactEmail = contactEmail,
                    contactPhone = contactPhone,
                    subscriptionPriceCents = priceCents
                )
            )
        },
        onDismiss = onDismiss,
        confirmLabel = "Create"
    ) {
        OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = vatId, onValueChange = { vatId = it }, label = { Text("VAT ID") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = contactEmail, onValueChange = { contactEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = subscriptionPriceInput, onValueChange = { subscriptionPriceInput = it }, label = { Text("Subscription Price") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun EditOwnerDialog(owner: OwnerResponse, onConfirm: (UpdateOwnerRequest) -> Unit, onDismiss: () -> Unit) {
    var companyName by remember { mutableStateOf(owner.companyName ?: "") }
    var contactPhone by remember { mutableStateOf(owner.contactPhone) }
    var subscriptionPriceInput by remember { mutableStateOf(centsToInputString(owner.subscriptionPriceCents)) }

    FormDialog(
        title = "Edit Owner",
        onConfirm = {
            val priceCents = ((subscriptionPriceInput.toDoubleOrNull() ?: 0.0) * 100).toLong()
            onConfirm(
                UpdateOwnerRequest(
                    companyName = companyName.takeIf { it.isNotBlank() },
                    contactPhone = contactPhone,
                    subscriptionPriceCents = priceCents
                )
            )
        },
        onDismiss = onDismiss
    ) {
        OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = subscriptionPriceInput, onValueChange = { subscriptionPriceInput = it }, label = { Text("Subscription Price") }, modifier = Modifier.fillMaxWidth())
    }
}
