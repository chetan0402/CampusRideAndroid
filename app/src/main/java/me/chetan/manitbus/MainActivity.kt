package me.chetan.manitbus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        var updateNeeded = false
        this.lifecycleScope.launch(Dispatchers.IO){
            try{
                (URL("$BASE/version").openConnection() as HttpURLConnection).run {
                    setRequestProperty("Accept","application/json")
                    connectTimeout=2000
                    runOnUiThread {
                        findViewById<TextView>(R.id.loadingText).text = "Checking for updates..."
                    }
                    val response = JSONObject(Util.streamToString(inputStream))
                    if(response.getInt("version")!=applicationContext.packageManager.getPackageInfo(applicationContext.packageName,0).versionCode){
                        updateNeeded=true
                        runOnUiThread {
                            findViewById<TextView>(R.id.loadingText).text = "Please update.. \n $BASE"
                            findViewById<LinearProgressIndicator>(R.id.loadingBar).hide()
                            MaterialAlertDialogBuilder(this@MainActivity).run {
                                title = "New version available"
                                setMessage("Please update")
                                setPositiveButton("Update") { _ , _ ->
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BASE)))
                                }
                            }
                        }
                    }
                }
            }catch(e:Exception){
                runOnUiThread {
                    internetFailed()
                }
                e.printStackTrace()
                return@launch
            }
            if(updateNeeded) return@launch
            try{
                (URL("$BASE/init").openConnection() as HttpURLConnection).run{
                    setRequestProperty("Accept","application/json")
                    connectTimeout=2000
                    runOnUiThread {
                        val loading=findViewById<TextView>(R.id.loadingText)
                        loading.text="Getting bus data..."
                    }
                    val response = JSONObject(Util.streamToString(inputStream))
                    val listOfBus = response.getJSONArray("busList")
                    var i=0
                    MapActivity.busList.removeAll{true}
                    while(i<listOfBus.length()){
                        val bus = listOfBus.getJSONObject(i)
                        MapActivity.busList.add(BusModel(bus.getString("id"),bus.getString("route"), GeoPoint(bus.getDouble("lat"),bus.getDouble("long")), bus.getString("last_update")))
                        i++
                    }
                }
                startActivity(Intent(this@MainActivity,MapActivity::class.java))
                finish()
            }catch(e:Exception){
                runOnUiThread {
                    internetFailed()
                }
                e.printStackTrace()
            }
        }
    }

    private fun internetFailed(){
        val loadingBar=findViewById<LinearProgressIndicator>(R.id.loadingBar)
        loadingBar.hide()
        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle("Unusable internet")
            .setMessage("Please check your internet connection.")
            .setNegativeButton("Close") { _, _ ->
                finish()
            }
            .setPositiveButton("Retry") { _ , _ ->
                startActivity(Intent(this@MainActivity,MainActivity::class.java))
            }
            .show()
    }

    companion object{
        const val BASE = "http://ebus.manit.ac.in"
    }
}