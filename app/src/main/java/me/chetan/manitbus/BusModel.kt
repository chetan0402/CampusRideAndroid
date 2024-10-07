package me.chetan.manitbus

import org.json.JSONArray
import org.osmdroid.util.GeoPoint

class BusModel(@JvmField var busID: String,@JvmField var busRoute: String,@JvmField var geoPoint: GeoPoint,@JvmField var update: String,@JvmField var busWhere: String,
    @JvmField var path: JSONArray)
