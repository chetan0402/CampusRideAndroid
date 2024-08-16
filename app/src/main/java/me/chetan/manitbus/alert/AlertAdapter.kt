package me.chetan.manitbus.alert

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.chetan.manitbus.R

class AlertAdapter(
    private val context: Context,
    private val alertList: ArrayList<AlertModel>
): RecyclerView.Adapter<AlertAdapter.ViewHolder>(){

    class ViewHolder(alertView: View):RecyclerView.ViewHolder(alertView){
        val alertMessage: TextView = alertView.findViewById(R.id.alert_message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.alert_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return alertList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.alertMessage.text = alertList[position].message
    }

}