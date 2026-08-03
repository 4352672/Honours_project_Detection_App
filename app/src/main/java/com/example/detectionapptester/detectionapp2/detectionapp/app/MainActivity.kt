package com.example.detectionapptester.detectionapp2.detectionapp.app

import android.app.AppOpsManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView

    private lateinit var btnScan: Button

    private lateinit var rvResults: RecyclerView

    private lateinit var tvNoResults: TextView

    private lateinit var adapter: ScanResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnScan = findViewById(R.id.btnScan)
        rvResults = findViewById(R.id.rvResults)
        tvNoResults= findViewById(R.id.tvNoResults)

        val btnInfo = findViewById<Button>(R.id.btnInfo)
        val btnExit = findViewById<Button>(R.id.btnExit)

       adapter = ScanResultAdapter()
       rvResults.layoutManager = LinearLayoutManager(this)
       rvResults.adapter = adapter

       btnScan.setOnClickListener { onScanClicked() }

       loadAppCount()

       btnInfo.setOnClickListener {
           AlertDialog.Builder(this)
               .setTitle("Detector Information")
               .setMessage("Detector is an Intrusion Detection System (IDS) for Android.\n\n"+
               "It scans your device for suspisious app permisiions and unusual"+
               " network or battery behaviour.")
               .setPositiveButton("OK",null)
               .show()
       }

       btnExit.setOnClickListener { finishAffinity() }

       }
    private fun loadAppCount(){
        lifecycleScope.launch {
            val count = withContext(Dispatchers.IO){
                val pm = packageManager
                pm.getInstalledPackages(0).count {pkg ->
                    val appInfo = pkg.applicationInfo?: return@count false
                    pkg.packageName!in WhiteList.packageNames &&
                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM)==0


                }
        }
            setStatus("$count apps installed - tap Scan to analyse","#4CAF50")

    }
    }
    private fun onScanClicked(){
        if (!hasUsageStatsPermission()){
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Detector is an Intrusion Detection System (IDS) for Android.\n\n"+
                        "It scans your device for suspisious app permisiions and unusual"+
                        " network or battery behaviour.")
                .setPositiveButton("Open Settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                .setNegativeButton("cancel",null)
                .show()
            return
        }
        runScan()

    }
    private fun runScan(){
        btnScan.isEnabled = false
        setStatus("Scanning...","#FFA726")

        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO){
                val scanner = AppScanner(applicationContext)
                val engine = RiskEngine()

                scanner.scan()
                    .map{engine.score(it)}
                    .sortedByDescending { it.spywareProbability }
            }
            adapter.updateResults(results)
            val flagged = results.count{it.riskLevel != RiskLevel.LOW}

            if (results.isEmpty()){
                rvResults.visibility = View.GONE
                tvNoResults.visibility = View.VISIBLE
                val statusColor = if (flagged > 0) "#F44336" else "#4CAF50"
                setStatus("Scan Complete- $flagged app${if (flagged ==1) "" else "s"}flagged",statusColor)
            }
            btnScan.isEnabled = true
        }
    }
    private fun setStatus(text:String,hexColor:String){
        tvStatus.text=text
        tvStatus.setTextColor(Color.parseColor(hexColor))
    }
    private fun hasUsageStatsPermission():Boolean{
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

}