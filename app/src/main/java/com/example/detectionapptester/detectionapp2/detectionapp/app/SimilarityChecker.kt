package com.example.detectionapptester.detectionapp2.detectionapp.app

class SimilarityChecker {
    companion object{
        private const val SIMILARITY_THRESHOLD = 0.75 //checks apps who has similar package or display names are considered impersonators.
    }

    data class Result(val isImpersonator : Boolean,val impersonating: String?)

    fun check(packageName : String, appName:String):Result{
        for((whitelistedPkg, whitelistedName) in WhiteList.displayNames){
            val pkgSimilarity = similarity(packageName, whitelistedPkg)
            val nameSimilarity = similarity(appName.lowercase(), whitelistedName.lowercase())

            if (pkgSimilarity>= SIMILARITY_THRESHOLD || nameSimilarity>= SIMILARITY_THRESHOLD){
                return Result(isImpersonator = true, impersonating = whitelistedName)
            }
        }
        return Result(isImpersonator = false, impersonating = null)
    }

    private fun levenshtein(s1 : String, s2: String): Int{
        val m =s1.length
        val n =s2.length
        val dp = Array(m+1){
            IntArray(n+1)
        }
        for (i in 0..m)dp[i][0]=i
        for (j in 0..n)dp[0][j]=j
        for (i in 0..m){
            for (j in 0..n) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
            }else{
                1+ minOf(dp[i - 1][j],dp[i][j - 1],dp[i - 1][j-1])
            }
        }
    }
        return dp[m][n]

    }
    private fun similarity(s1 : String, s2: String): Double{
        val maxLen = maxOf(s1.length,s2.length)
        if (maxLen == 0) return 1.0
        return 1.0 - levenshtein(s1, s2).toDouble()/maxLen
    }

}