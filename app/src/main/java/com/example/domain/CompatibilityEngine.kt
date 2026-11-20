package com.example.domain

import com.example.data.CompanionCandidate
import com.example.data.PrivatePrefs
import com.example.data.ProtectedPin
import com.example.data.PublicProfile
import com.example.data.ScoredCandidate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure matching, protected-pin exclusion, distance-band privacy, and private compatibility engine.
 * Implements Dove Spec §D4.3, §D5.1, §D5.3, §D5.6, and §D5.7.
 */
object CompatibilityEngine {

    // §D4.3 weights sum strictly to 1.0
    private val PRIVATE_WEIGHTS = mapOf(
        "pace" to 0.28,
        "exclusivity" to 0.28,
        "chem_weight" to 0.14,
        "discretion" to 0.10,
        "flirt_level" to 0.15,
        "place" to 0.05
    )

    private val PACE_ORDER = listOf("slow", "gradual", "natural", "open")
    private val CHEM_ORDER = listOf("low", "medium", "high")
    private val DISCRETION_ORDER = listOf("public", "selective", "private")
    private val FLIRT_ORDER = listOf("sweet", "flirty", "bold")

    fun weightsSum(): Double = PRIVATE_WEIGHTS.values.sum()

    fun snapTo500mGrid(lat: Double, lng: Double): Pair<Double, Double> {
        val gridDeg = 0.0045 // ~500 m
        val snappedLat = (lat / gridDeg).roundToInt() * gridDeg
        val snappedLng = (lng / gridDeg).roundToInt() * gridDeg
        return snappedLat to snappedLng
    }

    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * §D5.6 / §D5.7 Anti-triangulation distance bands. Never returns < 1 km.
     */
    fun toDistanceBand(km: Double): String = when {
        km < 2.0 -> "1–2 km"
        km < 5.0 -> "2–5 km"
        km < 10.0 -> "5–10 km"
        km < 25.0 -> "10–25 km"
        km < 50.0 -> "25–50 km"
        else -> "50+ km"
    }

    /**
     * §D5.6 Fixed protected-place pins: checks if candidate's area falls inside any active unpaused pin.
     */
    fun isHiddenByPin(
        candidateLat: Double,
        candidateLng: Double,
        pins: List<ProtectedPin>,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        return pins.any { pin ->
            val isUnpaused = pin.pausedUntilMs <= nowMs
            if (!pin.active || !isUnpaused) return@any false
            val distM = haversineKm(pin.lat, pin.lng, candidateLat, candidateLng) * 1000.0
            distM <= pin.radiusM
        }
    }

    /**
     * §D4.3 Pure private compatibility calculation.
     * Returns null if fewer than 2 shared non-skip keys, or -1.0 if a dealbreaker mismatch exists.
     */
    fun privateAlignment(a: PrivatePrefs?, b: PrivatePrefs?): Double? {
        if (a == null || b == null) return null

        // Dealbreaker checks (§D4.2: exclusivity and pace only)
        if (a.exclusivity != "skip" && b.exclusivity != "skip") {
            if ((a.exclusivityDealbreaker || b.exclusivityDealbreaker) && a.exclusivity != b.exclusivity) {
                if (!(a.exclusivity == "open_to_discuss" && b.exclusivity == "open_to_discuss")) {
                    return -1.0
                }
            }
        }
        if (a.pace != "skip" && b.pace != "skip") {
            if (a.paceDealbreaker || b.paceDealbreaker) {
                val idxA = PACE_ORDER.indexOf(a.pace)
                val idxB = PACE_ORDER.indexOf(b.pace)
                if (idxA >= 0 && idxB >= 0 && kotlin.math.abs(idxA - idxB) > 1) {
                    return -1.0
                }
            }
        }

        val sims = mutableMapOf<String, Double>()
        orderedSim(a.pace, b.pace, PACE_ORDER)?.let { sims["pace"] = it }
        exclusivitySim(a.exclusivity, b.exclusivity)?.let { sims["exclusivity"] = it }
        orderedSim(a.chemWeight, b.chemWeight, CHEM_ORDER)?.let { sims["chem_weight"] = it }
        orderedSim(a.discretion, b.discretion, DISCRETION_ORDER)?.let { sims["discretion"] = it }
        orderedSim(a.flirtLevel, b.flirtLevel, FLIRT_ORDER)?.let { sims["flirt_level"] = it }
        placeSim(a.place, b.place)?.let { sims["place"] = it }

        if (sims.size < 2) return null

        var num = 0.0
        var den = 0.0
        for ((k, sim) in sims) {
            val w = PRIVATE_WEIGHTS[k] ?: 0.0
            num += w * sim
            den += w
        }
        return if (den > 0.0) (num / den).coerceIn(0.0, 1.0) else null
    }

    private fun orderedSim(a: String, b: String, scale: List<String>): Double? {
        if (a == "skip" || b == "skip") return null
        val ia = scale.indexOf(a)
        val ib = scale.indexOf(b)
        if (ia < 0 || ib < 0) return null
        val maxDist = (scale.size - 1).coerceAtLeast(1)
        val diff = kotlin.math.abs(ia - ib).toDouble() / maxDist
        return (1.0 - diff * 0.9).coerceIn(0.1, 1.0)
    }

    private fun exclusivitySim(a: String, b: String): Double? {
        if (a == "skip" || b == "skip") return null
        if (a == b) return 1.0
        if (a == "open_to_discuss" || b == "open_to_discuss") return 0.5
        return 0.0
    }

    private fun placeSim(a: String, b: String): Double? {
        if (a == "skip" || b == "skip") return null
        if (a == "own_place" || b == "own_place") return 1.0
        if (a == "shared_place" || b == "shared_place") return 0.65
        return 0.4
    }

    /**
     * §D5.1 Both-ways hard filters.
     */
    fun passesBothWaysFilters(
        viewer: PublicProfile,
        candidate: PublicProfile,
        viewerPins: List<ProtectedPin>,
        blockedIds: Set<String>,
        swipedIds: Set<String>,
        viewerPrivate: PrivatePrefs?,
        candidatePrivate: PrivatePrefs?,
        forNearby: Boolean = false
    ): Boolean {
        if (candidate.userId == viewer.userId) return false
        if (candidate.status != "active" || candidate.paused) return false
        if (candidate.userId in blockedIds) return false
        if (!forNearby && candidate.userId in swipedIds) return false
        if (forNearby && !candidate.showInNearby) return false

        // Verified-only both ways (§D5.1)
        if (viewer.verifiedOnly && !candidate.selfieVerified) return false
        if (candidate.verifiedOnly && !viewer.selfieVerified) return false

        // Age both ways
        if (candidate.age !in viewer.prefAgeMin..viewer.prefAgeMax) return false
        if (viewer.age !in candidate.prefAgeMin..candidate.prefAgeMax) return false

        // Gender <-> interestedIn both ways
        val viewerLikesCandidateGender = viewer.interestedIn.isEmpty() ||
            genderMatchesInterest(candidate.gender, viewer.interestedIn)
        val candidateLikesViewerGender = candidate.interestedIn.isEmpty() ||
            genderMatchesInterest(viewer.gender, candidate.interestedIn)
        if (!viewerLikesCandidateGender || !candidateLikesViewerGender) return false

        // Max distance both ways
        val km = haversineKm(viewer.lat, viewer.lng, candidate.lat, candidate.lng)
        val mutualMaxKm = minOf(viewer.distMaxKm, candidate.distMaxKm).toDouble()
        if (km > mutualMaxKm) return false

        // Protected-place pins
        if (isHiddenByPin(candidate.lat, candidate.lng, viewerPins)) return false

        // Private dealbreakers (§D4.2)
        val privScore = privateAlignment(viewerPrivate, candidatePrivate)
        if (privScore != null && privScore < 0.0) return false

        return true
    }

    private fun genderMatchesInterest(gender: String, interestedIn: List<String>): Boolean {
        return when (gender) {
            "woman" -> "women" in interestedIn || "woman" in interestedIn
            "man" -> "men" in interestedIn || "man" in interestedIn
            else -> "nonbinary" in interestedIn || interestedIn.size >= 2
        }
    }

    /**
     * §D5.3 Mutual fit scoring and 4 card meters.
     */
    fun scoreCandidate(
        viewer: PublicProfile,
        viewerPrivate: PrivatePrefs?,
        candidateWrap: CompanionCandidate
    ): ScoredCandidate {
        val c = candidateWrap.profile
        val km = haversineKm(viewer.lat, viewer.lng, c.lat, c.lng)
        val mutualMaxKm = minOf(viewer.distMaxKm, c.distMaxKm).coerceAtLeast(1).toDouble()

        val sharedInterests = viewer.interests.map { it.lowercase() }
            .intersect(c.interests.map { it.lowercase() }.toSet()).size
        val unionInterests = (viewer.interests + c.interests).map { it.lowercase() }.toSet().size.coerceAtLeast(1)
        val sem = (0.35 + (sharedInterests.toDouble() / unionInterests) * 0.65).coerceIn(0.0, 1.0)

        val goal = goalCompatibility(viewer.lookingFor, c.lookingFor)
        val priv = privateAlignment(viewerPrivate, candidateWrap.privateAlignmentStub)?.takeIf { it >= 0.0 }
        val prox = (1.0 - km / mutualMaxKm).coerceIn(0.0, 1.0)
        val langOverlap = viewer.languages.intersect(c.languages.toSet()).isNotEmpty()
        val lifestyle = (0.45 + (if (langOverlap) 0.35 else 0.0) + (sharedInterests.coerceAtMost(3) * 0.06)).coerceIn(0.0, 1.0)
        val trust = (c.trustScore / 100.0).coerceIn(0.0, 1.0)
        val activity = 0.5.pow(candidateWrap.daysSinceActive / 3.0).coerceIn(0.0, 1.0)
        val hist = 0.65 // calibrated baseline

        val sAb = weightedCombine(sem, goal, priv, prox, lifestyle, trust, activity, hist)
        val sBa = weightedCombine(sem, goal, priv, prox, lifestyle, (viewer.trustScore / 100.0).coerceIn(0.0, 1.0), 0.9, hist)
        val pair = sqrt(sAb * sBa)
        val shownToViewer = (pair * (1.0 + 0.10 * candidateWrap.effortTowardUser.coerceIn(0.0, 1.0)))
            .roundToInt()
            .coerceIn(45, 97)

        val privateSummary = when {
            priv == null -> null
            priv >= 0.72 -> "You're on a similar page"
            else -> "You may want different things"
        }

        return ScoredCandidate(
            candidate = candidateWrap,
            matchPercent = shownToViewer,
            interestsScore = (sem * 100).roundToInt(),
            distanceScore = (prox * 100).roundToInt(),
            activeScore = (activity * 100).roundToInt(),
            verifiedScore = c.trustScore,
            distanceBand = toDistanceBand(km),
            privateAlignmentSummary = privateSummary
        )
    }

    private fun weightedCombine(
        sem: Double,
        goal: Double,
        priv: Double?,
        prox: Double,
        lifestyle: Double,
        trust: Double,
        activity: Double,
        hist: Double
    ): Double {
        val base = 0.26 * sem + 0.22 * goal + 0.12 * prox + 0.08 * lifestyle + 0.08 * trust + 0.06 * activity + 0.04 * hist
        val raw = if (priv != null) {
            base + 0.14 * priv
        } else {
            // Drop private term and renormalise by 1 / (1 - 0.14)
            base / 0.86
        }
        return 100.0 * raw.coerceIn(0.0, 0.97)
    }

    private fun goalCompatibility(a: String, b: String): Double {
        if (a == b) return 1.0
        val pair = setOf(a, b)
        return when {
            pair == setOf("long_term", "long_term_open_to_short") -> 0.82
            pair == setOf("short_term_open_to_long", "long_term_open_to_short") -> 0.78
            pair == setOf("short_term_fun", "short_term_open_to_long") -> 0.75
            "long_term" in pair && "short_term_fun" in pair -> 0.15
            "still_figuring_out" in pair -> 0.55
            "new_friends" in pair -> 0.45
            else -> 0.50
        }
    }
}
