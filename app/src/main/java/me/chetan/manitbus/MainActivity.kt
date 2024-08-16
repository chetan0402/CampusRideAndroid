package me.chetan.manitbus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets


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
        this.lifecycleScope.launch(Dispatchers.IO){
            val url = URL("$BASE/init")
            try{
                (url.openConnection() as HttpURLConnection).run{
                    setRequestProperty("Accept","application/json")
                    connectTimeout=2000
                    runOnUiThread {
                        val loading=findViewById<TextView>(R.id.loadingText)
                        loading.text="Getting bus data..."
                    }
                    val response = JSONObject(Util.streamToString(inputStream))
                    val listOfBus = response.getJSONArray("busList")
                    var i=0;
                    while(i<listOfBus.length()){
                        val bus = listOfBus.getJSONObject(i)
                        MapActivity.busList.add(BusModel(bus.getString("id"),bus.getString("route"), GeoPoint(bus.getDouble("lat"),bus.getDouble("long"))))
                        i++
                    }
                }
                startActivity(Intent(this@MainActivity,MapActivity::class.java))
            }catch(e:Exception){
                runOnUiThread {
                    val loading=findViewById<TextView>(R.id.loadingText)
                    val loadingBar=findViewById<LinearProgressIndicator>(R.id.loadingBar)
                    loading.text = "Internet issue. Unable to get data.\n Check your connection.\n Restart application"
                    loadingBar.hide()
                }
                e.printStackTrace()
            }
        }
    }

    companion object{
        const val BASE = "http://192.168.1.19:8000"
    }
}