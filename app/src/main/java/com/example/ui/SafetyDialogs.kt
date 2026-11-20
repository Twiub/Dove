package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.DoveSeedCatalog
import com.example.data.ReportCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockAndReportSheet(
    targetName: String,
    onDismiss: () -> Unit,
    onBlockAndReport: (ReportCategory, String) -> Unit,
    onBlockOnly: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ReportCategory.SCAM) }
    var details by remember { mutableStateOf("") }
    var showBlockOnlyFollowup by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!showBlockOnlyFollowup) {
                Text("Safety options for $targetName", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Blocking removes $targetName from your experience immediately while preserving messages as read-only evidence for 90 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Report category", style = MaterialTheme.typography.titleSmall)
                ReportCategory.entries.forEach { cat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                        Text(cat.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(400) },
                    label = { Text("What happened? (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Button(
                    onClick = { onBlockAndReport(selectedCategory, details) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_block_report_button")
                ) {
                    Icon(Icons.Filled.Report, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Block & Report")
                }

                OutlinedButton(
                    onClick = { showBlockOnlyFollowup = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("block_only_button")
                ) {
                    Text("Block Only")
                }
            } else {
                // §D7.5.1 Follow-up prompt after Block only
                Text("Blocked $targetName", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Was this a scam or abuse? Filing a report helps keep everyone on Dove safer.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { onBlockAndReport(selectedCategory, details) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Yes, Report Now")
                }
                OutlinedButton(
                    onClick = onBlockOnly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No, Just Keep Blocked")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeDateSheet(
    partnerName: String,
    countryCode: String,
    onDismiss: () -> Unit,
    onSaveSafeDate: (place: String, meetAtLabel: String, trustedContact: String) -> Unit
) {
    var selectedPlace by remember { mutableStateOf(DoveSeedCatalog.curatedPublicDateVenues.first()) }
    var customPlace by remember { mutableStateOf("") }
    var meetAt by remember { mutableStateOf("Saturday, 3:00 PM") }
    var trustedContact by remember { mutableStateOf("") }
    val helplines = DoveSeedCatalog.emergencyHelplinesByCountry[countryCode]
        ?: DoveSeedCatalog.emergencyHelplinesByCountry.getValue("KE")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("Safe Date Plan with $partnerName", style = MaterialTheme.typography.headlineSmall)
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(
                    text = "First meets should always happen in a busy public place during daytime or early evening.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Text("Suggested public venues", style = MaterialTheme.typography.titleSmall)
            DoveSeedCatalog.curatedPublicDateVenues.forEach { venue ->
                FilterChip(
                    selected = selectedPlace == venue && customPlace.isBlank(),
                    onClick = {
                        selectedPlace = venue
                        customPlace = ""
                    },
                    label = { Text(venue) }
                )
            }

            OutlinedTextField(
                value = customPlace,
                onValueChange = { customPlace = it.take(100) },
                label = { Text("Or enter another public café / mall") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = meetAt,
                onValueChange = { meetAt = it.take(60) },
                label = { Text("Date & time") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = trustedContact,
                onValueChange = { trustedContact = it.take(60) },
                label = { Text("Trusted friend's name or phone (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.LocalPolice, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Text("Verified emergency numbers", style = MaterialTheme.typography.titleSmall)
                    }
                    helplines.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Button(
                onClick = {
                    val place = customPlace.ifBlank { selectedPlace }
                    onSaveSafeDate(place, meetAt, trustedContact)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_safe_date_button")
            ) {
                Text("Save Safe Date & Check-In Reminder")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VictimRecoverySheet(
    partnerName: String,
    countryCode: String,
    aiGuidance: String?,
    aiLoading: Boolean,
    onRequestGuidance: (String) -> Unit,
    onBlockAndReportNow: (ReportCategory) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSituation by remember { mutableStateOf(ReportCategory.SCAM) }
    val helplines = DoveSeedCatalog.emergencyHelplinesByCountry[countryCode]
        ?: DoveSeedCatalog.emergencyHelplinesByCountry.getValue("KE")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Urgent Safety & Scam Support", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "You are safe to stop talking to $partnerName immediately. Never send money or pay extortion demands — paying never stops threats.",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedSituation == ReportCategory.SCAM,
                    onClick = {
                        selectedSituation = ReportCategory.SCAM
                        onRequestGuidance("Romance / money scam")
                    },
                    label = { Text("Money / Scam") }
                )
                FilterChip(
                    selected = selectedSituation == ReportCategory.HARASSMENT,
                    onClick = {
                        selectedSituation = ReportCategory.HARASSMENT
                        onRequestGuidance("Threats or extortion")
                    },
                    label = { Text("Threats / Extortion") }
                )
            }

            if (aiLoading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Preparing calm step-by-step guidance...", style = MaterialTheme.typography.bodySmall)
                }
            } else if (aiGuidance != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(
                        text = aiGuidance,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Official reporting channels", style = MaterialTheme.typography.titleSmall)
                    helplines.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }

            Button(
                onClick = { onBlockAndReportNow(selectedSituation) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Freeze Chat, Preserve Evidence & Report")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ChildSafetyStandardsSheet(
    onDismiss: () -> Unit,
    onSubmitConcern: (String, String) -> Unit
) {
    var details by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
        title = { Text("Dove Child Safety Standards") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Dove strictly prohibits Child Sexual Abuse and Exploitation (CSAE) and bars all users under 18. Suspected minor safety concerns are blocked immediately and escalated to our named CSAM point of contact (safety@doveapp.example).",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(400) },
                    label = { Text("In-app: Report a safety concern") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitConcern("minor", details.ifBlank { "Child safety concern submitted" }) }
            ) {
                Text("Submit Concern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
