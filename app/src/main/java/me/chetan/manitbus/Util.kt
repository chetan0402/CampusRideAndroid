package me.chetan.manitbus

import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.min


class Util {
    companion object{
        fun streamToString(inputStream: InputStream): String {
            val textBuilder = StringBuilder()
            BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                var c: Int
                while ((reader.read().also { c = it }) != -1) {
                    textBuilder.append(c.toChar())
                }
            }
            return textBuilder.toString()
        }

        private fun rayCastIntersect(point: GeoPoint, vertA: GeoPoint, vertB: GeoPoint): Boolean {
            val px = point.longitude
            var py = point.latitude
            var ax = vertA.longitude
            var ay = vertA.latitude
            var bx = vertB.longitude
            var by = vertB.latitude

            if (ay > by) {
                ax = vertB.longitude
                ay = vertB.latitude
                bx = vertA.longitude
                by = vertA.latitude
            }

            if (py == ay || py == by) {
                py += 0.00000001
            }

            if ((py > by || py < ay) || (px > max(ax, bx))) {
                return false
            }

            if (px < min(ax, bx)) {
                return true
            }

            val red = if ((ax != bx)) ((by - ay) / (bx - ax)) else Double.POSITIVE_INFINITY
            val blue = if ((ax != px)) ((py - ay) / (px - ax)) else Double.POSITIVE_INFINITY
            return blue >= red
        }

        fun isPointInPolygon(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
            var intersectCount = 0
            for (i in 0 until polygon.size - 1) {
                if (rayCastIntersect(point, polygon[i], polygon[i + 1])) {
                    intersectCount++
                }
            }
            if (rayCastIntersect(point, polygon[polygon.size - 1], polygon[0])) {
                intersectCount++
            }
            return (intersectCount % 2) == 1
        }
    }
}