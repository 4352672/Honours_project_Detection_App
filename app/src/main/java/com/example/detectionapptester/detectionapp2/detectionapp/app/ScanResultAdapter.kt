package com.example.detectionapptester.detectionapp2.detectionapp.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class ScanResultAdapter:RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    private val results=mutableListOf<AppScanResult>()

    class ViewHolder(view:View):RecyclerView.ViewHolder(view) {
        val tvAppName:TextView=view.findViewById(R.id.tvAppName)
        val tvRiskLevel:TextView=view.findViewById(R.id.tvRiskLevel)
        val tvProbability:TextView=view.findViewById(R.id.tvProbability)
        val tvImpersonation:TextView=view.findViewById(R.id.tvImpersonation)
        val tvNetworkFlags:TextView=view.findViewById(R.id.tvNetworkFlags)
    }

    override fun onCreateViewHolder(parent:ViewGroup,viewType:Int):ViewHolder {
        val view=LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_result,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder:ViewHolder,position:Int) {
        val result = results[position]

        holder.tvAppName.text = result.appName
        holder.tvProbability.text = "${result.spywareProbability}% spyware probability"

        val (riskLabel, riskColor) = when(result.riskLevel){
            RiskLevel.LOW -> "LOW" to Color.parseColor("#4CAF50")
            RiskLevel.MEDIUM -> "MEDIUM" to Color.parseColor("#FF9800")
            RiskLevel.HIGH -> "HIGH" to Color.parseColor("#F44336")
            RiskLevel.SPYWARE -> "SPYWARE" to Color.parseColor("#B71C1C")
        }
        holder.tvRiskLevel.text=riskLabel
        holder.tvRiskLevel.setTextColor(riskColor)

        if (result.isImpersonator && result.impersonating != null) {
            holder.tvImpersonation.visibility = View.VISIBLE
            holder.tvImpersonation.text = "Impersonation Risk — mimics ${result.impersonating}"
        }else{
            holder.tvImpersonation.visibility = View.GONE
        }

        //Network behaviour flags
        val stats=result.networkStats
        if (stats==null || (stats.totalBytesSent == 0L && stats.totalBytesReceived == 0L)) {
            holder.tvNetworkFlags.visibility = View.GONE
        }else{
            val flags=mutableListOf<String>()
            if (stats.hasBackgroundActivity)    flags.add("Background network activity detected")
            if (stats.isUploadHeavy)            flags.add("Suspicious upload pattern detected")
            if (stats.hasIdleSpikes)            flags.add("Data spikes during idle detected")
            if (stats.hasContinuousTransmission) flags.add("Continuous data transmission detected")

            if (flags.isEmpty()) {
                holder.tvNetworkFlags.visibility=View.GONE
            }else{
                holder.tvNetworkFlags.visibility=View.VISIBLE
                holder.tvNetworkFlags.text=flags.joinToString("\n")
            }
        }
    }

    override fun getItemCount():Int=results.size

    fun updateResults(newResults: List<AppScanResult>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }
}
