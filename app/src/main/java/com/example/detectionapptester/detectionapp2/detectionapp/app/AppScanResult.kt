package com.example.detectionapptester.detectionapp2.detectionapp.app

data class AppScanResult (
    val packageName: String,
    val appName: String,
    val permissions:List<String>,
    val isImpersonator:Boolean,
    val impersonating:String?,
    val riskLevel:RiskLevel = RiskLevel.LOW,
    val spywareProbability: Int =0,
    val networkStats: NetworkStats? = null,  //null if PACKAGE_USAGE_STATS
    val networkScoreAddition: Int=0 //Additional score contributed by network signals

)