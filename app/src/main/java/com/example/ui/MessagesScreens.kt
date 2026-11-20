package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChatMessageRecord
import com.example.data.ConnectionRecord
import com.example.data.MessageRequestRecord
import com.example.data.ReportCategory
import com.example.data.SafeDateRecord
import com.example.data.SwipeRecord
import com.example.data.UserAccount

private enum class MessagesSubTab(val label: String) {
    CHATS("Chats"),
    REQUESTS("Requests"),
    WAITING("Waiting")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesHubScreen(
    currentUserId: String,
    account: UserAccount?,
    connections: List<ConnectionRecord>,
    messageRequests: List<MessageRequestRecord>,
    swipes: List<SwipeRecord>,
    safeDates: List<SafeDateRecord>,
    onOpenConnection: (ConnectionRecord) -> Unit,
    onReplyToRequest: (MessageRequestRecord, String) -> Unit,
    onIgnoreRequest: (String) -> Unit,
    onBulkIgnoreFiltered: (List<MessageRequestRecord>) -> Unit,
    onBulkReportSpam: (List<MessageRequestRecord>) -> Unit,
    onClimbWaitingLadder: (SwipeRecord, String, String?) -> Unit,
    onCheckInSafeDate: (String, String) -> Unit,
    onBlockAndReportUser: (String, String, ReportCategory?, String, Boolean, String?) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var replyTarget by remember { mutableStateOf<MessageRequestRecord?>(null) }
    var shieldTarget by remember { mutableStateOf<MessageRequestRecord?>(null) }
    var ladderNoteTarget by remember { mutableStateOf<SwipeRecord?>(null) }
    var filteredExpanded by remember { mutableStateOf(false) }
    val sensitiveTapState = remember { mutableStateMapOf<String, Int>() }

    val activeChats = remember(connections) {
        connections.filter { it.status == "active" }
    }
    val incomingPending = remember(messageRequests, currentUserId) {
        messageRequests.filter { it.receiverId == currentUserId && it.status == "pending" }
    }
    val incomingFiltered = remember(messageRequests, currentUserId) {
        messageRequests.filter { it.receiverId == currentUserId && it.status == "filtered" }
    }
    val sentRequests = remember(messageRequests, currentUserId) {
        messageRequests.filter {
            it.senderId == currentUserId && it.status in listOf("pending", "filtered", "held", "blocked")
        }
    }
    val matchedPartnerIds = remember(connections, currentUserId) {
        connections.flatMap { it.memberUids }.filter { it != currentUserId }.toSet()
    }
    val waitingLikes = remember(swipes, matchedPartnerIds) {
        swipes.filter {
            it.action in listOf("like", "like_note") && it.targetId !in matchedPartnerIds
        }
    }

    val canSendRose = account?.selfieVerified == true &&
        (account.rosesBalance > 0) &&
        (account.rosesSentWeek < 5)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Messages", style = MaterialTheme.typography.headlineMedium)
            if (account != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFlorist,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${account.rosesBalance} Rose${if (account.rosesBalance == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        SecondaryTabRow(selectedTabIndex = selectedTab) {
            MessagesSubTab.entries.forEachIndexed { idx, tab ->
                val badgeCount = when (tab) {
                    MessagesSubTab.CHATS -> activeChats.size
                    MessagesSubTab.REQUESTS -> incomingPending.size
                    MessagesSubTab.WAITING -> waitingLikes.size
                }
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    modifier = Modifier.testTag("messages_tab_${tab.name.lowercase()}"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(tab.label)
                            if (badgeCount > 0) {
                                Badge { Text("$badgeCount") }
                            }
                        }
                    }
                )
            }
        }

        when (MessagesSubTab.entries[selectedTab]) {
            MessagesSubTab.CHATS -> {
                ChatsListTab(
                    currentUserId = currentUserId,
                    chats = activeChats,
                    scheduledDates = safeDates.filter { it.status == "scheduled" },
                    onOpenConnection = onOpenConnection,
                    onCheckInSafeDate = onCheckInSafeDate
                )
            }

            MessagesSubTab.REQUESTS -> {
                RequestsListTab(
                    standoutsMuted = account?.standoutsMuted == true,
                    pending = incomingPending,
                    filtered = incomingFiltered,
                    sent = sentRequests,
                    filteredExpanded = filteredExpanded,
                    sensitiveTapState = sensitiveTapState,
                    onToggleFiltered = { filteredExpanded = !filteredExpanded },
                    onTapSensitive = { id ->
                        val next = ((sensitiveTapState[id] ?: 0) + 1).coerceAtMost(2)
                        sensitiveTapState[id] = next
                    },
                    onOpenReply = { replyTarget = it },
                    onIgnore = onIgnoreRequest,
                    onShield = { shieldTarget = it },
                    onBulkIgnore = { onBulkIgnoreFiltered(incomingFiltered) },
                    onBulkReportSpam = { onBulkReportSpam(incomingFiltered) }
                )
            }

            MessagesSubTab.WAITING -> {
                WaitingListTab(
                    waiting = waitingLikes,
                    canSendRose = canSendRose,
                    onAddNote = { ladderNoteTarget = it },
                    onSendRose = { swipe -> onClimbWaitingLadder(swipe, "rose", null) },
                    onReintroduce = { swipe -> onClimbWaitingLadder(swipe, "reintroduce", null) }
                )
            }
        }
    }

    replyTarget?.let { req ->
        ReplyAndMatchDialog(
            request = req,
            onSendReply = { reply ->
                onReplyToRequest(req, reply)
                replyTarget = null
            },
            onDismiss = { replyTarget = null }
        )
    }

    shieldTarget?.let { req ->
        BlockAndReportSheet(
            targetName = req.senderName,
            onDismiss = { shieldTarget = null },
            onBlockAndReport = { cat, details ->
                onBlockAndReportUser(req.senderId, req.senderName, cat, details, true, req.body)
                onIgnoreRequest(req.id)
                shieldTarget = null
            },
            onBlockOnly = {
                onBlockAndReportUser(req.senderId, req.senderName, null, "", false, req.body)
                onIgnoreRequest(req.id)
                shieldTarget = null
            }
        )
    }

    ladderNoteTarget?.let { swipe ->
        WaitingAddNoteDialog(
            targetName = swipe.targetName,
            onSend = { note ->
                onClimbWaitingLadder(swipe, "note", note)
                ladderNoteTarget = null
            },
            onDismiss = { ladderNoteTarget = null }
        )
    }
}

@Composable
private fun ChatsListTab(
    currentUserId: String,
    chats: List<ConnectionRecord>,
    scheduledDates: List<SafeDateRecord>,
    onOpenConnection: (ConnectionRecord) -> Unit,
    onCheckInSafeDate: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (scheduledDates.isNotEmpty()) {
            items(scheduledDates, key = { "date_${it.id}" }) { safeDate ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = "Safe Date Plan with ${safeDate.partnerName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = "${safeDate.place} · ${safeDate.meetAtLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onCheckInSafeDate(safeDate.id, "checked_ok") },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("I'm Safe")
                            }
                            OutlinedButton(
                                onClick = { onCheckInSafeDate(safeDate.id, "panic") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Need Help")
                            }
                        }
                    }
                }
            }
        }

        if (chats.isEmpty()) {
            item {
                EmptyPoolCard(
                    title = "No open chats yet",
                    subtitle = "Chats open only when both people consent — via a mutual Like or replying to a one-message request."
                )
            }
        } else {
            items(chats, key = { it.id }) { conn ->
                val partnerName = if (conn.userAId == currentUserId) conn.userBName else conn.userAName
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenConnection(conn) }
                        .testTag("chat_row_${conn.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = partnerName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(partnerName, style = MaterialTheme.typography.titleMedium)
                                if (conn.userBVerified) {
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = "Verified",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (conn.userBArea.isNotBlank()) {
                                    Text(
                                        text = "· ${conn.userBArea}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = conn.lastMessagePreview.ifBlank { "Say hello in Dove chat" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestsListTab(
    standoutsMuted: Boolean,
    pending: List<MessageRequestRecord>,
    filtered: List<MessageRequestRecord>,
    sent: List<MessageRequestRecord>,
    filteredExpanded: Boolean,
    sensitiveTapState: Map<String, Int>,
    onToggleFiltered: () -> Unit,
    onTapSensitive: (String) -> Unit,
    onOpenReply: (MessageRequestRecord) -> Unit,
    onIgnore: (String) -> Unit,
    onShield: (MessageRequestRecord) -> Unit,
    onBulkIgnore: () -> Unit,
    onBulkReportSpam: () -> Unit
) {
    // §D5.9 Standouts: strictly whitelisted pair-specific effort reasons
    val standouts = remember(pending) {
        pending.filter { it.roseAttached || it.effortReasons.isNotEmpty() }.take(5)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!standoutsMuted && standouts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Standouts for you", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(standouts, key = { "standout_${it.id}" }) { req ->
                            val reasonLabel = whitelistedEffortLabel(req.effortReasons, req.roseAttached)
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                modifier = Modifier
                                    .widthIn(min = 210.dp, max = 250.dp)
                                    .clickable { onOpenReply(req) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = req.senderName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        if (req.senderVerified) {
                                            Icon(
                                                Icons.Filled.VerifiedUser,
                                                contentDescription = "Verified",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = reasonLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = req.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Verified Requests (${pending.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (pending.isEmpty()) {
            item {
                EmptyPoolCard(
                    title = "No pending requests",
                    subtitle = "Selfie-verified people who send you a 1-message opener appear here. You can reply to open chat or ignore silently."
                )
            }
        } else {
            items(pending, key = { it.id }) { req ->
                RequestCard(
                    request = req,
                    tapCount = sensitiveTapState[req.id] ?: 0,
                    onTapSensitive = { onTapSensitive(req.id) },
                    onReply = { onOpenReply(req) },
                    onIgnore = { onIgnore(req.id) },
                    onShield = { onShield(req) }
                )
            }
        }

        // Collapsible Filtered Requests (§D6.5)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleFiltered)
                            .testTag("filtered_requests_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.FilterAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Filtered Requests (${filtered.size})", style = MaterialTheme.typography.titleSmall)
                        }
                        Icon(
                            imageVector = if (filteredExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (filteredExpanded) "Collapse filtered requests" else "Expand filtered requests"
                        )
                    }

                    if (filteredExpanded) {
                        Text(
                            text = "From unverified accounts or lower-trust senders. Clear anytime without notifying senders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (filtered.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onBulkIgnore,
                                    modifier = Modifier.testTag("bulk_ignore_filtered_button")
                                ) {
                                    Text("Ignore all")
                                }
                                OutlinedButton(
                                    onClick = onBulkReportSpam,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("bulk_report_spam_button")
                                ) {
                                    Text("Report spam")
                                }
                            }
                            filtered.forEach { req ->
                                RequestCard(
                                    request = req,
                                    tapCount = sensitiveTapState[req.id] ?: 0,
                                    onTapSensitive = { onTapSensitive(req.id) },
                                    onReply = { onOpenReply(req) },
                                    onIgnore = { onIgnore(req.id) },
                                    onShield = { onShield(req) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (sent.isNotEmpty()) {
            item {
                Text("Sent Requests", style = MaterialTheme.typography.titleMedium)
            }
            items(sent, key = { "sent_${it.id}" }) { req ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("To ${req.receiverName}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = when (req.status) {
                                    "blocked" -> "Blocked"
                                    "held" -> "Held for review"
                                    else -> "Pending reply"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (req.status == "blocked" || req.status == "held") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(req.body, style = MaterialTheme.typography.bodySmall)
                        if (req.riskCategory.isNotBlank()) {
                            Text(
                                text = "Safety note: ${req.riskCategory}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun whitelistedEffortLabel(reasons: List<String>, roseAttached: Boolean): String {
    return when {
        roseAttached || "rose_sent" in reasons -> "Sent a Rose with their note"
        "note_on_prompt" in reasons && "verified" in reasons -> "Verified · Replied to your prompt"
        "note_on_prompt" in reasons -> "Replied to your prompt"
        "verified" in reasons -> "Selfie verified"
        else -> "Thoughtful opener"
    }
}

@Composable
private fun RequestCard(
    request: MessageRequestRecord,
    tapCount: Int,
    onTapSensitive: () -> Unit,
    onReply: () -> Unit,
    onIgnore: () -> Unit,
    onShield: () -> Unit
) {
    val isBlurred = request.sensitive && tapCount < 2
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("request_card_${request.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(request.senderName, style = MaterialTheme.typography.titleMedium)
                    if (request.senderVerified) {
                        Icon(
                            Icons.Filled.VerifiedUser,
                            contentDescription = "Selfie Verified",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (request.roseAttached) {
                        Icon(
                            Icons.Filled.LocalFlorist,
                            contentDescription = "Rose attached",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (request.senderArea.isNotBlank()) {
                        Text(
                            text = "· ${request.senderArea}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onShield) {
                    Icon(Icons.Filled.Shield, contentDescription = "Block or Report ${request.senderName}")
                }
            }

            if (isBlurred) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onTapSensitive)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = null)
                        Text(
                            text = if (tapCount == 0) "Sensitive message hidden · Tap twice to reveal"
                            else "Confirm reveal? Tap once more to view",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Text(request.body, style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onIgnore,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("ignore_request_${request.id}")
                ) {
                    Text("Ignore")
                }
                Button(
                    onClick = onReply,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("reply_request_${request.id}")
                ) {
                    Text("Reply & Match")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WaitingListTab(
    waiting: List<SwipeRecord>,
    canSendRose: Boolean,
    onAddNote: (SwipeRecord) -> Unit,
    onSendRose: (SwipeRecord) -> Unit,
    onReintroduce: (SwipeRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "People you liked who haven't replied yet. Status stays calmly as Waiting — no read receipts or rejection flags.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (waiting.isEmpty()) {
            item {
                EmptyPoolCard(
                    title = "No likes waiting right now",
                    subtitle = "When you Like someone in Picks or Discover, they appear here until they match back."
                )
            }
        } else {
            items(waiting, key = { it.targetId }) { swipe ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(swipe.targetName, style = MaterialTheme.typography.titleMedium)
                                if (swipe.targetArea.isNotBlank()) {
                                    Text(swipe.targetArea, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "Waiting",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        if (swipe.note.isNotBlank()) {
                            Text(
                                text = "Your note: \"${swipe.note}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text("Interest Ladder (each step once per person)", style = MaterialTheme.typography.labelSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onAddNote(swipe) },
                                enabled = "note" !in swipe.ladderUsed
                            ) {
                                Text(if ("note" in swipe.ladderUsed) "Note added" else "Add prompt note")
                            }
                            OutlinedButton(
                                onClick = { onSendRose(swipe) },
                                enabled = canSendRose && ("rose" !in swipe.ladderUsed)
                            ) {
                                Icon(Icons.Filled.LocalFlorist, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(if ("rose" in swipe.ladderUsed) "Rose sent" else "Send Rose")
                            }
                            OutlinedButton(
                                onClick = { onReintroduce(swipe) },
                                enabled = "reintroduce" !in swipe.ladderUsed
                            ) {
                                Text(if ("reintroduce" in swipe.ladderUsed) "Reintroduced" else "Reintroduce once")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyAndMatchDialog(
    request: MessageRequestRecord,
    onSendReply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var reply by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reply to ${request.senderName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "\"${request.body}\"",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it.take(300) },
                    label = { Text("Write your reply to open chat") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reply_request_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendReply(reply) },
                enabled = reply.isNotBlank(),
                modifier = Modifier.testTag("reply_request_confirm")
            ) {
                Text("Match & Open Chat")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun WaitingAddNoteDialog(
    targetName: String,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a prompt note for $targetName") },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(200) },
                label = { Text("Thoughtful note (max 200 chars)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onSend(note) }, enabled = note.isNotBlank()) {
                Text("Save Note")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    currentUserId: String,
    account: UserAccount?,
    connection: ConnectionRecord,
    messages: List<ChatMessageRecord>,
    aiGuidance: String?,
    aiGuidanceLoading: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String, Boolean) -> Unit,
    onDismissNudge: () -> Unit,
    onAppealStrike: () -> Unit,
    onBlockAndReport: (String, String, ReportCategory?, String, Boolean, String?) -> Unit,
    onSaveSafeDate: (String, String, String) -> Unit,
    onRequestVictimGuidance: (String) -> Unit
) {
    BackHandler(enabled = true) { onBack() }

    val partnerId = if (connection.userAId == currentUserId) connection.userBId else connection.userAId
    val partnerName = if (connection.userAId == currentUserId) connection.userBName else connection.userAName

    var input by remember { mutableStateOf("") }
    var attachRose by remember { mutableStateOf(false) }
    var showBlockSheet by remember { mutableStateOf(false) }
    var showSafeDateSheet by remember { mutableStateOf(false) }
    var showVictimSheet by remember { mutableStateOf(false) }
    val sensitiveRevealTaps = remember { mutableStateMapOf<String, Int>() }

    val nowMs = System.currentTimeMillis()
    val isSuspended = (account?.suspensionUntilMs ?: 0L) > nowMs
    val isBrowseOnly = account?.browseOnly == true
    val canSendRose = account?.selfieVerified == true &&
        (account.rosesBalance > 0) &&
        (account.rosesSentWeek < 5)

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(partnerName, style = MaterialTheme.typography.titleLarge)
                        if (connection.userBVerified) {
                            Icon(
                                Icons.Filled.VerifiedUser,
                                contentDescription = "Verified",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Messages")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSafeDateSheet = true },
                        modifier = Modifier.testTag("open_safe_date_button")
                    ) {
                        Icon(Icons.Filled.EventAvailable, contentDescription = "Safe Date Plan")
                    }
                    IconButton(
                        onClick = {
                            showVictimSheet = true
                            onRequestVictimGuidance("Romance / money scam")
                        },
                        modifier = Modifier.testTag("open_victim_support_button")
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = "Urgent safety support", tint = MaterialTheme.colorScheme.tertiary)
                    }
                    IconButton(
                        onClick = { showBlockSheet = true },
                        modifier = Modifier.testTag("chat_shield_button")
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = "Block or Report $partnerName")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Persistent scan notice (§D6.1)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.safety_scan_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Generic nudge banner (§D6.3)
            if (connection.nudgeBanner.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = connection.nudgeBanner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onDismissNudge) { Text("Dismiss") }
                            TextButton(onClick = { showBlockSheet = true }) { Text("Report / Block") }
                        }
                    }
                }
            }

            // Strike suspension banner + Appeal (§D6.2.4)
            if (isSuspended) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Chat temporarily paused (${account?.strikes ?: 0} strikes): ${account?.suspensionReason.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        OutlinedButton(
                            onClick = onAppealStrike,
                            modifier = Modifier.testTag("appeal_strike_button")
                        ) {
                            Text("Appeal Strike")
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.senderId == currentUserId
                    // Blocked messages visible to sender only (§D6.2)
                    if (msg.status == "blocked" && !isMine) return@items

                    ChatMessageBubble(
                        message = msg,
                        isMine = isMine,
                        revealTaps = sensitiveRevealTaps[msg.id] ?: 0,
                        onTapSensitive = {
                            val next = ((sensitiveRevealTaps[msg.id] ?: 0) + 1).coerceAtMost(2)
                            sensitiveRevealTaps[msg.id] = next
                        }
                    )
                }
            }

            // Composer
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (isBrowseOnly) {
                        Text(
                            text = "Browse-Only mode active. Enable Safety Consent in Me to chat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (canSendRose) {
                            FilterChip(
                                selected = attachRose,
                                onClick = { attachRose = !attachRose },
                                label = {
                                    Icon(
                                        Icons.Filled.LocalFlorist,
                                        contentDescription = "Attach Rose",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it.take(1000) },
                            placeholder = { Text("Message $partnerName (stays in Dove)") },
                            enabled = !isSuspended && !isBrowseOnly,
                            maxLines = 3,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )
                        IconButton(
                            onClick = {
                                val textToSend = input
                                input = ""
                                val rose = attachRose
                                attachRose = false
                                onSendMessage(textToSend, rose)
                            },
                            enabled = input.isNotBlank() && !isSuspended && !isBrowseOnly,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (input.isNotBlank() && !isSuspended && !isBrowseOnly) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    CircleShape
                                )
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = if (input.isNotBlank() && !isSuspended && !isBrowseOnly) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBlockSheet) {
        val lastPartnerSnippet = messages.lastOrNull { it.senderId != currentUserId }?.body
        BlockAndReportSheet(
            targetName = partnerName,
            onDismiss = { showBlockSheet = false },
            onBlockAndReport = { cat, details ->
                onBlockAndReport(partnerId, partnerName, cat, details, true, lastPartnerSnippet)
                showBlockSheet = false
            },
            onBlockOnly = {
                onBlockAndReport(partnerId, partnerName, null, "", false, lastPartnerSnippet)
                showBlockSheet = false
            }
        )
    }

    if (showSafeDateSheet) {
        SafeDateSheet(
            partnerName = partnerName,
            countryCode = account?.country ?: "KE",
            onDismiss = { showSafeDateSheet = false },
            onSaveSafeDate = { place, meetAt, contact ->
                onSaveSafeDate(place, meetAt, contact)
                showSafeDateSheet = false
            }
        )
    }

    if (showVictimSheet) {
        val lastPartnerSnippet = messages.lastOrNull { it.senderId != currentUserId }?.body
        VictimRecoverySheet(
            partnerName = partnerName,
            countryCode = account?.country ?: "KE",
            aiGuidance = aiGuidance,
            aiLoading = aiGuidanceLoading,
            onRequestGuidance = onRequestVictimGuidance,
            onBlockAndReportNow = { cat ->
                onBlockAndReport(partnerId, partnerName, cat, "Urgent safety freeze", true, lastPartnerSnippet)
                showVictimSheet = false
            },
            onDismiss = { showVictimSheet = false }
        )
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessageRecord,
    isMine: Boolean,
    revealTaps: Int,
    onTapSensitive: () -> Unit
) {
    val alignment = if (isMine) Alignment.End else Alignment.Start
    val isSensitiveBlurred = message.status == "sensitive" && !isMine && revealTaps < 2
    val containerColor = when {
        message.status == "blocked" -> MaterialTheme.colorScheme.errorContainer
        message.status == "held" -> MaterialTheme.colorScheme.tertiaryContainer
        isMine -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val textColor = when {
        message.status == "blocked" -> MaterialTheme.colorScheme.onErrorContainer
        message.status == "held" -> MaterialTheme.colorScheme.onTertiaryContainer
        isMine -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (message.roseAttached) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            Icons.Filled.LocalFlorist,
                            contentDescription = "Rose attached",
                            tint = textColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Rose attached", style = MaterialTheme.typography.labelSmall, color = textColor)
                    }
                }

                if (isSensitiveBlurred) {
                    Column(
                        modifier = Modifier.clickable(onClick = onTapSensitive),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = message.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.blur(10.dp)
                        )
                        Text(
                            text = if (revealTaps == 0) "Sensitive message · Tap twice to view"
                            else "Confirm? Tap once more to reveal",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }

                if (message.status == "blocked" || message.status == "held") {
                    Text(
                        text = if (message.status == "blocked") {
                            "Not delivered: ${message.reasonCode.ifBlank { "Safety policy violation" }}"
                        } else {
                            "Held for safety review (${message.reasonCode})"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor
                    )
                }
            }
        }
    }
}
