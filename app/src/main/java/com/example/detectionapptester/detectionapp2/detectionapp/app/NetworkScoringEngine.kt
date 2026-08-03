package com.example.detectionapptester.detectionapp2.detectionapp.app

class NetworkScoringEngine {
    companion object{
        private const val SCORE_BACKGROUND_ACTIVITY=20 //Silent background data

        private const val SCORE_UPLOAD_HEAVY=25 //More sent that received

        private const val SCORE_IDLE_SPIKES=20 // burst during low-activity

        private const val SCORE_CONTINUOUS_TRANSMISSION =15 // Constant data stream

    }
    fun score(stats: NetworkStats): Int {
        var additionalScore = 0

        if (stats.hasBackgroundActivity) additionalScore += SCORE_BACKGROUND_ACTIVITY
        if (stats.isUploadHeavy) additionalScore += SCORE_UPLOAD_HEAVY
        if (stats.hasIdleSpikes) additionalScore += SCORE_IDLE_SPIKES
        if (stats.hasContinuousTransmission) additionalScore += SCORE_CONTINUOUS_TRANSMISSION

        return additionalScore

    }
}