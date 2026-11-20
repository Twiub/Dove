package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.DoveSeedCatalog
import com.example.data.GenderOption
import com.example.data.LookingFor
import com.example.viewmodel.OnboardingDraft
import com.example.viewmodel.OnboardingStep

@Composable
fun Under18BlockedScreen() {
    BackHandler(enabled = true) { /* Hard block — cannot navigate back */ }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Access unavailable",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Dove is strictly for adults 18 and older. Based on the date of birth entered, registration is blocked on this device and no profile data was saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun OnboardingFlowScreen(
    step: OnboardingStep,
    draft: OnboardingDraft,
    aiLoading: Boolean,
    aiError: String?,
    aiClarifyHint: String?,
    onSubmitAge: (Int, Int, Int) -> Unit,
    onAcceptTerms: () -> Unit,
    onSubmitSafetyConsent: (Boolean, String) -> Unit,
    onRunAiExtract: (String) -> Unit,
    onUpdateDraft: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
    onSelfieVerified: () -> Unit,
    onSavePrivatePrefs: (Boolean, String, Boolean, String, Boolean, String, String, String, String) -> Unit,
    onNextStep: (OnboardingStep) -> Unit,
    onPublish: () -> Unit,
    onReportConcern: (String, String) -> Unit
) {
    var showChildSafetySheet by remember { mutableStateOf(false) }

    val progress = (step.ordinal + 1).toFloat() / OnboardingStep.entries.size.toFloat()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 560.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Step ${step.ordinal + 1} of ${OnboardingStep.entries.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { showChildSafetySheet = true }) {
                        Text("Child Safety")
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    OnboardingStep.AGE_SCREEN -> AgeGateStep(
                        initialYear = draft.dobYear,
                        initialMonth = draft.dobMonth,
                        initialDay = draft.dobDay,
                        onContinue = onSubmitAge
                    )
                    OnboardingStep.TERMS_GATE -> TermsGateStep(
                        onOpenChildSafety = { showChildSafetySheet = true },
                        onAccept = onAcceptTerms
                    )
                    OnboardingStep.SAFETY_CONSENT -> SafetyConsentStep(
                        onConsent = onSubmitSafetyConsent
                    )
                    OnboardingStep.PROFILE_BUILDER -> ProfileBuilderStep(
                        draft = draft,
                        aiLoading = aiLoading,
                        aiError = aiError,
                        aiClarifyHint = aiClarifyHint,
                        onRunAiExtract = onRunAiExtract,
                        onUpdateDraft = onUpdateDraft,
                        onNext = { onNextStep(OnboardingStep.PHOTOS_AND_VERIFY) }
                    )
                    OnboardingStep.PHOTOS_AND_VERIFY -> PhotosAndVerifyStep(
                        draft = draft,
                        onUpdateDraft = onUpdateDraft,
                        onSelfieVerified = onSelfieVerified,
                        onNext = { onNextStep(OnboardingStep.PRIVATE_COMPATIBILITY) }
                    )
                    OnboardingStep.PRIVATE_COMPATIBILITY -> PrivateCompatibilityStep(
                        onSaveAndContinue = { consented, pace, pDb, excl, eDb, chem, disc, flirt, place ->
                            if (consented) {
                                onSavePrivatePrefs(true, pace, pDb, excl, eDb, chem, disc, flirt, place)
                            }
                            onNextStep(OnboardingStep.PREFERENCES)
                        },
                        onSkip = { onNextStep(OnboardingStep.PREFERENCES) }
                    )
                    OnboardingStep.PREFERENCES -> PreferencesPublishStep(
                        draft = draft,
                        onUpdateDraft = onUpdateDraft,
                        onPublish = onPublish
                    )
                }
            }
        }
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
}

@Composable
private fun AgeGateStep(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onContinue: (Int, Int, Int) -> Unit
) {
    // Neutral DOB entry: never display "you must be 18" hint before input (§D3.1)
    var yearText by remember { mutableStateOf(initialYear.toString()) }
    var monthText by remember { mutableStateOf(initialMonth.toString()) }
    var dayText by remember { mutableStateOf(initialDay.toString()) }

    Text(
        text = "When is your birthday?",
        style = MaterialTheme.typography.headlineMedium
    )
    Text(
        text = "Enter your date of birth to set up your profile. Your age is displayed; your exact birth date stays private.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = dayText,
            onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) dayText = it },
            label = { Text("Day") },
            modifier = Modifier
                .weight(1f)
                .testTag("dob_day_input"),
            singleLine = true
        )
        OutlinedTextField(
            value = monthText,
            onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) monthText = it },
            label = { Text("Month") },
            modifier = Modifier
                .weight(1f)
                .testTag("dob_month_input"),
            singleLine = true
        )
        OutlinedTextField(
            value = yearText,
            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) yearText = it },
            label = { Text("Year") },
            modifier = Modifier
                .weight(1.4f)
                .testTag("dob_year_input"),
            singleLine = true
        )
    }

    val y = yearText.toIntOrNull()
    val m = monthText.toIntOrNull()
    val d = dayText.toIntOrNull()
    val validDate = y != null && y in 1920..2025 && m != null && m in 1..12 && d != null && d in 1..31

    Button(
        onClick = { if (validDate) onContinue(y!!, m!!, d!!) },
        enabled = validDate,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("dob_continue_button")
    ) {
        Text("Continue")
    }
}

@Composable
private fun TermsGateStep(
    onOpenChildSafety: () -> Unit,
    onAccept: () -> Unit
) {
    var agreed by remember { mutableStateOf(false) }

    Text(
        text = "Community Guidelines & Terms",
        style = MaterialTheme.typography.headlineMedium
    )
    Text(
        text = "Please review and accept Dove's Terms before creating your profile.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("• Keep all chats respectful, honest, and free of harassment or solicitation.", style = MaterialTheme.typography.bodyMedium)
            Text("• Never ask matches for money, airtime, transport fare, or verification codes.", style = MaterialTheme.typography.bodyMedium)
            Text("• Public profiles and photos must remain appropriate and non-explicit.", style = MaterialTheme.typography.bodyMedium)
            Text("• Zero tolerance for child sexual abuse or minor exploitation (CSAE).", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onOpenChildSafety) {
                Text("Read Child Safety Standards & Report Concern")
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = agreed,
            onCheckedChange = { agreed = it },
            modifier = Modifier.testTag("terms_checkbox")
        )
        Text(
            text = "I accept Dove's Terms of Use, Privacy Policy, and Community Guidelines.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 6.dp)
        )
    }

    Button(
        onClick = onAccept,
        enabled = agreed,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("terms_accept_button")
    ) {
        Text("Accept & Continue")
    }
}

@Composable
private fun SafetyConsentStep(
    onConsent: (Boolean, String) -> Unit
) {
    var safetyAgreed by remember { mutableStateOf(false) }
    var phoneE164 by remember { mutableStateOf("+254") }

    Text(
        text = "Safety & Message Scanning",
        style = MaterialTheme.typography.headlineMedium
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Messages are scanned automatically to catch scams and abuse. We can read them to do this; chats are not end-to-end encrypted.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Scanning checks for contact leaks, payment/airtime scams, threats, and minor safety signals. Never used for ads.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }

    OutlinedTextField(
        value = phoneE164,
        onValueChange = { phoneE164 = it },
        label = { Text("Phone number (optional E.164, e.g. +254712345678)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = safetyAgreed,
            onCheckedChange = { safetyAgreed = it },
            modifier = Modifier.testTag("safety_consent_checkbox")
        )
        Text(
            text = "I understand chats are scanned for safety and agree to enable messaging.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 6.dp)
        )
    }

    Button(
        onClick = { onConsent(true, phoneE164) },
        enabled = safetyAgreed,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("safety_consent_continue_button")
    ) {
        Text("Enable Chat & Continue")
    }

    OutlinedButton(
        onClick = { onConsent(false, phoneE164) },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("browse_only_button")
    ) {
        Text("Skip for now (Browse Only)")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileBuilderStep(
    draft: OnboardingDraft,
    aiLoading: Boolean,
    aiError: String?,
    aiClarifyHint: String?,
    onRunAiExtract: (String) -> Unit,
    onUpdateDraft: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
    onNext: () -> Unit
) {
    var freeText by remember { mutableStateOf("") }

    Text(
        text = "Build your profile",
        style = MaterialTheme.typography.headlineMedium
    )
    Text(
        text = "Describe yourself naturally and Dove AI will draft your profile for you to review, or fill the fields below directly.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedTextField(
        value = freeText,
        onValueChange = { freeText = it },
        label = { Text("Tell us about yourself") },
        placeholder = { Text("e.g. I'm Sam in Kilimani, software dev. Love hiking Ngong Hills and nyama choma. Looking for something serious.") },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_profile_prompt_input"),
        minLines = 3
    )

    Button(
        onClick = { onRunAiExtract(freeText) },
        enabled = !aiLoading && freeText.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_extract_button")
    ) {
        if (aiLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Drafting with Gemini AI...")
        } else {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Draft Profile with AI")
        }
    }

    if (aiError != null) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(aiError, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
    if (aiClarifyHint != null) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Text(aiClarifyHint, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }

    OutlinedTextField(
        value = draft.firstName,
        onValueChange = { v -> onUpdateDraft { it.copy(firstName = v.take(30)) } },
        label = { Text("First name") },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("draft_name_input"),
        singleLine = true
    )

    Text("I identify as", style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GenderOption.entries.forEach { g ->
            FilterChip(
                selected = draft.gender == g.code,
                onClick = { onUpdateDraft { it.copy(gender = g.code) } },
                label = { Text(g.label) }
            )
        }
    }

    Text("Interested in", style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("women" to "Women", "men" to "Men", "nonbinary" to "Non-binary").forEach { (code, label) ->
            val selected = code in draft.interestedIn
            FilterChip(
                selected = selected,
                onClick = {
                    onUpdateDraft { d ->
                        val next = if (selected && d.interestedIn.size > 1) d.interestedIn - code
                        else (d.interestedIn + code).distinct()
                        d.copy(interestedIn = next)
                    }
                },
                label = { Text(label) }
            )
        }
    }

    Text("Neighbourhood / Area", style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DoveSeedCatalog.areas.take(6).forEach { area ->
            FilterChip(
                selected = draft.areaName == area.name,
                onClick = { onUpdateDraft { it.copy(areaName = area.name) } },
                label = { Text(area.name) }
            )
        }
    }

    Text("Looking for", style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LookingFor.entries.forEach { lf ->
            FilterChip(
                selected = draft.lookingFor == lf.code,
                onClick = { onUpdateDraft { it.copy(lookingFor = lf.code) } },
                label = { Text(lf.label) }
            )
        }
    }

    OutlinedTextField(
        value = draft.bio,
        onValueChange = { v -> onUpdateDraft { it.copy(bio = v.take(300)) } },
        label = { Text("Bio (your own words, max 300 chars)") },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("draft_bio_input"),
        minLines = 2
    )

    OutlinedTextField(
        value = draft.promptAnswer,
        onValueChange = { v -> onUpdateDraft { it.copy(promptAnswer = v.take(200)) } },
        label = { Text("Prompt: ${draft.promptQuestion}") },
        modifier = Modifier.fillMaxWidth()
    )

    Button(
        onClick = onNext,
        enabled = draft.firstName.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("profile_builder_next_button")
    ) {
        Text("Next: Photos & Verification")
    }
}

@Composable
private fun PhotosAndVerifyStep(
    draft: OnboardingDraft,
    onUpdateDraft: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
    onSelfieVerified: () -> Unit,
    onNext: () -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateDraft { d -> d.copy(photoUris = (d.photoUris + uri.toString()).distinct().take(6)) }
        }
    }

    val challenges = remember {
        listOf(
            "Look straight ahead and smile",
            "Turn head slightly left, then blink",
            "Hold up two fingers beside your cheek"
        )
    }
    var challengeIdx by remember { mutableIntStateOf(0) }

    Text(
        text = "Photos & Selfie Verification",
        style = MaterialTheme.typography.headlineMedium
    )
    Text(
        text = "Add photos with the Android Photo Picker (no broad storage access needed) and complete a quick selfie liveness check to unlock full Picks, Requests, and weekly Roses.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedButton(
        onClick = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pick_photo_button")
    ) {
        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Select Photos (${draft.photoUris.size}/6)")
    }

    if (draft.photoUris.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            draft.photoUris.take(3).forEach { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected profile photo",
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (draft.selfieVerified) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (draft.selfieVerified) Icons.Filled.VerifiedUser else Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = if (draft.selfieVerified) "Selfie Verified (Established Tier)" else "Selfie Liveness Challenge",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (!draft.selfieVerified) {
                Text(
                    text = "Pose prompt: \"${challenges[challengeIdx]}\"",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Biometric notice: Your selfie is matched once against Photo 1 and deleted immediately — only the verified result is stored.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { challengeIdx = (challengeIdx + 1) % challenges.size }) {
                        Text("New Pose")
                    }
                    Button(
                        onClick = onSelfieVerified,
                        modifier = Modifier.testTag("complete_selfie_verify_button")
                    ) {
                        Text("Verify Selfie")
                    }
                }
            } else {
                Text(
                    text = "Verified! 15 Daily Picks, 5 Message Requests/day, and 1 free weekly Rose unlocked.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    Button(
        onClick = onNext,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("photos_next_button")
    ) {
        Text("Continue")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrivateCompatibilityStep(
    onSaveAndContinue: (Boolean, String, Boolean, String, Boolean, String, String, String, String) -> Unit,
    onSkip: () -> Unit
) {
    var consented by remember { mutableStateOf(false) }
    var pace by remember { mutableStateOf("gradual") }
    var paceDb by remember { mutableStateOf(false) }
    var excl by remember { mutableStateOf("exclusive") }
    var exclDb by remember { mutableStateOf(false) }
    var chem by remember { mutableStateOf("medium") }
    var disc by remember { mutableStateOf("selective") }
    var flirt by remember { mutableStateOf("sweet") }
    var place by remember { mutableStateOf("skip") }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Private Compatibility (Optional)", style = MaterialTheme.typography.headlineSmall)
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "These answers are private. Nobody sees them. They only adjust your compatibility %.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = consented, onCheckedChange = { consented = it })
                Text("I consent to using these private answers for matching % only.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (consented) {
        Text("Pace", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("slow" to "Slow", "gradual" to "Gradual", "natural" to "Natural", "open" to "Open", "skip" to "Skip").forEach { (k, l) ->
                FilterChip(selected = pace == k, onClick = { pace = k }, label = { Text(l) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Mark Pace as a dealbreaker filter", style = MaterialTheme.typography.bodySmall)
            Switch(checked = paceDb, onCheckedChange = { paceDb = it })
        }

        Text("Relationship structure", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("exclusive" to "Exclusive", "open_to_discuss" to "Open to discuss", "non_exclusive" to "Non-exclusive", "skip" to "Skip").forEach { (k, l) ->
                FilterChip(selected = excl == k, onClick = { excl = k }, label = { Text(l) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Mark Exclusivity as a dealbreaker filter", style = MaterialTheme.typography.bodySmall)
            Switch(checked = exclDb, onCheckedChange = { exclDb = it })
        }

        Text("Chemistry priority", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("low" to "Low", "medium" to "Medium", "high" to "High", "skip" to "Skip").forEach { (k, l) ->
                FilterChip(selected = chem == k, onClick = { chem = k }, label = { Text(l) })
            }
        }

        Text("Chat tone", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("sweet" to "Sweet", "flirty" to "Flirty", "bold" to "Playful & bold", "skip" to "Skip").forEach { (k, l) ->
                FilterChip(selected = flirt == k, onClick = { flirt = k }, label = { Text(l) })
            }
        }

        Text("Visibility preference", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("public" to "Open", "selective" to "Selective", "private" to "Private", "skip" to "Skip").forEach { (k, l) ->
                FilterChip(selected = disc == k, onClick = { disc = k }, label = { Text(l) })
            }
        }

        Text("Hosting space (private only, never shown)", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("own_place" to "Own place", "shared_place" to "Shared", "no_place" to "None", "skip" to "Skip").forEach { (k, l) ->
                FilterChip(selected = place == k, onClick = { place = k }, label = { Text(l) })
            }
        }
    }

    Button(
        onClick = { onSaveAndContinue(consented, pace, paceDb, excl, exclDb, chem, disc, flirt, place) },
        enabled = consented,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("save_private_prefs_button")
    ) {
        Text("Save Private Preferences")
    }

    OutlinedButton(
        onClick = onSkip,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("skip_private_prefs_button")
    ) {
        Text("Skip (No Penalty)")
    }
}

@Composable
private fun PreferencesPublishStep(
    draft: OnboardingDraft,
    onUpdateDraft: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
    onPublish: () -> Unit
) {
    Text(
        text = "Discovery Preferences",
        style = MaterialTheme.typography.headlineMedium
    )
    Text(
        text = "Filters work both ways on Dove — you only see people whose preferences also include you.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Text("Age range: ${draft.prefAgeMin} – ${draft.prefAgeMax}", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Min: ${draft.prefAgeMin}", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = draft.prefAgeMin.toFloat(),
                onValueChange = { v -> onUpdateDraft { it.copy(prefAgeMin = v.toInt().coerceAtMost(it.prefAgeMax)) } },
                valueRange = 18f..65f
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Max: ${draft.prefAgeMax}", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = draft.prefAgeMax.toFloat(),
                onValueChange = { v -> onUpdateDraft { it.copy(prefAgeMax = v.toInt().coerceAtLeast(it.prefAgeMin)) } },
                valueRange = 18f..75f
            )
        }
    }

    Text("Max distance: ${draft.distMaxKm} km", style = MaterialTheme.typography.titleSmall)
    Slider(
        value = draft.distMaxKm.toFloat(),
        onValueChange = { v -> onUpdateDraft { it.copy(distMaxKm = v.toInt()) } },
        valueRange = 5f..100f
    )

    Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Verified profiles only", style = MaterialTheme.typography.titleSmall)
                Text("Only match with selfie-verified adults (turn off anytime to browse everyone).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = draft.verifiedOnly,
                onCheckedChange = { v -> onUpdateDraft { it.copy(verifiedOnly = v) } }
            )
        }
    }

    Button(
        onClick = onPublish,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("publish_profile_button")
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Publish Profile & Start")
    }
}
