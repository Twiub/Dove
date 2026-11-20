package com.example.data

import com.example.R

object DoveSeedCatalog {

    val areas = listOf(
        AreaLocation("Kilimani, Nairobi", -1.2921, 36.7856),
        AreaLocation("Westlands, Nairobi", -1.2673, 36.8111),
        AreaLocation("Karen, Nairobi", -1.3210, 36.7120),
        AreaLocation("Lavington, Nairobi", -1.2797, 36.7704),
        AreaLocation("Kileleshwa, Nairobi", -1.2810, 36.7820),
        AreaLocation("CBD / Upper Hill", -1.2986, 36.8169),
        AreaLocation("Runda / Gigiri", -1.2248, 36.8091),
        AreaLocation("Ngong / Rongai", -1.3610, 36.6580),
        AreaLocation("Nyali, Mombasa", -4.0355, 39.7135),
        AreaLocation("London, UK", 51.5074, -0.1278),
        AreaLocation("New York, US", 40.7128, -74.0060)
    )

    val curatedPublicDateVenues = listOf(
        "Artcaffe — The Junction Mall (Public Café)",
        "Java House — Kilimani Argwings Kodhek (Public Café)",
        "Karura Forest — Limuru Rd Gate (Public Trail)",
        "Village Market — Courtyard Café (Public Mall)",
        "Nairobi Arboretum — Main Lawn (Public Park)",
        "Sarit Centre — Atrium Coffee Bar (Public Mall)"
    )

    val emergencyHelplinesByCountry = mapOf(
        "KE" to listOf("Kenya Police / Emergency: 999 or 112", "Gender Violence Helpline: 1195", "DCI Cybercrime Desk: +254 20 272 6011"),
        "GB" to listOf("UK Emergency: 999 or 112", "Action Fraud: 0300 123 2040"),
        "US" to listOf("US Emergency: 911", "FTC Scam Reporting: reportfraud.ftc.gov")
    )

    fun seedCompanions(): List<CompanionCandidate> = listOf(
        CompanionCandidate(
            profile = PublicProfile(
                userId = "companion_amara",
                firstName = "Amara",
                age = 27,
                gender = "woman",
                interestedIn = listOf("men", "women", "nonbinary"),
                area = "Kilimani, Nairobi",
                lat = -1.2915,
                lng = 36.7870,
                lookingFor = "long_term",
                bio = "Product designer who starts Saturdays with a Karura run, nyama choma with friends, and good Kenyan AA espresso.",
                promptQuestion = "A green flag I look for",
                promptAnswer = "Consistency, clear intentions, and someone who actually reads profiles.",
                interests = listOf("Hiking", "Coffee", "Design", "Nyama Choma", "Reading"),
                languages = listOf("en", "sw"),
                work = "Product Designer",
                selfieVerified = true,
                trustScore = 94,
                prefAgeMin = 21,
                prefAgeMax = 45,
                distMaxKm = 50,
                verifiedOnly = false,
                showInNearby = true,
                status = "active"
            ),
            photoRes = R.drawable.img_profile_amara,
            daysSinceActive = 0.2,
            effortTowardUser = 0.85,
            privateAlignmentStub = PrivatePrefs(
                userId = "companion_amara",
                consented = true,
                pace = "gradual",
                exclusivity = "exclusive",
                chemWeight = "medium",
                discretion = "selective",
                flirtLevel = "sweet",
                place = "own_place"
            ),
            likesUserBack = true,
            completedDates = 3,
            repliesTypical = "within 2 hours"
        ),
        CompanionCandidate(
            profile = PublicProfile(
                userId = "companion_david",
                firstName = "David",
                age = 29,
                gender = "man",
                interestedIn = listOf("women", "men", "nonbinary"),
                area = "Westlands, Nairobi",
                lat = -1.2673,
                lng = 36.8111,
                lookingFor = "long_term_open_to_short",
                bio = "Software engineer & weekend trail runner. Usually up Ngong Hills or hunting down the best chapati in town.",
                promptQuestion = "Two truths and a vibe",
                promptAnswer = "Climbed Mt. Longonot twice, cooks a mean pilau, and prefers deep talks over small talk.",
                interests = listOf("Hiking", "Software", "Cooking", "Running", "Jazz"),
                languages = listOf("en", "sw", "sheng"),
                work = "Android Engineer",
                selfieVerified = true,
                trustScore = 91,
                prefAgeMin = 21,
                prefAgeMax = 45,
                distMaxKm = 50,
                verifiedOnly = false,
                showInNearby = true,
                status = "active"
            ),
            photoRes = R.drawable.img_profile_david,
            daysSinceActive = 0.5,
            effortTowardUser = 0.90,
            privateAlignmentStub = PrivatePrefs(
                userId = "companion_david",
                consented = true,
                pace = "natural",
                exclusivity = "exclusive",
                chemWeight = "high",
                discretion = "public",
                flirtLevel = "flirty",
                place = "own_place"
            ),
            likesUserBack = true,
            completedDates = 4,
            repliesTypical = "within a few hours"
        ),
        CompanionCandidate(
            profile = PublicProfile(
                userId = "companion_zuri",
                firstName = "Zuri",
                age = 26,
                gender = "woman",
                interestedIn = listOf("men", "women", "nonbinary"),
                area = "Karen, Nairobi",
                lat = -1.3210,
                lng = 36.7120,
                lookingFor = "long_term",
                bio = "Ceramicist & botanical illustrator. Weekend farmers markets, coastal road trips, and quiet Sunday mornings.",
                promptQuestion = "My ideal first meet",
                promptAnswer = "Daytime coffee at a sunny courtyard café where we can talk properly.",
                interests = listOf("Art", "Pottery", "Travel", "Coffee", "Photography"),
                languages = listOf("en", "sw"),
                work = "Creative Studio Lead",
                selfieVerified = true,
                trustScore = 89,
                prefAgeMin = 22,
                prefAgeMax = 45,
                distMaxKm = 50,
                verifiedOnly = false,
                showInNearby = true,
                status = "active"
            ),
            photoRes = R.drawable.img_profile_zuri,
            daysSinceActive = 1.1,
            effortTowardUser = 0.65,
            privateAlignmentStub = PrivatePrefs(
                userId = "companion_zuri",
                consented = true,
                pace = "slow",
                exclusivity = "exclusive",
                chemWeight = "medium",
                discretion = "selective",
                flirtLevel = "sweet",
                place = "shared_place"
            ),
            likesUserBack = false,
            completedDates = 2,
            repliesTypical = "within a day"
        ),
        CompanionCandidate(
            profile = PublicProfile(
                userId = "companion_kamau",
                firstName = "Kamau",
                age = 31,
                gender = "man",
                interestedIn = listOf("women", "men", "nonbinary"),
                area = "Lavington, Nairobi",
                lat = -1.2797,
                lng = 36.7704,
                lookingFor = "short_term_open_to_long",
                bio = "Architect who loves cycling, board games, and trying new Ethiopian restaurants around Kilimani.",
                promptQuestion = "What I bring to the table",
                promptAnswer = "Zero games, great playlists, and honest communication from day one.",
                interests = listOf("Cycling", "Architecture", "Board Games", "Food", "Music"),
                languages = listOf("en", "sw"),
                work = "Architect",
                selfieVerified = true,
                trustScore = 86,
                prefAgeMin = 21,
                prefAgeMax = 45,
                distMaxKm = 50,
                verifiedOnly = false,
                showInNearby = true,
                status = "active"
            ),
            photoRes = R.drawable.img_profile_david,
            daysSinceActive = 1.8,
            effortTowardUser = 0.3,
            privateAlignmentStub = PrivatePrefs(
                userId = "companion_kamau",
                consented = true,
                pace = "natural",
                exclusivity = "open_to_discuss",
                chemWeight = "high",
                discretion = "selective",
                flirtLevel = "flirty",
                place = "own_place"
            ),
            likesUserBack = true,
            completedDates = 2,
            repliesTypical = "same day"
        ),
        CompanionCandidate(
            profile = PublicProfile(
                userId = "companion_aisha",
                firstName = "Aisha",
                age = 25,
                gender = "woman",
                interestedIn = listOf("men", "women", "nonbinary"),
                area = "Kileleshwa, Nairobi",
                lat = -1.2810,
                lng = 36.7820,
                lookingFor = "new_friends",
                bio = "Just moved to Nairobi for public health research. Looking to meet warm, grounded people for coffee and hikes.",
                promptQuestion = "Ask me about",
                promptAnswer = "My obsession with Swahili coastal food and learning acoustic guitar.",
                interests = listOf("Hiking", "Guitar", "Cooking", "Reading", "Health"),
                languages = listOf("en", "sw"),
                work = "Health Researcher",
                selfieVerified = false,
                trustScore = 62,
                prefAgeMin = 20,
                prefAgeMax = 42,
                distMaxKm = 50,
                verifiedOnly = false,
                showInNearby = true,
                status = "active"
            ),
            photoRes = R.drawable.img_profile_amara,
            daysSinceActive = 0.8,
            effortTowardUser = 0.2,
            privateAlignmentStub = null,
            likesUserBack = false,
            completedDates = 0,
            repliesTypical = "within a day"
        )
    )
}
