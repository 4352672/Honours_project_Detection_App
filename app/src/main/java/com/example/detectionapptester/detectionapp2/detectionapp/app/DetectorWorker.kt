package com.example.detectionapptester.detectionapp2.detectionapp.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DetectorWorker (
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context,params){

    override suspend fun doWork(): Result {
        return try {
            val scanner = AppScanner(applicationContext)
            val engine = RiskEngine()

            val results = scanner.scan().map { engine.score(it) }
            val flaggedCount = results.count { it.riskLevel != RiskLevel.LOW }

            if (flaggedCount > 0) {
                NotificationHelper.sendSuspiciousActivityNotification(
                    applicationContext,
                    flaggedCount
                )
            }
            Result.success()
        }catch(e: Exception){
            Result.failure()

        }
    }
}
