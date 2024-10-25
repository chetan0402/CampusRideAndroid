package me.chetan.manitbus

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class BusAdapter(
    private val context: Context,
    private val busModelArrayList: ArrayList<BusModel>): RecyclerView.Adapter<BusAdapter.ViewHolder>() {

        class ViewHolder(busView: View): RecyclerView.ViewHolder(busView){
            val busID: TextView = busView.findViewById(R.id.busID)
            val busWhere: TextView = busView.findViewById(R.id.busWhere)
            val busRoute: TextView = busView.findViewById(R.id.busRoute)
            val busItem: ConstraintLayout = busView.findViewById(R.id.busItem)
            val busUpdate: TextView = busView.findViewById(R.id.busUpdate)
            val busWarning: ImageView = busView.findViewById(R.id.busWarning)
            private var handler = Handler(Looper.getMainLooper())
            private var runnable: Runnable? = null

            fun bind(model: BusModel){
                updateTime(model)
                stopUpdates()
                runnable = Runnable {
                    updateTime(model)
                    handler.postDelayed(runnable!!, 60000)
                }
                handler.post(runnable!!)
            }

            private fun updateTime(model: BusModel){
                busUpdate.text = "Last updated: ${Util.getMinutesAgo(model.update)}min ago"
                if(Util.getMinutesAgo(model.update)>2){
                    busWarning.visibility = View.VISIBLE
                }else{
                    busWarning.visibility = View.GONE
                }
            }

            fun stopUpdates(){
                runnable?.let{ handler.removeCallbacks(it)}
            }
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
        holder.busWhere.text = context.getString(R.string.currently_at, model.busWhere)
        holder.busRoute.text = model.busRoute
        holder.bind(model)

        if(model.busID == MapActivity.highlight){
            holder.busItem.background = ContextCompat.getDrawable(context,R.drawable.round_border)
            holder.busUpdate.setTypeface(null,Typeface.BOLD)
        }else{
            holder.busItem.background = ContextCompat.getDrawable(context,R.color.black)
            holder.busUpdate.setTypeface(null,Typeface.NORMAL)
        }
        holder.busItem.setOnClickListener {
            MapActivity.highlight = model.busID
            (context as MapActivity).highlightBus()
            context.updateBusList()
            context.mapMoveToBus(model.busID)
        }
        holder.busWarning.setOnClickListener {
            (context as MapActivity).runOnUiThread {
                Snackbar.make(context.findViewById(R.id.main),"Location of this bus maybe unreliable",Snackbar.LENGTH_SHORT)
                    .setAction("Learn more") {
                        MaterialAlertDialogBuilder(context)
                            .setTitle("Location of this bus maybe unreliable")
                            .setMessage("Both the EICHER API and the GPS data from bus driver haven't been updated for more than 2 minutes server-side. This could be due to poor network connectivity from driver's side or the bus being stationary.")
                            .setPositiveButton("OK",null)
                            .setNeutralButton("Learn more") {
                                _, _ -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${MainActivity.BASE}/how-it-works")))
                            }
                            .show()
                    }.show()
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.stopUpdates()
    }
}