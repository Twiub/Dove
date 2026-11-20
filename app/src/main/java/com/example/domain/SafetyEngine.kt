package com.example.domain

/**
 * Dove Contact-Leak, Romance-Scam (DS-01..DS-21), Harassment & Play-Policy Safety Engine.
 * Implements §D6.2, §D6.3, §D6.4, §D7.2, and §D9.6.
 * Never reveals internal rule IDs to end users; exposes category codes only (§D7.5.10).
 */
object SafetyEngine {

    enum class ScanOutcome {
        ALLOW,
        SENSITIVE_BLUR,
        BLOCK_CONTACT_LEAK,
        HOLD_SCAM,
        REJECT_POLICY,
        ESCALATE_CHILD_SAFETY
    }

    data class ScanResult(
        val outcome: ScanOutcome,
        val userCategory: String,
        val nudgeBanner: String? = null,
        val strikeIncrement: Int = 0,
        val isSensitiveExplicit: Boolean = false
    )

    private val LEET_MAP = mapOf(
        '0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's',
        '7' to 't', '@' to 'a', '$' to 's'
    )

    private val NUMBER_WORDS = mapOf(
        // English
        "zero" to "0", "oh" to "0", "one" to "1", "two" to "2", "three" to "3",
        "four" to "4", "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
        // Kiswahili
        "sifuri" to "0", "moja" to "1", "mbili" to "2", "tatu" to "3",
        "nne" to "4", "tano" to "5", "sita" to "6", "saba" to "7", "nane" to "8", "tisa" to "9"
    )

    fun normalizeForScan(raw: String): String {
        val noZeroWidth = raw.replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
        val lower = noZeroWidth.lowercase()
            .replace("[dot]", ".")
            .replace("(dot)", ".")
            .replace(" dot ", ".")
            .replace("[at]", "@")
            .replace("(at)", "@")
        return lower.replace(Regex("\\s+"), " ").trim()
    }

    fun foldNumberWordsAndDigits(norm: String): String {
        val tokens = norm.split(Regex("[\\s,.;:/\\-]+"))
        val converted = tokens.joinToString("") { token ->
            NUMBER_WORDS[token] ?: token
        }
        return converted.replace(Regex("[^a-z0-9+]"), "")
    }

    fun normalizeLeet(norm: String): String =
        norm.map { LEET_MAP[it] ?: it }.joinToString("")

    fun scanMessage(rawText: String, messageIndexInThread: Int = 1): ScanResult {
        val norm = normalizeForScan(rawText)
        val leet = normalizeLeet(norm)
        val foldedDigits = foldNumberWordsAndDigits(norm)

        // 1. DS-16 Minor / child safety signals (highest priority)
        if (Regex("\\b(i[' ]?m|i am|aged?)\\s*(1[0-7])\\b").containsMatchIn(norm) ||
            norm.contains("high schooler") || norm.contains("form 3") || norm.contains("form 4")
        ) {
            return ScanResult(
                outcome = ScanOutcome.ESCALATE_CHILD_SAFETY,
                userCategory = "Child safety policy violation",
                strikeIncrement = 5
            )
        }

        // 2. DS-08 Sextortion
        if (Regex("(pay or i (send|post|share)|i have your (pics|nudes|photos)|send money or i leak)")
                .containsMatchIn(norm) ||
            Regex("(pay or i (send|post|share)|i have your (pics|nudes|photos)|send money or i leak)")
                .containsMatchIn(leet)
        ) {
            return ScanResult(
                outcome = ScanOutcome.REJECT_POLICY,
                userCategory = "Threats or extortion",
                nudgeBanner = "Threats and extortion are strictly prohibited. Report and block immediately.",
                strikeIncrement = 5
            )
        }

        // 3. DS-09 / DS-10 Compensated arrangements / solicitation / escort (Play policy)
        if (Regex("\\b(sponsor|sugar daddy|sugar mummy|nakulipa|pay for company|allowance for meeting|full night|incall|outcall|escort|hourly rate)\\b")
                .containsMatchIn(norm) ||
            Regex("\\b(sponsor|sugar daddy|sugar mummy|nakulipa|pay for company|full night|incall|outcall|escort)\\b")
                .containsMatchIn(leet)
        ) {
            return ScanResult(
                outcome = ScanOutcome.REJECT_POLICY,
                userCategory = "Solicitation or compensated arrangement",
                strikeIncrement = 2
            )
        }

        // 4. DS-13 OTP / PIN harvest
        if (Regex("\\b(send (me )?(your )?(otp|pin|verification code)|mpesa pin|m-pesa pin)\\b")
                .containsMatchIn(norm)
        ) {
            return ScanResult(
                outcome = ScanOutcome.REJECT_POLICY,
                userCategory = "Account security risk (code/PIN request)",
                nudgeBanner = "Never share verification codes or PINs with anyone.",
                strikeIncrement = 3
            )
        }

        // 5. Contact-leak detection (§D6.2)
        val hasKenyaPhone = Regex("(?:\\+?254|0)[17]\\d{8}").containsMatchIn(foldedDigits)
        val hasIntlPhone = Regex("\\+?[1-9]\\d{9,14}").containsMatchIn(foldedDigits)
        val hasHandleOrUrl = Regex("(wa\\.me/|t\\.me/|https?://|hxxps?://|www\\.|@[a-z0-9_.]{3,}|[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,})")
            .containsMatchIn(norm)
        val hasOffPlatformPush = Regex("\\b(whatsapp|whatsap|watsap|telegram|tele|instagram|insta|ig|snapchat|snap|nipigie|text me on|add me on|dm me on|tuma whatsapp)\\b")
            .containsMatchIn(norm) &&
            Regex("\\b(me|my|on|at|number|no\\.?|handle|tuma|add|dm|call|nipigie|talk|chat|move|let'?s)\\b")
                .containsMatchIn(norm)
        val hasTillPaybill = Regex("\\b(till|paybill|pochi|send to my number)\\b").containsMatchIn(norm) &&
            Regex("\\d{4,}").containsMatchIn(foldedDigits)

        if (hasKenyaPhone || hasIntlPhone || hasHandleOrUrl || hasTillPaybill) {
            return ScanResult(
                outcome = ScanOutcome.BLOCK_CONTACT_LEAK,
                userCategory = "Contact details shared",
                nudgeBanner = "They tried to share contact details. Keep chatting in Dove for your safety.",
                strikeIncrement = 1
            )
        }
        if (hasOffPlatformPush) {
            val earlyWeight = if (messageIndexInThread < 6) 1 else 1
            return ScanResult(
                outcome = ScanOutcome.BLOCK_CONTACT_LEAK,
                userCategory = "Off-platform chat request",
                nudgeBanner = "They asked to move to another app. Keep chatting here: scammers often do this.",
                strikeIncrement = earlyWeight
            )
        }

        // 6. DS-01..DS-06 & DS-12 Romance scam / money request triggers
        val scamPattern = Regex(
            "\\b(send (me )?(airtime|bundles|data|vocha|fare|nauli|transport|money|cash|pesa|ksh|kes|usd|deposit)|" +
                "nitumie|tuma (nauli|fare|vocha|pesa|ksh)|" +
                "hospital bill|stuck at|customs fee|clearance fee|parcel fee|" +
                "invest with me|crypto signal|forex signal|double your money|" +
                "sent by mistake.*refund|reverse.*mpesa|m-?pesa reversal|" +
                "book the room.*deposit)\\b"
        )
        if (scamPattern.containsMatchIn(norm) || scamPattern.containsMatchIn(leet)) {
            return ScanResult(
                outcome = ScanOutcome.HOLD_SCAM,
                userCategory = "Money or payment request",
                nudgeBanner = "They asked for money or a transfer. Never send money to someone you haven't met.",
                strikeIncrement = 1
            )
        }

        // 7. Sensitive / explicit sexual text friction (§D6.4)
        if (Regex("\\b(nudes?|send pics|explicit|horny|sex tonight|sleep with me|wanna fuck)\\b")
                .containsMatchIn(norm)
        ) {
            return ScanResult(
                outcome = ScanOutcome.SENSITIVE_BLUR,
                userCategory = "Sensitive message",
                isSensitiveExplicit = true
            )
        }

        return ScanResult(
            outcome = ScanOutcome.ALLOW,
            userCategory = "Clean"
        )
    }

    /**
     * Public bio/prompt moderation (§D3.4)
     */
    fun validatePublicText(raw: String): String? {
        val norm = normalizeForScan(raw)
        if (Regex("\\b(support|admin|dove official|safaricom|mpesa)\\b").containsMatchIn(norm)) {
            return "Display text cannot use reserved official or brand names."
        }
        val scan = scanMessage(raw)
        return when (scan.outcome) {
            ScanOutcome.BLOCK_CONTACT_LEAK -> "Please remove phone numbers, handles, or links from your public profile."
            ScanOutcome.HOLD_SCAM, ScanOutcome.REJECT_POLICY, ScanOutcome.ESCALATE_CHILD_SAFETY ->
                "This text isn't allowed on public profiles (${scan.userCategory})."
            ScanOutcome.SENSITIVE_BLUR -> "Public profiles must stay clean and free of explicit wording."
            ScanOutcome.ALLOW -> null
        }
    }
}
