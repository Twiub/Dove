package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Firestore Repository strictly aligned 1:1 with firestore.rules and firebase-blueprint.json.
 * Uses the named database R.string.firestore_database_id exclusively.
 */
class DoveRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.handleFirestoreStreamError(op: OperationType, path: String, uid: String): Flow<T> =
        this.catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val ex = fsErr ?: (error as? Exception)
            if (ex != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(ex, op, path)
            }
            throw error
        }

    fun observeUserAccount(): Flow<UserAccount?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFirestoreStreamError(OperationType.GET, path, uid)
        )
    }

    fun observeOwnPublicProfile(): Flow<PublicProfile?> = flow {
        val uid = requireUserId()
        val path = "publicProfiles/$uid"
        emitAll(
            db.collection("publicProfiles").document(uid)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFirestoreStreamError(OperationType.GET, path, uid)
        )
    }

    fun observePublicProfiles(): Flow<List<PublicProfile>> = flow {
        val uid = requireUserId()
        val path = "publicProfiles"
        emitAll(
            db.collection("publicProfiles")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFirestoreStreamError(OperationType.LIST, path, uid)
        )
    }

    fun observePrivatePrefs(): Flow<PrivatePrefs?> = flow {
        val uid = requireUserId()
        val path = "users/$uid/privatePrefs/compatibility"
        emitAll(
            db.collection("users").document(uid).collection("privatePrefs").document("compatibility")
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(PrivatePrefs::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFirestoreStreamError(OperationType.GET, path, uid)
        )
    }

    fun observePins(): Flow<List<ProtectedPin>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/pins"
        emitAll(
            db.collection("users").document(uid).collection("pins")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ProtectedPin::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFirestoreStreamError(OperationType.LIST, path, uid)
        )
    }

    fun observeSwipes(): Flow<List<SwipeRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/swipes"
        emitAll(
            db.collection("users").document(uid).collection("swipes")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SwipeRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFirestoreStreamError(OperationType.LIST, path, uid)
        )
    }

    fun observeBlocks(): Flow<List<BlockRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/blocks"
        emitAll(
            db.collection("users").document(uid).collection("blocks")
                .snapshots()
                .map { snap ->
                    snap.toObjects(BlockRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFirestoreStreamError(OperationType.LIST, path, uid)
        )
    }

    fun observeSafeDates(): Flow<List<SafeDateRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/safeDates"
        emitAll(
            db.collection("users").document(uid).collection("safeDates")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SafeDateRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFirestoreStreamError(OperationType.LIST, path, uid)
        )
    }

    fun observeMessageRequests(): Flow<List<MessageRequestRecord>> = flow {
        val uid = requireUserId()
        val path = "messageRequests"
        emitAll(
            db.collection("messageRequests")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MessageRequestRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFirestoreStreamError(OperationType.LIST, path, uid)
        )
    }

    fun observeConnections(): Flow<List<ConnectionRecord>> = flow {
        val uid = requireUserId()
        val path = "connections"
        emitAll(
            db.collection("connections")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ConnectionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFirestoreStreamError(OperationType.LIST, path, uid)
        )
    }

    fun observeMessages(connectionId: String): Flow<List<ChatMessageRecord>> = flow {
        val uid = requireUserId()
        val path = "connections/$connectionId/messages"
        emitAll(
            db.collection("connections").document(connectionId).collection("messages")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessageRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .handleFirestoreStreamError(OperationType.LIST, path, uid)
        )
    }

    fun observeMyReports(): Flow<List<SafetyReportRecord>> = flow {
        val uid = requireUserId()
        val path = "reports"
        emitAll(
            db.collection("reports")
                .whereEqualTo("reporterId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SafetyReportRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFirestoreStreamError(OperationType.LIST, path, uid)
        )
    }

    // --- Mutations ---

    suspend fun createOrUpdateAccountInitial(
        email: String,
        age: Int,
        dobYear: Int,
        dobMonth: Int,
        dobDay: Int,
        termsAccepted: Boolean,
        safetyConsent: Boolean,
        browseOnly: Boolean,
        phoneE164: String?
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            path = ref.path
            val existing = ref.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "email" to email.take(200),
                    "age" to age,
                    "dobYear" to dobYear,
                    "dobMonth" to dobMonth,
                    "dobDay" to dobDay,
                    "termsAccepted" to termsAccepted,
                    "safetyConsent" to safetyConsent,
                    "browseOnly" to browseOnly,
                    "selfieVerified" to false,
                    "phoneVerified" to !phoneE164.isNullOrBlank(),
                    "phoneE164" to phoneE164?.take(24),
                    "country" to "KE",
                    "locale" to "en",
                    "whoCanMessageFirst" to "verified_inbox",
                    "allowRoses" to true,
                    "standoutsMuted" to false,
                    "liteMode" to false,
                    "rosesBalance" to 1,
                    "rosesSentWeek" to 0,
                    "strikes" to 0,
                    "suspensionUntilMs" to 0L,
                    "onboardingComplete" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                ref.set(payload).await()
            } else {
                val updateMap = mapOf(
                    "safetyConsent" to safetyConsent,
                    "browseOnly" to browseOnly,
                    "phoneVerified" to !phoneE164.isNullOrBlank(),
                    "phoneE164" to phoneE164?.take(24),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                ref.update(updateMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserFields(fields: Map<String, Any>): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            path = ref.path
            val payload = fields.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            ref.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun savePublicProfile(profile: PublicProfile): Result<Unit> {
        var path = "publicProfiles"
        return try {
            val uid = requireUserId()
            val ref = db.collection("publicProfiles").document(uid)
            path = ref.path
            val existing = ref.get().await()
            val payload = mapOf(
                "userId" to uid,
                "firstName" to profile.firstName.trim().take(30),
                "age" to profile.age.coerceIn(18, 120),
                "gender" to profile.gender,
                "interestedIn" to profile.interestedIn.take(3),
                "area" to profile.area.take(60),
                "lat" to profile.lat,
                "lng" to profile.lng,
                "lookingFor" to profile.lookingFor,
                "bio" to profile.bio.take(300),
                "promptQuestion" to profile.promptQuestion.take(120),
                "promptAnswer" to profile.promptAnswer.take(200),
                "interests" to profile.interests.take(12),
                "languages" to profile.languages.take(6),
                "work" to profile.work.take(60),
                "selfieVerified" to profile.selfieVerified,
                "trustScore" to profile.trustScore.coerceIn(0, 100),
                "prefAgeMin" to profile.prefAgeMin.coerceIn(18, 99),
                "prefAgeMax" to profile.prefAgeMax.coerceIn(profile.prefAgeMin.coerceIn(18, 99), 99),
                "distMaxKm" to profile.distMaxKm.coerceIn(1, 500),
                "verifiedOnly" to profile.verifiedOnly,
                "showInNearby" to profile.showInNearby,
                "paused" to profile.paused,
                "incognito" to profile.incognito,
                "status" to profile.status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!existing.exists()) {
                ref.set(payload + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                ref.update(payload.filterKeys { it != "userId" && it != "age" }).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun savePrivatePrefs(prefs: PrivatePrefs): Result<Unit> {
        var path = "users/privatePrefs"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("privatePrefs").document("compatibility")
            path = ref.path
            val payload = mapOf(
                "userId" to uid,
                "consented" to prefs.consented,
                "consentVersion" to prefs.consentVersion,
                "pace" to prefs.pace,
                "paceDealbreaker" to prefs.paceDealbreaker,
                "exclusivity" to prefs.exclusivity,
                "exclusivityDealbreaker" to prefs.exclusivityDealbreaker,
                "chemWeight" to prefs.chemWeight,
                "discretion" to prefs.discretion,
                "flirtLevel" to prefs.flirtLevel,
                "place" to prefs.place,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePrivatePrefs(): Result<Unit> {
        var path = "users/privatePrefs"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("privatePrefs").document("compatibility")
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addPin(label: String, areaName: String, lat: Double, lng: Double, radiusM: Int): Result<Unit> {
        var path = "users/pins"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("pins").document()
            path = ref.path
            val payload = mapOf(
                "userId" to uid,
                "label" to label.trim().take(60),
                "areaName" to areaName.trim().take(80),
                "lat" to lat,
                "lng" to lng,
                "radiusM" to radiusM,
                "active" to true,
                "pausedUntilMs" to 0L,
                "createdAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun pauseAllPins(pins: List<ProtectedPin>, hours: Long): Result<Unit> {
        var path = "users/pins"
        return try {
            val uid = requireUserId()
            val pausedUntil = if (hours <= 0L) 0L else System.currentTimeMillis() + hours * 3_600_000L
            for (pin in pins) {
                val ref = db.collection("users").document(uid).collection("pins").document(pin.id)
                path = ref.path
                ref.update("pausedUntilMs", pausedUntil).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePin(pinId: String): Result<Unit> {
        var path = "users/pins"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("pins").document(pinId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun recordSwipe(
        targetId: String,
        targetName: String,
        targetArea: String,
        action: String,
        note: String?,
        roseAttached: Boolean
    ): Result<Unit> {
        var path = "users/swipes"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("swipes").document(targetId)
            path = ref.path
            val ladder = buildList {
                if (!note.isNullOrBlank()) add("note")
                if (roseAttached) add("rose")
            }
            val payload = mapOf(
                "actorId" to uid,
                "targetId" to targetId,
                "targetName" to targetName.take(60),
                "targetArea" to targetArea.take(80),
                "action" to action,
                "note" to note?.take(200),
                "roseAttached" to roseAttached,
                "reintroduced" to false,
                "ladderUsed" to ladder,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSwipeLadder(
        targetId: String,
        note: String? = null,
        roseAttached: Boolean? = null,
        reintroduced: Boolean? = null,
        ladderUsed: List<String>
    ): Result<Unit> {
        var path = "users/swipes"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("swipes").document(targetId)
            path = ref.path
            val map = mutableMapOf<String, Any>("ladderUsed" to ladderUsed.take(4))
            if (note != null) {
                map["note"] = note.take(200)
                map["action"] = "like_note"
            }
            if (roseAttached != null) map["roseAttached"] = roseAttached
            if (reintroduced != null) map["reintroduced"] = reintroduced
            ref.update(map).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createConnection(
        partnerId: String,
        partnerName: String,
        partnerArea: String,
        partnerVerified: Boolean,
        myName: String,
        initialPreview: String
    ): Result<String> {
        var path = "connections"
        return try {
            val uid = requireUserId()
            val connId = "${uid}_$partnerId".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
            val ref = db.collection("connections").document(connId)
            path = ref.path
            val existing = ref.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "memberUids" to listOf(uid, partnerId),
                    "userAId" to uid,
                    "userAName" to myName.ifBlank { "You" }.take(60),
                    "userBId" to partnerId,
                    "userBName" to partnerName.take(60),
                    "userBArea" to partnerArea.take(60),
                    "userBVerified" to partnerVerified,
                    "lastMessagePreview" to initialPreview.take(200),
                    "nudgeCount" to 0,
                    "nudgeBanner" to "",
                    "status" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(connId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        connection: ConnectionRecord,
        body: String,
        status: String,
        reasonCode: String?,
        nudgeCategory: String?,
        nudgeBannerToSet: String?,
        roseAttached: Boolean
    ): Result<Unit> {
        var path = "connections/${connection.id}/messages"
        return try {
            val uid = requireUserId()
            val msgRef = db.collection("connections").document(connection.id).collection("messages").document()
            path = msgRef.path
            val payload = mapOf(
                "connectionId" to connection.id,
                "memberUids" to connection.memberUids,
                "senderId" to uid,
                "body" to body.take(1000),
                "status" to status,
                "reasonCode" to reasonCode?.take(60),
                "nudgeCategory" to nudgeCategory?.take(60),
                "roseAttached" to roseAttached,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            msgRef.set(payload).await()

            val connRef = db.collection("connections").document(connection.id)
            val newPreview = when (status) {
                "blocked" -> "Blocked by Dove safety filter"
                "sensitive" -> "Sensitive message (blurred)"
                else -> body.take(200)
            }
            val nextNudgeCount = if (!nudgeBannerToSet.isNullOrBlank() && connection.nudgeCount < 2) {
                connection.nudgeCount + 1
            } else {
                connection.nudgeCount
            }
            val connUpdate = mutableMapOf<String, Any>(
                "lastMessagePreview" to newPreview,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!nudgeBannerToSet.isNullOrBlank() && connection.nudgeCount < 2) {
                connUpdate["nudgeCount"] = nextNudgeCount
                connUpdate["nudgeBanner"] = nudgeBannerToSet.take(200)
            }
            connRef.update(connUpdate).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun dismissConnectionNudge(connectionId: String): Result<Unit> {
        var path = "connections/$connectionId"
        return try {
            requireUserId()
            val ref = db.collection("connections").document(connectionId)
            path = ref.path
            ref.update(mapOf("nudgeBanner" to "", "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessageRequest(
        senderName: String,
        senderArea: String,
        senderVerified: Boolean,
        receiverId: String,
        receiverName: String,
        body: String,
        status: String,
        roseAttached: Boolean,
        sensitive: Boolean,
        effortReasons: List<String>,
        riskCategory: String?
    ): Result<Unit> {
        var path = "messageRequests"
        return try {
            val uid = requireUserId()
            val reqId = "${uid}_$receiverId".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
            val ref = db.collection("messageRequests").document(reqId)
            path = ref.path
            val payload = mapOf(
                "senderId" to uid,
                "senderName" to senderName.ifBlank { "You" }.take(60),
                "senderArea" to senderArea.take(60),
                "senderVerified" to senderVerified,
                "receiverId" to receiverId,
                "receiverName" to receiverName.take(60),
                "memberUids" to listOf(uid, receiverId),
                "body" to body.take(200),
                "status" to status,
                "roseAttached" to roseAttached,
                "sensitive" to sensitive,
                "effortReasons" to effortReasons.take(5),
                "riskCategory" to riskCategory?.take(40),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun seedSampleIncomingRequestsIfEmpty(myName: String): Result<Unit> {
        var path = "messageRequests"
        return try {
            val uid = requireUserId()
            val existing = db.collection("messageRequests")
                .whereArrayContains("memberUids", uid)
                .get()
                .await()
            if (!existing.isEmpty) return Result.success(Unit)

            val samples = listOf(
                mapOf(
                    "id" to "seed_req_david_$uid".take(120),
                    "senderId" to "companion_david",
                    "senderName" to "David",
                    "senderArea" to "Westlands, Nairobi",
                    "senderVerified" to true,
                    "receiverId" to uid,
                    "receiverName" to myName.ifBlank { "You" }.take(60),
                    "memberUids" to listOf("companion_david", uid),
                    "body" to "Loved your hiking note! Which Ngong Hills trail is your favorite?",
                    "status" to "pending",
                    "roseAttached" to true,
                    "sensitive" to false,
                    "effortReasons" to listOf("verified", "note_on_prompt", "rose_sent"),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "id" to "seed_req_aisha_$uid".take(120),
                    "senderId" to "companion_aisha",
                    "senderName" to "Aisha",
                    "senderArea" to "Kileleshwa, Nairobi",
                    "senderVerified" to false,
                    "receiverId" to uid,
                    "receiverName" to myName.ifBlank { "You" }.take(60),
                    "memberUids" to listOf("companion_aisha", uid),
                    "body" to "Hey! Fellow newcomer to Nairobi coffee spots — would love to connect.",
                    "status" to "filtered",
                    "roseAttached" to false,
                    "sensitive" to false,
                    "effortReasons" to listOf("note_on_prompt"),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            for (item in samples) {
                val docId = item["id"] as String
                val ref = db.collection("messageRequests").document(docId)
                path = ref.path
                ref.set(item.filterKeys { it != "id" }).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun acceptRequestAndOpenThread(
        request: MessageRequestRecord,
        replyBody: String,
        myName: String
    ): Result<String> {
        var path = "messageRequests/${request.id}"
        return try {
            val uid = requireUserId()
            db.collection("messageRequests").document(request.id)
                .update(mapOf("status" to "accepted", "updatedAt" to FieldValue.serverTimestamp()))
                .await()

            val connRes = createConnection(
                partnerId = request.senderId,
                partnerName = request.senderName,
                partnerArea = request.senderArea,
                partnerVerified = request.senderVerified,
                myName = myName,
                initialPreview = replyBody
            )
            val connId = connRes.getOrThrow()
            path = "connections/$connId/messages"

            // Write request as first message and reply as second
            val members = listOf(uid, request.senderId)
            val msg1 = mapOf(
                "connectionId" to connId,
                "memberUids" to members,
                "senderId" to request.senderId,
                "body" to request.body.take(1000),
                "status" to if (request.sensitive) "sensitive" else "sent",
                "roseAttached" to request.roseAttached,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("connections").document(connId).collection("messages").document().set(msg1).await()

            val msg2 = mapOf(
                "connectionId" to connId,
                "memberUids" to members,
                "senderId" to uid,
                "body" to replyBody.take(1000),
                "status" to "sent",
                "roseAttached" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("connections").document(connId).collection("messages").document().set(msg2).await()
            Result.success(connId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun ignoreMessageRequest(requestId: String): Result<Unit> {
        var path = "messageRequests/$requestId"
        return try {
            requireUserId()
            val ref = db.collection("messageRequests").document(requestId)
            path = ref.path
            ref.update(mapOf("status" to "ignored", "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun blockAndPreserveEvidence(
        targetUid: String,
        targetName: String,
        reasonCategory: String?,
        evidenceSnapshot: String?,
        connectionId: String?
    ): Result<Unit> {
        var path = "users/blocks"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("blocks").document(targetUid)
            path = ref.path
            val retainUntil = System.currentTimeMillis() + 90L * 86_400_000L // 90 days (§D7.5.3)
            val payload = mapOf(
                "blockerId" to uid,
                "blockedUid" to targetUid,
                "blockedName" to targetName.take(60),
                "reasonCategory" to reasonCategory?.take(40),
                "evidenceSnapshot" to evidenceSnapshot?.take(1000),
                "evidenceRetainedUntilMs" to retainUntil,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()

            if (!connectionId.isNullOrBlank()) {
                db.collection("connections").document(connectionId)
                    .update(mapOf("status" to "blocked", "updatedAt" to FieldValue.serverTimestamp()))
                    .await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun fileReport(
        targetUid: String,
        targetName: String,
        category: String,
        details: String,
        filedAfterBlock: Boolean,
        bulk: Boolean,
        evidenceSnippet: String?
    ): Result<Unit> {
        var path = "reports"
        return try {
            val uid = requireUserId()
            val ref = db.collection("reports").document()
            path = ref.path
            val payload = mapOf(
                "reporterId" to uid,
                "targetUid" to targetUid,
                "targetName" to targetName.take(60),
                "category" to category,
                "details" to details.take(500),
                "filedAfterBlock" to filedAfterBlock,
                "bulk" to bulk,
                "evidenceSnippet" to evidenceSnippet?.take(500),
                "outcome" to "reviewing",
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveSafeDate(
        connectionId: String,
        partnerName: String,
        place: String,
        meetAtLabel: String,
        trustedContact: String
    ): Result<Unit> {
        var path = "users/safeDates"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("safeDates").document()
            path = ref.path
            val payload = mapOf(
                "userId" to uid,
                "connectionId" to connectionId,
                "partnerName" to partnerName.take(60),
                "place" to place.take(120),
                "meetAtLabel" to meetAtLabel.take(80),
                "trustedContact" to trustedContact.take(60),
                "status" to "scheduled",
                "createdAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSafeDateStatus(dateId: String, status: String): Result<Unit> {
        var path = "users/safeDates/$dateId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("safeDates").document(dateId)
            path = ref.path
            ref.update("status", status).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccountData(): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val userDoc = db.collection("users").document(uid)
            for (sub in listOf("privatePrefs", "pins", "swipes", "blocks", "safeDates")) {
                val snap = userDoc.collection(sub).get().await()
                for (d in snap.documents) d.reference.delete().await()
            }
            path = "publicProfiles/$uid"
            db.collection("publicProfiles").document(uid).delete().await()
            path = userDoc.path
            userDoc.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
