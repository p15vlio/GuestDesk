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
import androidx.compose.ui.unit.sp
import kotlinx.browser.window
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
import net.kustax.opendelivery.web.util.formatEuros
import net.kustax.opendelivery.web.util.formatTimestamp

// Converts a yyyy-MM-dd string to epoch milliseconds (approximate — no timezone handling)
private fun parseDateToEpochMs(dateStr: String): Long? {
    val parts = dateStr.trim().split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    val y = year - 1970
    val leapYears = (0 until y).count { yr ->
        val abs = 1970 + yr
        abs % 4 == 0 && (abs % 100 != 0 || abs % 400 == 0)
    }
    val daysInMonths = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val dayOfYear = daysInMonths.take(month - 1).sum() + day
    val totalDays = y * 365L + leapYears + dayOfYear - 1
    return totalDays * 86_400_000L
}

// Converts epoch ms to yyyy-MM-dd string (approximate — mirrors formatTimestamp logic)
private fun epochMsToDateString(epochMs: Long): String {
    val s = epochMs / 1000L
    val y = 1970 + (s / 31557600).toInt()
    val remaining = s % 31557600
    val mon = (remaining / 2629800).toInt() + 1
    val d = ((remaining % 2629800) / 86400).toInt() + 1
    return "$y-${mon.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}

@Composable
fun OwnerListScreen(client: ApiClient, onNavigate: (Screen) -> Unit = {}) {
    var ownerStats by remember { mutableStateOf(emptyList<OwnerWithStatsResponse>()) }
    var search by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingOwner by remember { mutableStateOf<OwnerResponse?>(null) }
    var createdOwnerPassword by remember { mutableStateOf<String?>(null) }
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
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                StatBadge("${item.propertyCount}", "Properties")
                                StatBadge("${item.deviceCount}", "Devices")
                                StatBadge("${item.productCount}", "Products")
                            }
                            Text(
                                "€${owner.subscriptionPriceCents / 100}/yr · Since ${formatTimestamp(owner.createdAt).take(10)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        // Action icons — view, edit, delete
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            IconButton(onClick = {
                                onNavigate(Screen.AdminOwnerView(owner.id, owner.schemaName, owner.displayName))
                            }) {
                                Icon(Icons.Default.OpenInNew, contentDescription = "View", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { editingOwner = owner }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                            }
                            // Hard delete — backend only supports permanent deletion for owners
                            IconButton(onClick = { ownerToDelete = owner }) {
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
        }
    }

    if (showCreateDialog) {
        CreateOwnerDialog(
            onConfirm = { request ->
                scope.launch {
                    try {
                        val created = client.createOwner(request)
                        showCreateDialog = false
                        if (created.temporaryPassword != null) {
                            createdOwnerPassword = created.temporaryPassword
                        } else {
                            SnackbarController.show("Owner created")
                            reload()
                        }
                    }
                    catch (e: Exception) { errorMessage = e.message }
                }
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    createdOwnerPassword?.let { tempPassword ->
        TempPasswordDialog(
            tempPassword = tempPassword,
            onDismiss = {
                createdOwnerPassword = null
                SnackbarController.show("Owner created")
                scope.launch { reload() }
            }
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
            message = "Permanently delete ${owner.displayName}? This cannot be undone. All tenant data will be lost.",
            confirmLabel = "Delete Permanently",
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
private fun StatBadge(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionDivider(label: String) {
    Spacer(Modifier.height(4.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateOwnerDialog(onConfirm: (CreateOwnerRequest) -> Unit, onDismiss: () -> Unit) {
    var ownerType by remember { mutableStateOf("COMPANY") }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var vatId by remember { mutableStateOf("") }
    var contactFirstName by remember { mutableStateOf("") }
    var contactLastName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var secondaryContactName by remember { mutableStateOf("") }
    var secondaryContactPhone by remember { mutableStateOf("") }
    var secondaryContactEmail by remember { mutableStateOf("") }
    var companyActivity by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var subscriptionPriceInput by remember { mutableStateOf("0.00") }

    val isCompany = ownerType == "COMPANY"
    val requiredFieldsFilled = vatId.isNotBlank() && contactEmail.isNotBlank() && contactPhone.isNotBlank() &&
        if (isCompany) companyName.isNotBlank() else firstName.isNotBlank() && lastName.isNotBlank()

    FormDialog(
        title = "Create Owner",
        onConfirm = {
            val priceCents = ((subscriptionPriceInput.toDoubleOrNull() ?: 0.0) * 100).toLong()
            onConfirm(
                CreateOwnerRequest(
                    ownerType = ownerType,
                    firstName = firstName.takeIf { !isCompany && it.isNotBlank() },
                    lastName = lastName.takeIf { !isCompany && it.isNotBlank() },
                    companyName = companyName.takeIf { isCompany && it.isNotBlank() },
                    vatId = vatId,
                    contactFirstName = contactFirstName.takeIf { isCompany && it.isNotBlank() },
                    contactLastName = contactLastName.takeIf { isCompany && it.isNotBlank() },
                    contactEmail = contactEmail,
                    contactPhone = contactPhone,
                    secondaryContactName = secondaryContactName.takeIf { it.isNotBlank() },
                    secondaryContactPhone = secondaryContactPhone.takeIf { it.isNotBlank() },
                    secondaryContactEmail = secondaryContactEmail.takeIf { it.isNotBlank() },
                    companyActivity = companyActivity.takeIf { it.isNotBlank() },
                    address = address.takeIf { it.isNotBlank() },
                    website = website.takeIf { it.isNotBlank() },
                    notes = notes.takeIf { it.isNotBlank() },
                    subscriptionPriceCents = priceCents
                )
            )
        },
        onDismiss = onDismiss,
        confirmEnabled = requiredFieldsFilled,
        confirmLabel = "Create"
    ) {
        // ── IDENTITY ──────────────────────────────────────────────
        SectionDivider("IDENTITY")

        // Owner Type dropdown
        ExposedDropdownMenuBox(
            expanded = typeMenuExpanded,
            onExpandedChange = { typeMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = if (isCompany) "Company" else "Individual",
                onValueChange = {},
                readOnly = true,
                label = { Text("Owner Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = typeMenuExpanded,
                onDismissRequest = { typeMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Company") },
                    onClick = { ownerType = "COMPANY"; typeMenuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Individual") },
                    onClick = { ownerType = "INDIVIDUAL"; typeMenuExpanded = false }
                )
            }
        }

        // Company name (company only) or First + Last name (individual only)
        if (isCompany) {
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        // VAT ID + (for company) Contact First Name
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = vatId,
                onValueChange = { vatId = it },
                label = { Text("VAT ID") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            if (isCompany) {
                OutlinedTextField(
                    value = contactFirstName,
                    onValueChange = { contactFirstName = it },
                    label = { Text("Contact First Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        // Contact Last Name + Contact Email (company) or Contact Email full-width (individual)
        if (isCompany) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = contactLastName,
                    onValueChange = { contactLastName = it },
                    label = { Text("Contact Last Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    label = { Text("Contact Email") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        } else {
            OutlinedTextField(
                value = contactEmail,
                onValueChange = { contactEmail = it },
                label = { Text("Contact Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            label = { Text("Contact Phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // ── SUBSCRIPTION ──────────────────────────────────────────
        SectionDivider("SUBSCRIPTION")

        OutlinedTextField(
            value = subscriptionPriceInput,
            onValueChange = { subscriptionPriceInput = it },
            label = { Text("Subscription Price (€/year)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // ── OPTIONAL DETAILS ──────────────────────────────────────
        SectionDivider("OPTIONAL DETAILS")

        OutlinedTextField(
            value = companyActivity,
            onValueChange = { companyActivity = it },
            label = { Text("Company Activity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = website,
            onValueChange = { website = it },
            label = { Text("Website") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = secondaryContactName,
            onValueChange = { secondaryContactName = it },
            label = { Text("Secondary Contact Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = secondaryContactPhone,
                onValueChange = { secondaryContactPhone = it },
                label = { Text("Secondary Phone") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = secondaryContactEmail,
                onValueChange = { secondaryContactEmail = it },
                label = { Text("Secondary Email") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
    }
}

@Composable
private fun TempPasswordDialog(tempPassword: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Owner Created") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The owner has been created. Share this temporary password with them — it will not be shown again.")
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            tempPassword,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        IconButton(
                            onClick = { window.navigator.clipboard.writeText(tempPassword) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy password",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun EditOwnerDialog(
    owner: OwnerResponse,
    onConfirm: (UpdateOwnerRequest) -> Unit,
    onDismiss: () -> Unit
) {
    var contactPhone by remember { mutableStateOf(owner.contactPhone) }
    var contactFirstName by remember { mutableStateOf(owner.contactFirstName ?: "") }
    var contactLastName by remember { mutableStateOf(owner.contactLastName ?: "") }
    var firstName by remember { mutableStateOf(owner.firstName ?: "") }
    var lastName by remember { mutableStateOf(owner.lastName ?: "") }
    var companyName by remember { mutableStateOf(owner.companyName ?: "") }
    var secondaryContactName by remember { mutableStateOf(owner.secondaryContactName ?: "") }
    var secondaryContactPhone by remember { mutableStateOf(owner.secondaryContactPhone ?: "") }
    var secondaryContactEmail by remember { mutableStateOf(owner.secondaryContactEmail ?: "") }
    var companyActivity by remember { mutableStateOf(owner.companyActivity ?: "") }
    var address by remember { mutableStateOf(owner.address ?: "") }
    var website by remember { mutableStateOf(owner.website ?: "") }
    var notes by remember { mutableStateOf(owner.notes ?: "") }
    var subscriptionPriceInput by remember { mutableStateOf(centsToInputString(owner.subscriptionPriceCents)) }
    // Pre-fill with yyyy-MM-dd string, not raw epoch ms
    var subscriptionActiveUntilInput by remember {
        mutableStateOf(owner.subscriptionActiveUntil?.let { epochMsToDateString(it) } ?: "")
    }

    val isCompany = owner.ownerType == "COMPANY"

    FormDialog(
        title = "Edit Owner",
        onConfirm = {
            val priceCents = ((subscriptionPriceInput.toDoubleOrNull() ?: 0.0) * 100).toLong()
            onConfirm(
                UpdateOwnerRequest(
                    firstName = firstName.takeIf { !isCompany && it.isNotBlank() },
                    lastName = lastName.takeIf { !isCompany && it.isNotBlank() },
                    companyName = companyName.takeIf { isCompany && it.isNotBlank() },
                    contactFirstName = contactFirstName.takeIf { isCompany && it.isNotBlank() },
                    contactLastName = contactLastName.takeIf { isCompany && it.isNotBlank() },
                    contactPhone = contactPhone,
                    secondaryContactName = secondaryContactName.takeIf { it.isNotBlank() },
                    secondaryContactPhone = secondaryContactPhone.takeIf { it.isNotBlank() },
                    secondaryContactEmail = secondaryContactEmail.takeIf { it.isNotBlank() },
                    companyActivity = companyActivity.takeIf { it.isNotBlank() },
                    address = address.takeIf { it.isNotBlank() },
                    website = website.takeIf { it.isNotBlank() },
                    notes = notes.takeIf { it.isNotBlank() },
                    subscriptionPriceCents = priceCents,
                    subscriptionActiveUntil = subscriptionActiveUntilInput
                        .takeIf { it.isNotBlank() }
                        ?.let { parseDateToEpochMs(it) }
                )
            )
        },
        onDismiss = onDismiss,
        confirmEnabled = contactPhone.isNotBlank(),
        confirmLabel = "Save"
    ) {
        // ── IDENTITY ──────────────────────────────────────────────
        SectionDivider("IDENTITY")

        // Read-only identity fields shown side by side
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = owner.ownerType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Owner Type") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = owner.vatId,
                onValueChange = {},
                readOnly = true,
                label = { Text("VAT ID") },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = owner.contactEmail,
            onValueChange = {},
            readOnly = true,
            label = { Text("Contact Email") },
            modifier = Modifier.fillMaxWidth()
        )

        // Editable name fields
        if (isCompany) {
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = contactFirstName,
                    onValueChange = { contactFirstName = it },
                    label = { Text("Contact First Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = contactLastName,
                    onValueChange = { contactLastName = it },
                    label = { Text("Contact Last Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            label = { Text("Contact Phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // ── SUBSCRIPTION ──────────────────────────────────────────
        SectionDivider("SUBSCRIPTION")

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = subscriptionPriceInput,
                onValueChange = { subscriptionPriceInput = it },
                label = { Text("Price (€/year)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = subscriptionActiveUntilInput,
                onValueChange = { subscriptionActiveUntilInput = it },
                label = { Text("Active Until (yyyy-MM-dd)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        // ── OPTIONAL DETAILS ──────────────────────────────────────
        SectionDivider("OPTIONAL DETAILS")

        OutlinedTextField(
            value = companyActivity,
            onValueChange = { companyActivity = it },
            label = { Text("Company Activity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = website,
            onValueChange = { website = it },
            label = { Text("Website") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = secondaryContactName,
            onValueChange = { secondaryContactName = it },
            label = { Text("Secondary Contact Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = secondaryContactPhone,
                onValueChange = { secondaryContactPhone = it },
                label = { Text("Secondary Phone") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = secondaryContactEmail,
                onValueChange = { secondaryContactEmail = it },
                label = { Text("Secondary Email") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
    }
}
