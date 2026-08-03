package com.example.detectionapptester.detectionapp2.detectionapp.app

import android.app.AppOpsManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Process
import android.app.usage.NetworkStats as AndroidNetworkStats

data class NetworkStats(
    val totalBytesReceived: Long=0L,
    val totalBytesSent: Long=0L,
    val backgroundBytesSent: Long=0L,
    val backgroundBytesReceived: Long=0L,
    val isUploadHeavy: Boolean = false, //Bytes sent> bytes received by > 40%
    val hasBackgroundActivity: Boolean = false, // Background bytes sent above the threshold
    val hasContinuousTransmission: Boolean = false, //Activity across most time buckets
    val hasIdleSpikes: Boolean = false //Large spikes during low-activity periods
){
    companion object{
        fun empty() = NetworkStats()
    }
}

class NetworkMonitor(private val context: Context){
    companion object{
        private const val BG_ACTIVITY_THRESHOLD=512*1024L //Minimum background bytes that are sent to flag background activity (512 KB)

        private const val UPLOAD_HEAVY_RATIO =1.4 //The upload is considered heavy when tx>rx* this ratio

        private const val CONTINUOUS_TX_RATIO =0.6 //Flag continous transmission when this fraction of buckets have activity

        private const val IDLE_SPIKE_MULTIPLIER =5 //A spike must be this many times larger than the average bucket to be flagged

        private const val IDLE_SPIKE_MAX_AVG =50_000L //Only flag idle spikes when average bucket is below this (low-activity baseline)
    }


    fun getNetworkStats(uid: Int): NetworkStats{
        if(!hasUsageStatsPermission()) return NetworkStats.empty()
        val manager = context.getSystemService( Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24*60*60*1000L //Last 24 hours

        var totalRx = 0L
        var totalTx = 0L
        val bucketTotals = mutableListOf<Long>() //Per-bucket totals for pattern detection

        @Suppress("DEPRECATION")
        val networkTypes = listOf(ConnectivityManager.TYPE_WIFI,ConnectivityManager.TYPE_MOBILE)
        for (networkType in networkTypes){
            try{
                val statsSession = manager.queryDetailsForUid(  //SuscriberId is null for Wifi and safety returns all-subscriber data for mobile
                networkType,null,startTime,endTime,uid)     //Passing null avoids the Read_PHONE_STATE requirement
                val bucket = AndroidNetworkStats.Bucket()
                while (statsSession.hasNextBucket()){
                    statsSession.getNextBucket(bucket)
                    val rx = bucket.rxBytes.coerceAtLeast(0L)
                    val tx = bucket.txBytes.coerceAtLeast(0L)
                    totalRx += rx
                    totalTx += tx
                    bucketTotals.add(tx+rx)
                }
                statsSession.close()
            }catch (e: SecurityException){//Permission revoked mid scan-return safe default
                return NetworkStats.empty()
            }catch (e: Exception){ //Network type unabailable on this device e.g no sim which will skip and continue
        }


    }
        // Scoring flags

    val isUploadHeavy = totalRx > 0L && totalTx > (totalRx * UPLOAD_HEAVY_RATIO).toLong()//Bytes sent exceed bytes received more than 40%

    val hasBackgroundActivity = totalTx > BG_ACTIVITY_THRESHOLD //Total bytes sent exceeds threshold used as a proxy for background exfiltration since the public API cannot split fg vs bg bytes directly.

    val nonZeroBuckets = bucketTotals.count{it > 0}
    val hasContinuousTransmission = bucketTotals.size >= 4 && nonZeroBuckets.toDouble()/bucketTotals.size>=CONTINUOUS_TX_RATIO // Active in the 60% of all recorded time  buckets

    val avgBucket = if (bucketTotals.isNotEmpty())bucketTotals.sum()/bucketTotals.size else 0L
    val hasIdleSpikes = avgBucket in 1  until  IDLE_SPIKE_MAX_AVG && bucketTotals.any(){it > avgBucket * IDLE_SPIKE_MULTIPLIER}// bursts are 5x the average during a low-activity baseline
    return NetworkStats(
        totalBytesReceived = totalRx,
        totalBytesSent = totalTx,
        backgroundBytesSent = totalTx, //Approximated - public api CANNOT SPLIT FG/BG
        backgroundBytesReceived = totalRx,
        isUploadHeavy = isUploadHeavy,
        hasBackgroundActivity = hasBackgroundActivity,
        hasContinuousTransmission = hasContinuousTransmission,
        hasIdleSpikes = hasIdleSpikes
    )
    }

    private fun hasUsageStatsPermission(): Boolean{
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception){
            false
        }
    }
}
