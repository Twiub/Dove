package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CapabilityTier
import com.example.data.LookingFor
import com.example.data.PublicProfile
import com.example.data.ReportCategory
import com.example.data.ScoredCandidate
import com.example.data.UserAccount
import com.example.ui.theme.RingAmber
import com.example.ui.theme.RingGrey
import com.example.ui.theme.RingMint
import com.example.viewmodel.DiscoverFilters

@Composable
fun PicksScreen(
    account: UserAccount?,
    candidates: List<ScoredCandidate>,
    tier: CapabilityTier,
    onSwipe: (ScoredCandidate, String, String?, Boolean) -> Unit,
    onBlockAndReport: (PublicProfile, ReportCategory?, String, Boolean) -> Unit
) {
    val capped = remember(candidates, tier) { candidates.take(tier.dailyPicks) }
    var selectedForWhy by remember { mutableStateOf<ScoredCandidate?>(null) }
    var selectedForNote by remember { mutableStateOf<ScoredCandidate?>(null) }
    var selectedForProfile by remember { mutableStateOf<ScoredCandidate?>(null) }
    var selectedForShield by remember { mutableStateOf<PublicProfile?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Picks", style = MaterialTheme.typography.headlineMedium)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${capped.size}/${tier.dailyPicks} (${tier.label})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Text(
                    text = "Capped daily picks ranked by mutual compatibility, never attractiveness or spam.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (capped.isEmpty()) {
            item {
                EmptyPoolCard(
                    title = "You're caught up on today's Picks",
                    subtitle = "Check Nearby or adjust your age/distance preferences in Me to expand your pool."
                )
            }
        } else {
            items(capped, key = { it.candidate.profile.userId }) { scored ->
                PickCandidateCard(
                    scored = scored,
                    onViewProfile = { selectedForProfile = scored },
                    onWhyScore = { selectedForWhy = scored },
                    onPass = { onSwipe(scored, "pass", null, false) },
                    onLike = { onSwipe(scored, "like", null, false) },
                    onLikeWithNote = { selectedForNote = scored },
                    onShield = { selectedForShield = scored.candidate.profile }
                )
            }
        }
    }

    selectedForWhy?.let { s ->
        WhyScoreDialog(scored = s, onDismiss = { selectedForWhy = null })
    }

    selectedForNote?.let { s ->
        LikeWithNoteDialog(
            scored = s,
            canSendRose = (account?.selfieVerified == true && (account.rosesBalance > 0) && (account.rosesSentWeek < 5)),
            rosesUsedWeek = account?.rosesSentWeek ?: 0,
            onSend = { note, rose ->
                onSwipe(s, "like_note", note, rose)
                selectedForNote = null
            },
            onDismiss = { selectedForNote = null }
        )
    }

    selectedForProfile?.let { s ->
        ProfileDetailSheet(
            scored = s,
            onDismiss = { selectedForProfile = null },
            onLike = {
                onSwipe(s, "like", null, false)
                selectedForProfile = null
            },
            onShield = {
                selectedForShield = s.candidate.profile
                selectedForProfile = null
            }
        )
    }

    selectedForShield?.let { target ->
        BlockAndReportSheet(
            targetName = target.firstName,
            onDismiss = { selectedForShield = null },
            onBlockAndReport = { cat, details ->
                onBlockAndReport(target, cat, details, true)
                selectedForShield = null
            },
            onBlockOnly = {
                onBlockAndReport(target, null, "", false)
                selectedForShield = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickCandidateCard(
    scored: ScoredCandidate,
    onViewProfile: () -> Unit,
    onWhyScore: () -> Unit,
    onPass: () -> Unit,
    onLike: () -> Unit,
    onLikeWithNote: () -> Unit,
    onShield: () -> Unit
) {
    val p = scored.candidate.profile
    val ringColor = when {
        scored.matchPercent >= 85 -> RingMint
        scored.matchPercent >= 68 -> RingAmber
        else -> RingGrey
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewProfile)
            .testTag("pick_card_${p.userId}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
            ) {
                if (scored.candidate.photoRes != null) {
                    Image(
                        painter = painterResource(id = scored.candidate.photoRes),
                        contentDescription = "${p.firstName}'s profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(p.firstName.take(1), style = MaterialTheme.typography.displaySmall)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                                startY = 220f
                            )
                        )
                )

                // Top-right Shield + Match Ring
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = CircleShape,
                        modifier = Modifier.clickable(onClick = onWhyScore)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { scored.matchPercent / 100f },
                                    color = ringColor,
                                    trackColor = ringColor.copy(alpha = 0.2f),
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = "${scored.matchPercent}% fit",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(Icons.Filled.Info, contentDescription = "Why this match score", modifier = Modifier.size(16.dp))
                        }
                    }

                    IconButton(
                        onClick = onShield,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
                            .testTag("shield_button_${p.userId}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Report or Block ${p.firstName}",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Bottom overlay identity
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${p.firstName}, ${p.age}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        if (p.selfieVerified) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = "Selfie Verified",
                                tint = Color(0xFF7ADBC2),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "${p.area} · ${scored.distanceBand} · ${LookingFor.fromCode(p.lookingFor).label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(p.bio, style = MaterialTheme.typography.bodyMedium)

                if (p.promptAnswer.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(p.promptQuestion, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(p.promptAnswer, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Four Dove card meters (§D5.3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CardMeter("Interests", scored.interestsScore, Modifier.weight(1f))
                    CardMeter("Distance", scored.distanceScore, Modifier.weight(1f))
                    CardMeter("Active", scored.activeScore, Modifier.weight(1f))
                    CardMeter("Verified", scored.verifiedScore, Modifier.weight(1f))
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    p.interests.take(5).forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = CircleShape
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPass,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pass_button_${p.userId}")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Pass")
                    }
                    OutlinedButton(
                        onClick = onLikeWithNote,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("like_note_button_${p.userId}")
                    ) {
                        Text("Like + Note")
                    }
                    Button(
                        onClick = onLike,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("like_button_${p.userId}")
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Like")
                    }
                }
            }
        }
    }
}

@Composable
private fun CardMeter(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$value", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(
            progress = { (value / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun DiscoverScreen(
    candidates: List<ScoredCandidate>,
    filters: DiscoverFilters,
    onUpdateFilters: (DiscoverFilters) -> Unit,
    onSwipe: (ScoredCandidate, String, String?, Boolean) -> Unit,
    onSendRequest: (PublicProfile, String, Boolean) -> Unit,
    onBlockAndReport: (PublicProfile, ReportCategory?, String, Boolean) -> Unit,
    canSendRose: Boolean,
    rosesUsedWeek: Int
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<ScoredCandidate?>(null) }
    var selectedForRequest by remember { mutableStateOf<PublicProfile?>(null) }
    var selectedForShield by remember { mutableStateOf<PublicProfile?>(null) }

    val filtered = remember(candidates, filters) {
        candidates.filter { s ->
            val p = s.candidate.profile
            p.age in filters.minAge..filters.maxAge &&
                (filters.lookingFor == null || p.lookingFor == filters.lookingFor) &&
                (!filters.verifiedOnly || p.selfieVerified)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Discover", style = MaterialTheme.typography.headlineMedium)
                Text("${filtered.size} mutual-fit profiles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier.testTag("discover_filter_button")
            ) {
                Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Filters")
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.padding(16.dp)) {
                EmptyPoolCard(
                    title = "No profiles match these filters",
                    subtitle = "Widen your age or goal filter to see more compatible people."
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 165.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.candidate.profile.userId }) { scored ->
                    val p = scored.candidate.profile
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProfile = scored }
                    ) {
                        Column {
                            Box(modifier = Modifier.height(175.dp).fillMaxWidth()) {
                                if (scored.candidate.photoRes != null) {
                                    Image(
                                        painter = painterResource(id = scored.candidate.photoRes),
                                        contentDescription = p.firstName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "${scored.matchPercent}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${p.firstName}, ${p.age}", style = MaterialTheme.typography.titleMedium)
                                    if (p.selfieVerified) {
                                        Icon(Icons.Filled.VerifiedUser, contentDescription = "Verified", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(scored.distanceBand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(LookingFor.fromCode(p.lookingFor).label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        DiscoverFilterDialog(
            filters = filters,
            onApply = {
                onUpdateFilters(it)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    selectedProfile?.let { s ->
        ProfileDetailSheet(
            scored = s,
            onDismiss = { selectedProfile = null },
            onLike = {
                onSwipe(s, "like", null, false)
                selectedProfile = null
            },
            onSendOneMessage = {
                selectedForRequest = s.candidate.profile
                selectedProfile = null
            },
            onShield = {
                selectedForShield = s.candidate.profile
                selectedProfile = null
            }
        )
    }

    selectedForRequest?.let { target ->
        SendOneMessageDialog(
            target = target,
            canSendRose = canSendRose,
            rosesUsedWeek = rosesUsedWeek,
            onSend = { body, rose ->
                onSendRequest(target, body, rose)
                selectedForRequest = null
            },
            onDismiss = { selectedForRequest = null }
        )
    }

    selectedForShield?.let { target ->
        BlockAndReportSheet(
            targetName = target.firstName,
            onDismiss = { selectedForShield = null },
            onBlockAndReport = { cat, details ->
                onBlockAndReport(target, cat, details, true)
                selectedForShield = null
            },
            onBlockOnly = {
                onBlockAndReport(target, null, "", false)
                selectedForShield = null
            }
        )
    }
}

@Composable
fun NearbyScreen(
    ownProfile: PublicProfile?,
    nearbyCandidates: List<ScoredCandidate>,
    onToggleShowInNearby: (Boolean) -> Unit,
    onSwipeLike: (ScoredCandidate) -> Unit,
    onSendRequest: (PublicProfile, String, Boolean) -> Unit,
    onBlockAndReport: (PublicProfile, ReportCategory?, String, Boolean) -> Unit,
    canSendRose: Boolean,
    rosesUsedWeek: Int
) {
    var selectedProfile by remember { mutableStateOf<ScoredCandidate?>(null) }
    var selectedForRequest by remember { mutableStateOf<PublicProfile?>(null) }
    var selectedForShield by remember { mutableStateOf<PublicProfile?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nearby", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Distance bands only (≥ 1 km, snapped to a 500 m grid). Mutual filters and Protected Place pins are always enforced.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show me in Nearby", style = MaterialTheme.typography.titleSmall)
                            Text("Turn off to browse privately without appearing here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = ownProfile?.showInNearby ?: true,
                            onCheckedChange = onToggleShowInNearby,
                            modifier = Modifier.testTag("show_in_nearby_switch")
                        )
                    }
                }
            }
        }

        if (nearbyCandidates.isEmpty()) {
            item {
                EmptyPoolCard(
                    title = "No profiles in your Nearby bands right now",
                    subtitle = "Protected Place pins or mutual filters may be keeping colleagues and neighbours hidden."
                )
            }
        } else {
            items(nearbyCandidates, key = { it.candidate.profile.userId }) { scored ->
                val p = scored.candidate.profile
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedProfile = scored }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (scored.candidate.photoRes != null) {
                            Image(
                                painter = painterResource(id = scored.candidate.photoRes),
                                contentDescription = p.firstName,
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${p.firstName}, ${p.age}", style = MaterialTheme.typography.titleMedium)
                                if (p.selfieVerified) {
                                    Icon(Icons.Filled.VerifiedUser, contentDescription = "Verified", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("${scored.distanceBand} · ${p.area}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { selectedForRequest = p },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("nearby_message_button_${p.userId}")
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("Send 1 message", style = MaterialTheme.typography.labelMedium)
                                }
                                IconButton(onClick = { selectedForShield = p }) {
                                    Icon(Icons.Filled.Shield, contentDescription = "Report or block ${p.firstName}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedProfile?.let { s ->
        ProfileDetailSheet(
            scored = s,
            onDismiss = { selectedProfile = null },
            onLike = {
                onSwipeLike(s)
                selectedProfile = null
            },
            onSendOneMessage = {
                selectedForRequest = s.candidate.profile
                selectedProfile = null
            },
            onShield = {
                selectedForShield = s.candidate.profile
                selectedProfile = null
            }
        )
    }

    selectedForRequest?.let { target ->
        SendOneMessageDialog(
            target = target,
            canSendRose = canSendRose,
            rosesUsedWeek = rosesUsedWeek,
            onSend = { body, rose ->
                onSendRequest(target, body, rose)
                selectedForRequest = null
            },
            onDismiss = { selectedForRequest = null }
        )
    }

    selectedForShield?.let { target ->
        BlockAndReportSheet(
            targetName = target.firstName,
            onDismiss = { selectedForShield = null },
            onBlockAndReport = { cat, details ->
                onBlockAndReport(target, cat, details, true)
                selectedForShield = null
            },
            onBlockOnly = {
                onBlockAndReport(target, null, "", false)
                selectedForShield = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailSheet(
    scored: ScoredCandidate,
    onDismiss: () -> Unit,
    onLike: () -> Unit,
    onSendOneMessage: (() -> Unit)? = null,
    onShield: () -> Unit
) {
    val p = scored.candidate.profile
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${p.firstName}, ${p.age}", style = MaterialTheme.typography.headlineSmall)
                    if (p.selfieVerified) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = "Selfie Verified", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                IconButton(onClick = onShield) {
                    Icon(Icons.Filled.Shield, contentDescription = "Report or Block")
                }
            }

            if (scored.candidate.photoRes != null) {
                Image(
                    painter = painterResource(id = scored.candidate.photoRes),
                    contentDescription = p.firstName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Concrete trust facts, never raw trust number (§D8.4)
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                val badgeFacts = buildString {
                    append(if (p.selfieVerified) "Selfie verified" else "Unverified")
                    if (scored.candidate.completedDates > 0) append(" · ${scored.candidate.completedDates} dates completed")
                    append(" · replies ${scored.candidate.repliesTypical}")
                }
                Text(
                    text = badgeFacts,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Text("${p.area} (${scored.distanceBand}) · ${p.work}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(p.bio, style = MaterialTheme.typography.bodyLarge)

            if (scored.privateAlignmentSummary != null) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = "Private compatibility: ${scored.privateAlignmentSummary}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (onSendOneMessage != null) {
                    OutlinedButton(onClick = onSendOneMessage, modifier = Modifier.weight(1f)) {
                        Text("Send 1 message")
                    }
                }
                Button(onClick = onLike, modifier = Modifier.weight(1f)) {
                    Text("Like")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WhyScoreDialog(scored: ScoredCandidate, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Why ${scored.matchPercent}% compatibility?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• Interests & Bio fit: ${scored.interestsScore}%")
                Text("• Distance (${scored.distanceBand}): ${scored.distanceScore}%")
                Text("• Recent activity: ${scored.activeScore}%")
                Text("• Profile verification & conduct: ${scored.verifiedScore}%")
                if (scored.privateAlignmentSummary != null) {
                    Text("• Private alignment: ${scored.privateAlignmentSummary}")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun LikeWithNoteDialog(
    scored: ScoredCandidate,
    canSendRose: Boolean,
    rosesUsedWeek: Int,
    onSend: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    var attachRose by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reply to ${scored.candidate.profile.firstName}'s prompt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (scored.candidate.profile.promptAnswer.isNotBlank()) {
                    Text(
                        text = "\"${scored.candidate.profile.promptAnswer}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text("Your thoughtful note (max 200 chars)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = attachRose,
                        onCheckedChange = { attachRose = it },
                        enabled = canSendRose
                    )
                    Icon(Icons.Filled.LocalFlorist, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (canSendRose) " Attach a Rose ($rosesUsedWeek of 5 used this week)"
                        else " Roses unlock after Selfie Verification",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSend(note, attachRose) }, enabled = note.isNotBlank()) {
                Text("Send Like + Note")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SendOneMessageDialog(
    target: PublicProfile,
    canSendRose: Boolean,
    rosesUsedWeek: Int,
    onSend: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var body by remember { mutableStateOf("") }
    var attachRose by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("One message request to ${target.firstName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "You get 1 message before matching (max 200 chars). No links, handles, or phone numbers. Full chat opens only if ${target.firstName} replies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it.take(200) },
                    label = { Text("Write a respectful opener (${body.length}/200)") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("one_message_request_input")
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = attachRose,
                        onCheckedChange = { attachRose = it },
                        enabled = canSendRose
                    )
                    Text(
                        text = if (canSendRose) "Attach Rose ($rosesUsedWeek of 5/week)" else "Verify selfie to attach a Rose",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(body, attachRose) },
                enabled = body.isNotBlank(),
                modifier = Modifier.testTag("send_one_message_confirm")
            ) {
                Text("Send Request")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverFilterDialog(
    filters: DiscoverFilters,
    onApply: (DiscoverFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var verifiedOnly by remember { mutableStateOf(filters.verifiedOnly) }
    var lookingFor by remember { mutableStateOf(filters.lookingFor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discover Filters") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Verified profiles only", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = verifiedOnly, onCheckedChange = { verifiedOnly = it })
                }
                Text("Looking for", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = lookingFor == null,
                        onClick = { lookingFor = null },
                        label = { Text("Any") }
                    )
                    LookingFor.entries.forEach { lf ->
                        FilterChip(
                            selected = lookingFor == lf.code,
                            onClick = { lookingFor = lf.code },
                            label = { Text(lf.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApply(filters.copy(verifiedOnly = verifiedOnly, lookingFor = lookingFor)) }) {
                Text("Apply")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EmptyPoolCard(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
