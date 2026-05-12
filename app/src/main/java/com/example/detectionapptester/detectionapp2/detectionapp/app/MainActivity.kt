package com.example.detectionapptester.detectionapp2.detectionapp.app

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnScan = findViewById<Button>(R.id.btnScan)
        val btnInfo = findViewById<Button>(R.id.btnInfo)
        val btnExit = findViewById<Button>(R.id.btnExit)
        //Scan
        btnScan.setOnClickListener { Toast.makeText(this, "scan", Toast.LENGTH_SHORT).show() }
        //Info
        btnInfo.setOnClickListener {
        AlertDialog.Builder(this)
            .setTitle("About Detector")
            .setMessage(
                "Detector Intrusion Detection System.\n\n" +
                "This app will scan the apps for malware and viruses"
            )
            .setPositiveButton("OK", null)
            .show()

        }
        //Exit
        btnExit.setOnClickListener {
            finishAffinity()
        }
    }
}