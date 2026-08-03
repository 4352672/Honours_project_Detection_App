package com.example.detectionapptester.detectionapp2.detectionapp.app


enum class RiskLevel { LOW, MEDIUM, HIGH, SPYWARE }

class RiskEngine {

    companion object {
        // Individual permission weights
        private const val W_CAMERA = 15
        private const val W_RECORD_AUDIO = 15
        private const val W_FINE_LOCATION = 20
        private const val W_READ_CONTACTS = 15
        private const val W_READ_SMS = 20
        private const val W_BOOT_COMPLETED = 20

        // Combination bonusses
        private const val COMBO_CAMERA_INTERNET = 30
        private const val COMBO_CAMERA_AUDIO_INTERNET = 55
        private const val COMBO_CONTACTS_SMS_INTERNET = 50
        private const val COMBO_LOCATION_INTERNET = 35

        // Impersonation penalty (%)
        private const val W_IMPERSONATOR = 40
    }

    fun score(result: AppScanResult): AppScanResult {
        //Strip namespace prefix so checks work against short permission names
        val perms = result.permissions.map { it.removePrefix("android.permission.") }.toSet()

        var score = 0
        val hasInternet = "INTERNET" in perms

        // Tracks which permissions have been consumed by a combination so their individual scores are not double-counted (Option B scoring).
        val usedInCombo = mutableSetOf<String>()

        //Step 1:Combinations (checked first, highest specificity first)

        //Three-way: CAMERA + RECORD_AUDIO + INTERNET
        if ("CAMERA" in perms && "RECORD_AUDIO" in perms && hasInternet) {
            score += COMBO_CAMERA_AUDIO_INTERNET
            usedInCombo += "CAMERA"
            usedInCombo += "RECORD_AUDIO"
        }

        //Two-way: CAMERA + INTERNET (only works if three-way did not already fire)
        if ("CAMERA" in perms && hasInternet && "CAMERA" !in usedInCombo) {
            score += COMBO_CAMERA_INTERNET
            usedInCombo += "CAMERA"
        }

        //Three-way: READ_CONTACTS + READ_SMS + INTERNET
        if ("READ_CONTACTS" in perms && "READ_SMS" in perms && hasInternet) {
            score += COMBO_CONTACTS_SMS_INTERNET
            usedInCombo += "READ_CONTACTS"
            usedInCombo += "READ_SMS"
        }

        //Two-way: ACCESS_FINE_LOCATION + INTERNET
        if ("ACCESS_FINE_LOCATION" in perms && hasInternet) {
            score += COMBO_LOCATION_INTERNET
            usedInCombo += "ACCESS_FINE_LOCATION"
        }

        // --- Step 2: Individual scores (only for permissions not consumed by a combo) ---
        if ("CAMERA" in perms && "CAMERA" !in usedInCombo)                     score += W_CAMERA
        if ("RECORD_AUDIO" in perms && "RECORD_AUDIO" !in usedInCombo)         score += W_RECORD_AUDIO
        if ("ACCESS_FINE_LOCATION" in perms && "ACCESS_FINE_LOCATION" !in usedInCombo) score += W_FINE_LOCATION
        if ("READ_CONTACTS" in perms && "READ_CONTACTS" !in usedInCombo)       score += W_READ_CONTACTS
        if ("READ_SMS" in perms && "READ_SMS" !in usedInCombo)                 score += W_READ_SMS
        if ("RECEIVE_BOOT_COMPLETED" in perms) score += W_BOOT_COMPLETED       // always individual

        // --- Step 3: Impersonation penalty ---
        if (result.isImpersonator) score += W_IMPERSONATOR

        // TODO: ThreatClassifier hook — plug ML classifier output in here
        // val mlBonus = ThreatClassifier.classify(result)
        // score += mlBonus

        // TODO: Adware detection hook — flag apps with excessive ad-network permissions
        // score += AdwareDetector.score(result)

        // TODO: Battery monitoring hook — add score for WAKE_LOCK + INTERNET (silent beaconing pattern)
        // if ("WAKE_LOCK" in perms && hasInternet) score += W_WAKE_BEACON

        // --- Step 4: Network behaviour scoring ---
        val networkStats = result.networkStats ?: NetworkStats.empty()
        val networkScore = NetworkScoringEngine().score(networkStats)
        score += networkScore

        // Cap combined permission + network score at 100%
        val probability = score.coerceIn(0, 100)

        val riskLevel = when {
            probability <= 30 -> RiskLevel.LOW
            probability <= 60 -> RiskLevel.MEDIUM
            probability <= 85 -> RiskLevel.HIGH
            else -> RiskLevel.SPYWARE
        }

        return result.copy(
            spywareProbability = probability,
            riskLevel = riskLevel,
            networkScoreAddition = networkScore
        )
    }
}
