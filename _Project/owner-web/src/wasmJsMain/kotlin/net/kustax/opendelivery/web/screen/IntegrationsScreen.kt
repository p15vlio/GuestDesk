package net.kustax.opendelivery.web.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.browser.localStorage

private data class Integration(
    val name: String,
    val adapterClass: String,
    val localStorageKey: String,
    val keyLabel: String
)

private val INTEGRATIONS = listOf(
    Integration("Payment Gateway — Viva Wallet", "EmulatedPaymentAdapter", "integration_viva_key", "Viva Wallet API Key"),
    Integration("Payment Gateway — Revolut QR", "EmulatedPaymentAdapter", "integration_revolut_key", "Revolut API Key"),
    Integration("Logistics — Wolt Drive", "EmulatedLogisticsAdapter", "integration_wolt_drive_key", "Wolt Drive API Key"),
    Integration("Food Delivery — efood", "EmulatedFoodDeliveryAdapter", "integration_efood_key", "efood API Key"),
    Integration("Food Delivery — Wolt", "EmulatedFoodDeliveryAdapter", "integration_wolt_key", "Wolt API Key"),
    Integration("Notification / Email (SMTP)", "EmulatedEmailAdapter", "integration_smtp_key", "SMTP Password / API Key")
)

@Composable
fun IntegrationsScreen() {
    var configuringIntegration by remember { mutableStateOf<Integration?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Integrations", style = MaterialTheme.typography.headlineSmall)
        Text(
            "All adapters are currently operating in emulated mode. No external services are connected.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))

        INTEGRATIONS.forEach { integration ->
            IntegrationCard(
                integration = integration,
                onConfigure = { configuringIntegration = integration }
            )
        }
    }

    configuringIntegration?.let { integration ->
        ConfigureDialog(
            integration = integration,
            onDismiss = { configuringIntegration = null }
        )
    }
}

@Composable
private fun IntegrationCard(integration: Integration, onConfigure: () -> Unit) {
    val amberColor = Color(0xFFF57F17)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(integration.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    integration.adapterClass,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = amberColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp),
                    contentColor = amberColor
                ) {
                    Text(
                        "Emulated",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                OutlinedButton(onClick = onConfigure) {
                    Text("Configure")
                }
            }
        }
    }
}

@Composable
private fun ConfigureDialog(integration: Integration, onDismiss: () -> Unit) {
    // Load the stored key from localStorage on first composition
    var keyInput by remember {
        mutableStateOf(localStorage.getItem(integration.localStorageKey) ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure ${integration.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "This value is stored locally in your browser. It is not sent to the backend in emulated mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text(integration.keyLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                localStorage.setItem(integration.localStorageKey, keyInput)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
