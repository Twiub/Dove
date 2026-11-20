package com.example

import com.example.data.PrivatePrefs
import com.example.data.ProtectedPin
import com.example.data.PublicProfile
import com.example.domain.CompatibilityEngine
import com.example.domain.SafetyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoveEnginesTest {

    @Test
    fun privateCompatibilityWeightsSumStrictlyToOne() {
        assertEquals(1.0, CompatibilityEngine.weightsSum(), 1e-6)
    }

    @Test
    fun privateCompatibilityDealbreakerReturnsNegativeAndRejectsCandidate() {
        val viewerPriv = PrivatePrefs(
            userId = "u1",
            consented = true,
            pace = "slow",
            paceDealbreaker = true,
            exclusivity = "exclusive",
            exclusivityDealbreaker = true
        )
        val mismatchPriv = PrivatePrefs(
            userId = "u2",
            consented = true,
            pace = "open",
            exclusivity = "non_exclusive"
        )
        assertEquals(-1.0, CompatibilityEngine.privateAlignment(viewerPriv, mismatchPriv)!!, 1e-6)

        val viewer = PublicProfile(
            userId = "u1",
            firstName = "Sam",
            age = 28,
            gender = "man",
            interestedIn = listOf("women"),
            lat = -1.2921,
            lng = 36.7856,
            prefAgeMin = 22,
            prefAgeMax = 35,
            distMaxKm = 25,
            verifiedOnly = false,
            selfieVerified = true
        )
        val cand = PublicProfile(
            userId = "u2",
            firstName = "Amara",
            age = 27,
            gender = "woman",
            interestedIn = listOf("men"),
            lat = -1.2915,
            lng = 36.7870,
            prefAgeMin = 22,
            prefAgeMax = 35,
            distMaxKm = 25,
            verifiedOnly = false,
            selfieVerified = true
        )
        assertFalse(
            CompatibilityEngine.passesBothWaysFilters(
                viewer = viewer,
                candidate = cand,
                viewerPins = emptyList(),
                blockedIds = emptySet(),
                swipedIds = emptySet(),
                viewerPrivate = viewerPriv,
                candidatePrivate = mismatchPriv
            )
        )
    }

    @Test
    fun privateCompatibilityRequiresAtLeastTwoSharedNonSkipKeys() {
        val a = PrivatePrefs(userId = "u1", consented = true, pace = "gradual")
        val b = PrivatePrefs(userId = "u2", consented = true, pace = "gradual")
        assertNull(CompatibilityEngine.privateAlignment(a, b))
    }

    @Test
    fun protectedPinHidesCandidateInsideRadiusAndRevealsWhenPaused() {
        val activePin = ProtectedPin(
            id = "p1",
            userId = "u1",
            label = "Office",
            lat = -1.2921,
            lng = 36.7856,
            radiusM = 2000,
            active = true,
            pausedUntilMs = 0L
        )
        assertTrue(CompatibilityEngine.isHiddenByPin(-1.2930, 36.7860, listOf(activePin), nowMs = 1000L))

        val pausedPin = activePin.copy(pausedUntilMs = 5000L)
        assertFalse(CompatibilityEngine.isHiddenByPin(-1.2930, 36.7860, listOf(pausedPin), nowMs = 1000L))
    }

    @Test
    fun distanceBandsNeverExposeSubKilometerResolution() {
        assertEquals("1–2 km", CompatibilityEngine.toDistanceBand(0.05))
        assertEquals("1–2 km", CompatibilityEngine.toDistanceBand(1.5))
        assertEquals("2–5 km", CompatibilityEngine.toDistanceBand(3.2))
        assertEquals("5–10 km", CompatibilityEngine.toDistanceBand(7.5))
        assertEquals("50+ km", CompatibilityEngine.toDistanceBand(80.0))
    }

    @Test
    fun safetyEngineDetectsKenyaAndSpacedPhonesAndHandles() {
        val rawPhone = SafetyEngine.scanMessage("Call me on 0712 345 678 later")
        assertEquals(SafetyEngine.ScanOutcome.BLOCK_CONTACT_LEAK, rawPhone.outcome)

        val wordPhone = SafetyEngine.scanMessage("zero seven one two three four five six seven eight")
        assertEquals(SafetyEngine.ScanOutcome.BLOCK_CONTACT_LEAK, wordPhone.outcome)

        val handle = SafetyEngine.scanMessage("Add me on @nairobi_babe or wa.me/254712345678")
        assertEquals(SafetyEngine.ScanOutcome.BLOCK_CONTACT_LEAK, handle.outcome)
    }

    @Test
    fun safetyEngineDetectsMoneyScamsAndMinorSignalsAndExplicitText() {
        val scam = SafetyEngine.scanMessage("Babe tuma nauli kidogo nimekwama")
        assertEquals(SafetyEngine.ScanOutcome.HOLD_SCAM, scam.outcome)

        val minor = SafetyEngine.scanMessage("Actually I'm 16 and in high school")
        assertEquals(SafetyEngine.ScanOutcome.ESCALATE_CHILD_SAFETY, minor.outcome)

        val sextortion = SafetyEngine.scanMessage("Send money or I leak your photos")
        assertEquals(SafetyEngine.ScanOutcome.REJECT_POLICY, sextortion.outcome)

        val explicit = SafetyEngine.scanMessage("Are you down for sex tonight?")
        assertEquals(SafetyEngine.ScanOutcome.SENSITIVE_BLUR, explicit.outcome)
        assertTrue(explicit.isSensitiveExplicit)
    }

    @Test
    fun publicBioModerationRejectsReservedWordsAndContactLeaks() {
        assertNotNull(SafetyEngine.validatePublicText("Dove official support account"))
        assertNotNull(SafetyEngine.validatePublicText("Reach me at +254712345678"))
        assertNull(SafetyEngine.validatePublicText("I love Sunday hikes in Karura and coffee in Kilimani."))
    }
}
