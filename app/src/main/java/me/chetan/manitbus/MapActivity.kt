package me.chetan.manitbus

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import me.chetan.manitbus.alert.AlertAdapter
import me.chetan.manitbus.alert.AlertModel
import org.json.JSONObject
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.HttpURLConnection
import java.net.URL

class MapActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var myLocation: Marker
    private var locationNotice: Boolean = false
    private var internetNotice: Boolean = false
    private lateinit var recyclerViewAlert: RecyclerView
    private lateinit var recyclerViewBus: RecyclerView
    private var activityVisible=true
    private var pollInstances: MutableList<Job> = mutableListOf()
    private val pathLines: ArrayList<Polyline> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(false)
        map.setMultiTouchControls(true)
        val mapController = map.controller
        mapController.setZoom(16.0)
        mapController.setCenter(GeoPoint(23.2100,77.4050))

        myLocation = Marker(map)
        myLocation.setInfoWindow(null)
        map.overlays.add(myLocation)

        recyclerViewBus=findViewById(R.id.busList)
        val busAdapter = BusAdapter(this, busList)
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerViewBus.layoutManager=linearLayoutManager
        recyclerViewBus.adapter=busAdapter

        alertList = ArrayList()
        alertList.add(AlertModel(ContextCompat.getString(this,R.string.dev)))
        recyclerViewAlert=findViewById(R.id.alertList)
        val alertAdapter = AlertAdapter(this, alertList)
        val linearLayoutManagerAlert = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerViewAlert.layoutManager=linearLayoutManagerAlert
        recyclerViewAlert.adapter=alertAdapter

        val polygon = listOf(
            GeoPoint(23.2210, 77.4038),
            GeoPoint(23.2160, 77.3963),
            GeoPoint(23.2090, 77.3978),
            GeoPoint(23.2055, 77.4060),
            GeoPoint(23.2074, 77.4135),
            GeoPoint(23.2146, 77.4183),
            GeoPoint(23.2161, 77.4154),
            GeoPoint(23.2139, 77.4105),
            GeoPoint(23.2197, 77.4100)
        )

        myLocation.icon = ContextCompat.getDrawable(this,R.drawable.baseline_my_location_24)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null ) {
                    if(Util.isPointInPolygon(GeoPoint(location.latitude,location.longitude),polygon)){
                        myLocation.position = GeoPoint(location.latitude,location.longitude)
                        if(locationNotice){
                            locationNotice=false
                            alertList.forEach {
                                if(it.message == "Smartphone GPS unreliable."){
                                    alertList.remove(it)
                                }
                            }
                            recyclerViewAlert.adapter?.notifyItemRangeChanged(0,alertList.size+1)
                        }
                    }else{
                        if(!locationNotice){
                            locationNotice=true
                            alertList.add(AlertModel("Smartphone GPS unreliable."))
                            recyclerViewAlert.adapter?.notifyItemInserted(alertList.size)
                        }
                    }
                }
            }
        }

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_DENIED){
            val locationPermissionRequest = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                when{
                    permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                        startLocationUpdates()
                    }
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)->{
                        Toast.makeText(this,"Location shown maybe be different from actual due to approximate location",Toast.LENGTH_LONG).show()
                        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                        startLocationUpdates()
                    } else -> {
                        Toast.makeText(this,"Location permission denied",Toast.LENGTH_SHORT).show()
                    }
                }
            }
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
        }else{
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            startLocationUpdates()
        }

        busList.forEach {
            val bus = Marker(map)
            busMarker.add(bus)
            bus.position = it.geoPoint
            bus.id = it.busID
            bus.icon = ContextCompat.getDrawable(this,R.drawable.baseline_directions_bus_24)
            bus.setOnMarkerClickListener { _, _ ->
                highlight=bus.id
                highlightBus()
                updateBusList()
                recyclerViewBus.smoothScrollToPosition(busList.indexOfFirst { it.busID == bus.id })
                true
            }
            map.overlays.add(bus)
        }
        map.invalidate()
    }

    override fun onResume() {
        super.onResume()
        activityVisible=true
        pollManger()
    }

    override fun onPause() {
        super.onPause()
        activityVisible=false
    }

    private fun pollManger(){
        pollInstances.forEach {
            it.cancel()
            pollInstances.remove(it)
        }
        val pollInstance = this.lifecycleScope.launch(Dispatchers.IO) {
            while(activityVisible){
                try{
                    poll()
                }catch (e:Exception){
                    runOnUiThread {
                        if(!internetNotice){
                            internetNotice=true
                            alertList.add(AlertModel("Unstable internet."))
                            recyclerViewAlert.adapter?.notifyItemInserted(alertList.size)
                        }
                    }
                    e.printStackTrace()
                }
                var i=0
                while(i<10){
                    yield()
                    Thread.sleep(1_000)
                    i++
                }
            }
        }
        pollInstances.add(pollInstance)
    }

    fun updateBusList(){
        val recyclerView=findViewById<RecyclerView>(R.id.busList)
        recyclerView.adapter?.let { recyclerView.adapter?.notifyItemRangeChanged(0, it.itemCount) }
    }

    private fun moveBus(){
        busMarker.forEach{ marker ->
            busList.forEach{
                if(marker.id == it.busID){
                    marker.position = it.geoPoint
                }
            }
        }
        map.invalidate()
    }

    fun highlightBus(){
        busMarker.forEach {
            if(it.id== highlight){
                it.icon = ContextCompat.getDrawable(this,R.drawable.baseline_directions_bus_24_green)
            }else{
                it.icon = ContextCompat.getDrawable(this,R.drawable.baseline_directions_bus_24)
            }
        }
        map.invalidate()
    }

    fun drawPaths(){
        pathLines.forEach {
            map.overlays.remove(it)
        }
        busList.forEach {
            var i=0
            val waypoints = ArrayList<GeoPoint>()
            while (i<it.path.length()){
                val ele = it.path.getJSONObject(i)
                waypoints.add(GeoPoint(ele.getDouble("lat"),ele.getDouble("long")))
                i++
            }
            val path = Polyline()
            path.setPoints(waypoints)
            path.color = ContextCompat.getColor(this,R.color.blue)
            path.outlinePaint.strokeWidth = 10f
            path.outlinePaint.strokeCap = Paint.Cap.ROUND
            pathLines.add(path)
            map.overlays.add(path)
        }
        map.invalidate()
    }

    private fun poll(){
        val url = URL("${MainActivity.BASE}/poll")
        (url.openConnection() as HttpURLConnection).run {
            setRequestProperty("Accept","application/json")
            connectTimeout=2000
            val response = JSONObject(Util.streamToString(inputStream)).getJSONArray("busList")
            var i=0
            while(i<response.length()){
                val bus = response.getJSONObject(i)
                busList.forEach {
                    if(bus.getDouble("lat")!=0.0 && bus.getDouble("long")!=0.0 && it.busID == bus.getString("id")){
                        it.geoPoint = GeoPoint(bus.getDouble("lat"),bus.getDouble("long"))
                        it.update = bus.getString("last_update")
                        it.busWhere = bus.getString("where")
                        it.path = bus.getJSONArray("path")
                    }
                }
                drawPaths()
                runOnUiThread {
                    moveBus()
                    if(internetNotice){
                        internetNotice=false
                        var position: Int = -1
                        alertList.forEachIndexed { i,it ->
                            if(it.message == "Unstable internet."){
                                position = i
                                alertList.remove(it)
                            }
                        }
                        recyclerViewAlert.adapter?.notifyItemRemoved(position+1)
                    }
                    recyclerViewBus.adapter?.notifyDataSetChanged()
                }
                i++
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY,10000).setMinUpdateDistanceMeters(5f).build(),
            locationCallback,
            Looper.getMainLooper())
    }

    fun mapMoveToBus(busId: String){
        val busLoc = busMarker.find { it.id == busId }?.position
        if (busLoc != null) {
            if(busLoc.latitude==0.0 && busLoc.longitude==0.0) return
        }
        map.controller.animateTo(busLoc)
    }

    companion object{
        val busList: ArrayList<BusModel> = ArrayList()
        var busMarker: ArrayList<Marker> = ArrayList()
        var alertList: ArrayList<AlertModel> = ArrayList()
        var highlight: String = "none"
    }
}