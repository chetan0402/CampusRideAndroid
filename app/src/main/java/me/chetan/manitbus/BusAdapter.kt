package me.chetan.manitbus

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class BusAdapter(
    private val context: Context,
    private val busModelArrayList: ArrayList<BusModel>): RecyclerView.Adapter<BusAdapter.ViewHolder>() {

        class ViewHolder(busView: View): RecyclerView.ViewHolder(busView){
            val busID: TextView = busView.findViewById(R.id.busID)
            val busRoute: TextView = busView.findViewById(R.id.busRoute)
            val busItem: ConstraintLayout = busView.findViewById(R.id.busItem)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return busModelArrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = busModelArrayList[position]
        holder.busID.text = model.busID
        holder.busRoute.text = model.busRoute

        if(model.busID == MapActivity.highlight){
            holder.busItem.background = ContextCompat.getDrawable(context,R.drawable.round_border)
        }else{
            holder.busItem.background = ContextCompat.getDrawable(context,R.color.white)
        }
        holder.busItem.setOnClickListener {
            MapActivity.highlight = model.busID
            (context as MapActivity).highlightBus()
            context.drawList()
        }
    }
}