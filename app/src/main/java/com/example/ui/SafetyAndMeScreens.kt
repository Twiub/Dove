package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.BlockRecord
import com.example.data.CapabilityTier
import com.example.data.DoveSeedCatalog
import com.example.data.PrivatePrefs
import com.example.data.ProtectedPin
import com.example.data.PublicProfile
import com.example.data.ReportCategory
import com.example.data.SafeDateRecord
import com.example.data.SafetyReportRecord
import com.example.data.UserAccount
import com.example.data.WhoCanMessageFirst

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeScreen(
    account: UserAccount?,
    ownProfile: PublicProfile?,
    privatePrefs: PrivatePrefs?,
    pins: List<ProtectedPin>,
    blocks: List<BlockRecord>,
    reports: List<SafetyReportRecord>,
    safeDates: List<SafeDateRecord>,
    tier: CapabilityTier,
    onCompleteSelfieVerify: () -> Unit,
    onEnableSafetyConsent: () -> Unit,
    onAddPin: (String, String, Int) -> Unit,
    onPauseAllPins: (Long) -> Unit,
    onDeletePin: (String) -> Unit,
    onSavePrivatePrefs: (Boolean, String, Boolean, String, Boolean, String, String, String, String) -> Unit,
    onDeletePrivatePrefs: () -> Unit,
    onUpdateReachSettings: (
        whoCanMessageFirst: String?,
        showInNearby: Boolean?,
        paused: Boolean?,
        incognito: Boolean?,
        allowRoses: Boolean?,
        standoutsMuted: Boolean?,
        liteMode: Boolean?,
        verifiedOnly: Boolean?,
        distMaxKm: Int?
    ) -> Unit,
    onReportAfterBlock: (BlockRecord, String, String) -> Unit,
    onCheckInSafeDate: (String, String) -> Unit,
    onReportConcern: (String, String) -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit
) {
    var showAddPinDialog by remember { mutableStateOf(false) }
    var showEditPrivateDialog by remember { mutableStateOf(false) }
    var reportAfterBlockTarget by remember { mutableStateOf<BlockRecord?>(null) }
    var showChildSafetySheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val nowMs = System.currentTimeMillis()
    val anyPinPaused = pins.any { it.pausedUntilMs > nowMs }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile & Verification Header
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${ownProfile?.firstName ?: "Member"}, ${ownProfile?.age ?: account?.age ?: 18}",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                if (account?.selfieVerified == true) {
                                    Icon(
                                        Icons.Filled.VerifiedUser,
                                        contentDescription = "Selfie Verified",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Text(
                                text = "${ownProfile?.area ?: "Nairobi"} · ${tier.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.LocalFlorist, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("${account?.rosesBalance ?: 0} Roses", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    Text(
                        text = ownProfile?.bio.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (account?.browseOnly == true) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Browse-Only Mode", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "You skipped message scanning consent earlier. Accept safety scanning to reply and message.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(onClick = onEnableSafetyConsent) {
                                    Text("Enable Chat & Safety Scan")
                                }
                            }
                        }
                    }

                    if (account?.selfieVerified != true) {
                        Button(
                            onClick = onCompleteSelfieVerify,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("me_verify_selfie_button")
                        ) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Verify Selfie (Unlock 15 Picks & 5 Roses/wk)")
                        }
                    }
                }
            }
        }

        // Roses & Capability Tier Ladder (§D5.10, §D8)
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Roses & Trust Tier", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Roses balance: ${account?.rosesBalance ?: 0} · Sent this week: ${account?.rosesSentWeek ?: 0} of 5 (non-cashable, verified only)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Current Limits (${tier.label}): ${tier.dailyPicks} Daily Picks · ${tier.requestsPerDay} Requests/day · ${tier.maxPhotos} Photos max",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Safe Dates Check-Ins (§D9.5)
        if (safeDates.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("Safe Date Plans", style = MaterialTheme.typography.titleMedium)
                        }
                        safeDates.forEach { d ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${d.partnerName} · ${d.place}", style = MaterialTheme.typography.titleSmall)
                                    Text("${d.meetAtLabel} · Status: ${d.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (d.status == "scheduled") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TextButton(onClick = { onCheckInSafeDate(d.id, "checked_ok") }) { Text("Safe") }
                                        TextButton(onClick = { onCheckInSafeDate(d.id, "panic") }) {
                                            Text("SOS", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Protected Place Fixed Map Pins (§D5.6)
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Protected Places (${pins.size}/5)", style = MaterialTheme.typography.titleMedium)
                        }
                        IconButton(
                            onClick = { showAddPinDialog = true },
                            enabled = pins.size < 5,
                            modifier = Modifier.testTag("add_pin_button")
                        ) {
                            Icon(Icons.Filled.AddLocationAlt, contentDescription = "Add Protected Place")
                        }
                    }
                    Text(
                        text = "Hide your profile and keep yourself away from specific neighborhoods (workplace, home, school).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (pins.isNotEmpty()) {
                        pins.forEach { pin ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pin.label, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${pin.areaName} · ${pin.radiusM / 1000} km radius" +
                                            if (pin.pausedUntilMs > nowMs) " (Paused)" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeletePin(pin.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove pin ${pin.label}")
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { onPauseAllPins(if (anyPinPaused) 0L else 24L) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (anyPinPaused) "Resume Protected Pins Now" else "Pause All Pins for 24h")
                        }
                    }
                }
            }
        }

        // Private Compatibility Controls (§D4)
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Private Compatibility", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = if (privatePrefs?.consented == true) {
                            "Active (owner-only). Never shown to matches, analytics, or AI."
                        } else {
                            "Not set. Optional answers privately adjust compatibility %."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showEditPrivateDialog = true },
                            modifier = Modifier.testTag("edit_private_prefs_button")
                        ) {
                            Text(if (privatePrefs?.consented == true) "Edit Answers" else "Set Private Answers")
                        }
                        if (privatePrefs != null) {
                            TextButton(onClick = onDeletePrivatePrefs) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Reach & Privacy Settings (§D6.5, §D5.1, §D19.3)
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reach & Privacy Controls", style = MaterialTheme.typography.titleMedium)
                    Text("Who can message you first", style = MaterialTheme.typography.titleSmall)
                    WhoCanMessageFirst.entries.forEach { mode ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (account?.whoCanMessageFirst ?: "verified_inbox") == mode.code,
                                onClick = {
                                    onUpdateReachSettings(mode.code, null, null, null, null, null, null, null, null)
                                }
                            )
                            Column {
                                Text(mode.label, style = MaterialTheme.typography.bodyMedium)
                                Text(mode.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    HorizontalDivider()
                    SettingSwitchRow(
                        title = "Verified profiles only",
                        checked = ownProfile?.verifiedOnly ?: false,
                        onCheckedChange = { onUpdateReachSettings(null, null, null, null, null, null, null, it, null) }
                    )
                    SettingSwitchRow(
                        title = "Show me in Nearby",
                        checked = ownProfile?.showInNearby ?: true,
                        onCheckedChange = { onUpdateReachSettings(null, it, null, null, null, null, null, null, null) }
                    )
                    SettingSwitchRow(
                        title = "Pause profile (keep matches)",
                        checked = ownProfile?.paused ?: false,
                        onCheckedChange = { onUpdateReachSettings(null, null, it, null, null, null, null, null, null) }
                    )
                    SettingSwitchRow(
                        title = "Allow Roses",
                        checked = account?.allowRoses ?: true,
                        onCheckedChange = { onUpdateReachSettings(null, null, null, null, it, null, null, null, null) }
                    )
                    SettingSwitchRow(
                        title = "Mute Standouts strip",
                        checked = account?.standoutsMuted ?: false,
                        onCheckedChange = { onUpdateReachSettings(null, null, null, null, null, it, null, null, null) }
                    )
                    SettingSwitchRow(
                        title = "Data saver / Lite mode",
                        checked = account?.liteMode ?: false,
                        onCheckedChange = { onUpdateReachSettings(null, null, null, null, null, null, it, null, null) }
                    )
                }
            }
        }

        // Blocked Users & Report-After-Block (§D7.5.1, §D7.5.3)
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Block, contentDescription = null)
                        Text("Blocked Users (${blocks.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Evidence snapshots from blocked threads are preserved read-only for 90 days in case you choose to report later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    blocks.forEach { blk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(blk.blockedName, style = MaterialTheme.typography.titleSmall)
                                if (blk.reasonCategory.isNotBlank()) {
                                    Text("Reason: ${blk.reasonCategory}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            TextButton(onClick = { reportAfterBlockTarget = blk }) {
                                Text("Report")
                            }
                        }
                    }
                }
            }
        }

        // My Reports Outcome Loop (§D7.5.5)
        if (reports.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Report, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("My Safety Reports", style = MaterialTheme.typography.titleMedium)
                        }
                        reports.forEach { rep ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${rep.targetName} (${rep.category})", style = MaterialTheme.typography.titleSmall)
                                    if (rep.details.isNotBlank()) {
                                        Text(rep.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = when (rep.outcome) {
                                            "restricted" -> "Action taken"
                                            "no_violation" -> "No violation"
                                            else -> "Reviewing"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Child Safety Standards, Account Deletion & Sign Out (§D2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showChildSafetySheet = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Child Safety Standards & Report Concern")
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Sign Out")
                }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_account_button")
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Delete Account & Data")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showAddPinDialog) {
        AddProtectedPinDialog(
            onSave = { label, area, radius ->
                onAddPin(label, area, radius)
                showAddPinDialog = false
            },
            onDismiss = { showAddPinDialog = false }
        )
    }

    if (showEditPrivateDialog) {
        EditPrivateCompatibilityDialog(
            current = privatePrefs,
            onSave = { consented, pace, pDb, excl, eDb, chem, disc, flirt, place ->
                onSavePrivatePrefs(consented, pace, pDb, excl, eDb, chem, disc, flirt, place)
                showEditPrivateDialog = false
            },
            onDismiss = { showEditPrivateDialog = false }
        )
    }

    reportAfterBlockTarget?.let { blk ->
        ReportAfterBlockDialog(
            block = blk,
            onSubmit = { cat, details ->
                onReportAfterBlock(blk, cat.code, details)
                reportAfterBlockTarget = null
            },
            onDismiss = { reportAfterBlockTarget = null }
        )
    }

    if (showChildSafetySheet) {
        ChildSafetyStandardsSheet(
            onDismiss = { showChildSafetySheet = false },
            onSubmitConcern = { cat, details ->
                onReportConcern(cat, details)
                showChildSafetySheet = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete your Dove account?") },
            text = {
                Text("This immediately deletes your public profile, private compatibility answers, protected pins, and swipes, and signs you out.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddProtectedPinDialog(
    onSave: (String, String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("Workplace") }
    var selectedArea by remember { mutableStateOf(DoveSeedCatalog.areas.first().name) }
    var radiusM by remember { mutableIntStateOf(2000) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Protected Place Pin") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(60) },
                    label = { Text("Label (e.g. Office, Campus)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Area", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DoveSeedCatalog.areas.take(6).forEach { area ->
                        FilterChip(
                            selected = selectedArea == area.name,
                            onClick = { selectedArea = area.name },
                            label = { Text(area.name) }
                        )
                    }
                }
                Text("Protection radius", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1000 to "1 km", 2000 to "2 km", 5000 to "5 km", 10000 to "10 km").forEach { (r, text) ->
                        FilterChip(
                            selected = radiusM == r,
                            onClick = { radiusM = r },
                            label = { Text(text) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(label.ifBlank { "Protected Place" }, selectedArea, radiusM) }) {
                Text("Save Pin")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditPrivateCompatibilityDialog(
    current: PrivatePrefs?,
    onSave: (Boolean, String, Boolean, String, Boolean, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var consented by remember { mutableStateOf(current?.consented ?: true) }
    var pace by remember { mutableStateOf(current?.pace ?: "gradual") }
    var paceDb by remember { mutableStateOf(current?.paceDealbreaker ?: false) }
    var excl by remember { mutableStateOf(current?.exclusivity ?: "exclusive") }
    var exclDb by remember { mutableStateOf(current?.exclusivityDealbreaker ?: false) }
    var chem by remember { mutableStateOf(current?.chemWeight ?: "medium") }
    var disc by remember { mutableStateOf(current?.discretion ?: "selective") }
    var flirt by remember { mutableStateOf(current?.flirtLevel ?: "sweet") }
    var place by remember { mutableStateOf(current?.place ?: "skip") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Private Compatibility") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consented, onCheckedChange = { consented = it })
                    Text("Private answers used strictly for matching %.", style = MaterialTheme.typography.bodySmall)
                }
                Text("Pace", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("slow", "gradual", "natural", "open", "skip").forEach { v ->
                        FilterChip(selected = pace == v, onClick = { pace = v }, label = { Text(v) })
                    }
                }
                SettingSwitchRow("Pace dealbreaker", paceDb) { paceDb = it }
                Text("Exclusivity", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("exclusive", "open_to_discuss", "non_exclusive", "skip").forEach { v ->
                        FilterChip(selected = excl == v, onClick = { excl = v }, label = { Text(v) })
                    }
                }
                SettingSwitchRow("Exclusivity dealbreaker", exclDb) { exclDb = it }
                Text("Chemistry priority", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("low", "medium", "high", "skip").forEach { v ->
                        FilterChip(selected = chem == v, onClick = { chem = v }, label = { Text(v) })
                    }
                }
                Text("Tone", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("sweet", "flirty", "bold", "skip").forEach { v ->
                        FilterChip(selected = flirt == v, onClick = { flirt = v }, label = { Text(v) })
                    }
                }
                Text("Privacy preference", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("public", "selective", "private", "skip").forEach { v ->
                        FilterChip(selected = disc == v, onClick = { disc = v }, label = { Text(v) })
                    }
                }
                Text("Hosting space", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("own_place", "shared_place", "no_place", "skip").forEach { v ->
                        FilterChip(selected = place == v, onClick = { place = v }, label = { Text(v) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(consented, pace, paceDb, excl, exclDb, chem, disc, flirt, place) },
                enabled = consented
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ReportAfterBlockDialog(
    block: BlockRecord,
    onSubmit: (ReportCategory, String) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf(ReportCategory.SCAM) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report ${block.blockedName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Attach the preserved evidence snapshot from your block to help moderators review.",
                    style = MaterialTheme.typography.bodySmall
                )
                ReportCategory.entries.forEach { cat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = category == cat, onClick = { category = cat })
                        Text(cat.label, style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(400) },
                    label = { Text("Additional context") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(category, details) }) {
                Text("Submit Report")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
