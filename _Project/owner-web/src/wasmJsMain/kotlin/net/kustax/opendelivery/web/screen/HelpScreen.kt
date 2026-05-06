package net.kustax.opendelivery.web.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen() {
    val faqs = listOf(
        "How do I register a new kiosk device?" to """
            1. Go to Properties → select a property → Devices.
            2. Tap the + button and enter a device name.
            3. The activation code dialog will appear immediately after creation.
            4. On the kiosk Android device, open the Guest Desk app.
            5. Tap "Enter Activation Code" and type the code shown here.
            6. The device will activate and appear as Online within seconds.
            Tip: Use the "Show Code" button on any unactivated device to see its code again.
        """.trimIndent(),

        "How do I create an owner account?" to """
            1. Go to Owners (Platform Admin only).
            2. Tap "New Owner" and fill in the form.
            3. After creation, a temporary password dialog will appear — copy it immediately.
            4. The temporary password is shown only once and is never stored.
            5. Share the email and temporary password with the owner so they can log in.
        """.trimIndent(),

        "How does the order flow work?" to """
            Orders follow a fixed state machine:
            PENDING → CONFIRMED → PREPARING → OUT_FOR_DELIVERY → DELIVERED
            Any non-final state can transition to CANCELLED.

            - Guests place orders from the kiosk Android app.
            - Owners manage order status from the Properties → Orders screen or the Orders sidebar item.
            - Each status update is logged in the Audit Log.
        """.trimIndent(),

        "Why does payment show as 'Emulated'?" to """
            This system uses emulated payment adapters for demonstration and thesis purposes.
            All payment transactions are simulated — no real money is charged.
            The Integrations page shows which adapters (Viva Wallet, Revolut QR, etc.) are configured.
            In production, these adapters would connect to live payment provider APIs.
        """.trimIndent(),

        "What is the Audit Log for?" to """
            The Audit Log records every action taken by platform admins and owners:
            - Entity created, updated, or deleted
            - Which actor performed the action (role + ID)
            - Which tenant (schema) was affected
            - Timestamp of the action

            Only Platform Admins can view the full audit log.
            Use the Entity Type and Action filters to find specific events.
        """.trimIndent(),

        "How do I view orders for a specific property?" to """
            Option 1: Properties → tap a property card → View Orders button.
            Option 2: Use the Orders sidebar item (shows all orders across all your properties).
            In the Orders view, use the Property filter dropdown to narrow down to one property.
        """.trimIndent()
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Help & FAQ",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Common questions about the Guest Desk management console.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        faqs.forEach { (question, answer) ->
            item {
                FaqItem(question = question, answer = answer)
            }
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    question,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Text(
                    answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
