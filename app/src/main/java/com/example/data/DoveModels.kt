package com.example.data

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class LookingFor(val code: String, val label: String) {
    LONG_TERM("long_term", "Long-term partner"),
    LONG_TERM_OPEN("long_term_open_to_short", "Long-term, open to short"),
    SHORT_TERM_OPEN("short_term_open_to_long", "Short-term, open to long"),
    SHORT_TERM_FUN("short_term_fun", "Short-term fun"),
    NEW_FRIENDS("new_friends", "New friends"),
    STILL_FIGURING_OUT("still_figuring_out", "Still figuring it out");

    companion object {
        fun fromCode(raw: String?): LookingFor =
            entries.firstOrNull { it.code == raw } ?: LONG_TERM
    }
}

enum class GenderOption(val code: String, val label: String) {
    WOMAN("woman", "Woman"),
    MAN("man", "Man"),
    NONBINARY("nonbinary", "Non-binary"),
    SELF_DESCRIBE("self_describe", "Self-describe");

    companion object {
        fun fromCode(raw: String?): GenderOption =
            entries.firstOrNull { it.code == raw } ?: WOMAN
    }
}

enum class WhoCanMessageFirst(val code: String, val label: String, val summary: String) {
    VERIFIED_INBOX("verified_inbox", "Verified to Requests (Recommended)", "Selfie-verified senders land in Requests; unverified go to Filtered."),
    VERIFIED_ONLY("verified_only", "Verified accounts only", "Only selfie-verified people can send you a message request."),
    OFF("off", "Matches only (Off)", "No pre-match message requests; chat opens only on mutual Like.");

    companion object {
        fun fromCode(raw: String?): WhoCanMessageFirst =
            entries.firstOrNull { it.code == raw } ?: VERIFIED_INBOX
    }
}

enum class ReportCategory(val code: String, val label: String) {
    SCAM("scam", "Scam or money request"),
    HARASSMENT("harassment", "Harassment or threats"),
    EXPLICIT("explicit", "Unsolicited explicit content"),
    MINOR("minor", "Under-18 / Minor safety concern"),
    IMPERSONATION("impersonation", "Fake or stolen photos"),
    SOLICITATION("solicitation", "Commercial / solicitation"),
    SPAM("spam", "Spam or bot behavior");

    companion object {
        fun fromCode(raw: String?): ReportCategory =
            entries.firstOrNull { it.code == raw } ?: SCAM
    }
}

enum class CapabilityTier(
    val label: String,
    val dailyPicks: Int,
    val likesPerDay: Int,
    val likesWithNotePerDay: Int,
    val firstMessagesPerDay: Int,
    val requestsPerDay: Int,
    val rosesPerWeek: Int,
    val maxPhotos: Int
) {
    UNVERIFIED("Unverified", 5, 10, 0, 3, 1, 0, 3),
    SELFIE_VERIFIED("Selfie Verified", 15, 50, 3, 20, 5, 5, 6)
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val age: Int = 18,
    val dobYear: Int = 1998,
    val dobMonth: Int = 1,
    val dobDay: Int = 1,
    val termsAccepted: Boolean = false,
    val safetyConsent: Boolean = false,
    val browseOnly: Boolean = true,
    val selfieVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val phoneE164: String = "",
    val country: String = "KE",
    val locale: String = "en",
    val whoCanMessageFirst: String = "verified_inbox",
    val allowRoses: Boolean = true,
    val standoutsMuted: Boolean = false,
    val liteMode: Boolean = false,
    val rosesBalance: Int = 1,
    val rosesSentWeek: Int = 0,
    val strikes: Int = 0,
    val suspensionUntilMs: Long = 0L,
    val suspensionReason: String = "",
    val onboardingComplete: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivatePrefs(
    @DocumentId val id: String = "",
    val userId: String = "",
    val consented: Boolean = false,
    val consentVersion: Int = 1,
    val pace: String = "skip",
    val paceDealbreaker: Boolean = false,
    val exclusivity: String = "skip",
    val exclusivityDealbreaker: Boolean = false,
    val chemWeight: String = "skip",
    val discretion: String = "skip",
    val flirtLevel: String = "skip",
    val place: String = "skip",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val firstName: String = "",
    val age: Int = 25,
    val gender: String = "woman",
    val interestedIn: List<String> = emptyList(),
    val area: String = "Kilimani",
    val lat: Double = -1.2921,
    val lng: Double = 36.7856,
    val lookingFor: String = "long_term",
    val bio: String = "",
    val promptQuestion: String = "My ideal Sunday",
    val promptAnswer: String = "",
    val interests: List<String> = emptyList(),
    val languages: List<String> = listOf("en"),
    val work: String = "",
    val selfieVerified: Boolean = false,
    val trustScore: Int = 70,
    val prefAgeMin: Int = 21,
    val prefAgeMax: Int = 45,
    val distMaxKm: Int = 25,
    val verifiedOnly: Boolean = true,
    val showInNearby: Boolean = true,
    val paused: Boolean = false,
    val incognito: Boolean = false,
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProtectedPin(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val areaName: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val radiusM: Int = 2000,
    val active: Boolean = true,
    val pausedUntilMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SwipeRecord(
    @DocumentId val id: String = "",
    val actorId: String = "",
    val targetId: String = "",
    val targetName: String = "",
    val targetArea: String = "",
    val action: String = "like",
    val note: String = "",
    val roseAttached: Boolean = false,
    val reintroduced: Boolean = false,
    val ladderUsed: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BlockRecord(
    @DocumentId val id: String = "",
    val blockerId: String = "",
    val blockedUid: String = "",
    val blockedName: String = "",
    val reasonCategory: String = "",
    val evidenceSnapshot: String = "",
    val evidenceRetainedUntilMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SafeDateRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val connectionId: String = "",
    val partnerName: String = "",
    val place: String = "",
    val meetAtLabel: String = "",
    val trustedContact: String = "",
    val status: String = "scheduled",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MessageRequestRecord(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderArea: String = "",
    val senderVerified: Boolean = false,
    val receiverId: String = "",
    val receiverName: String = "",
    val memberUids: List<String> = emptyList(),
    val body: String = "",
    val status: String = "pending",
    val roseAttached: Boolean = false,
    val sensitive: Boolean = false,
    val effortReasons: List<String> = emptyList(),
    val riskCategory: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ConnectionRecord(
    @DocumentId val id: String = "",
    val memberUids: List<String> = emptyList(),
    val userAId: String = "",
    val userAName: String = "",
    val userBId: String = "",
    val userBName: String = "",
    val userBArea: String = "",
    val userBVerified: Boolean = false,
    val lastMessagePreview: String = "",
    val nudgeCount: Int = 0,
    val nudgeBanner: String = "",
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessageRecord(
    @DocumentId val id: String = "",
    val connectionId: String = "",
    val memberUids: List<String> = emptyList(),
    val senderId: String = "",
    val body: String = "",
    val status: String = "sent",
    val reasonCode: String = "",
    val nudgeCategory: String = "",
    val roseAttached: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SafetyReportRecord(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetUid: String = "",
    val targetName: String = "",
    val category: String = "scam",
    val details: String = "",
    val filedAfterBlock: Boolean = false,
    val bulk: Boolean = false,
    val evidenceSnippet: String = "",
    val outcome: String = "reviewing",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class CompanionCandidate(
    val profile: PublicProfile,
    @DrawableRes val photoRes: Int?,
    val daysSinceActive: Double,
    val effortTowardUser: Double = 0.0,
    val privateAlignmentStub: PrivatePrefs? = null, // Used in local scoring simulation ONLY, never shown in UI
    val likesUserBack: Boolean = false,
    val completedDates: Int = 0,
    val repliesTypical: String = "within a day"
)

data class ScoredCandidate(
    val candidate: CompanionCandidate,
    val matchPercent: Int,
    val interestsScore: Int,
    val distanceScore: Int,
    val activeScore: Int,
    val verifiedScore: Int,
    val distanceBand: String,
    val privateAlignmentSummary: String?
)

data class AreaLocation(
    val name: String,
    val lat: Double,
    val lng: Double
)

data class ProfileDraftResult(
    val firstName: String = "",
    val gender: String? = null,
    val interestedIn: List<String> = emptyList(),
    val area: String? = null,
    val lookingFor: String? = null,
    val bio: String = "",
    val interests: List<String> = emptyList(),
    val languages: List<String> = listOf("en"),
    val work: String = "",
    val clarifyQuestion: String? = null
)
