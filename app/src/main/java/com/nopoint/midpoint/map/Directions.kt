package com.nopoint.midpoint.map

import android.location.Location
import android.net.Uri
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import com.nopoint.midpoint.BuildConfig
import com.nopoint.midpoint.map.models.Direction
import com.nopoint.midpoint.map.models.FullRoute
import org.json.JSONObject

object Directions {
    /**
     * Builds url for Directions API based on location name
     * @param origin Starting point for the route
     * @param destination Name of the location to get the route to
     * @return String with the parameters for the directions API GET request
     */
    fun buildUrl(origin: Location?, destination: String): String {
        val builder = Uri.Builder()
        builder.appendQueryParameter("origin", "${origin?.latitude},${origin?.longitude}")
            .appendQueryParameter("destination", destination)
            .appendQueryParameter("mode", "walking")
            .appendQueryParameter("key", BuildConfig.api_key)
        return builder.build().toString()
    }

    /**
     * Builds url for Directions API based on location name
     * @param origin Starting point for the route
     * @param destination Location object with the coordinates for the destination
     * @return String with the parameters for the directions API GET request
     */
    fun buildUrl(origin: Location?, destination: Location): String {
        val builder = Uri.Builder()
        builder.appendQueryParameter("origin", "${origin?.latitude},${origin?.longitude}")
            .appendQueryParameter("destination", "${destination.latitude},${destination.longitude}")
            .appendQueryParameter("mode", "walking")
            .appendQueryParameter("key", BuildConfig.api_key)
        return builder.build().toString()
    }

    fun buildUrlFromLatLng(origin: LatLng, destination: LatLng?): String {
        val builder = Uri.Builder()
        builder.appendQueryParameter("origin", "${origin.latitude},${origin.longitude}")
            .appendQueryParameter("destination", "${destination!!.latitude},${destination.longitude}")
            .appendQueryParameter("mode", "walking")
            .appendQueryParameter("key", BuildConfig.api_key)
        return builder.build().toString()
    }

    /**
     * Builds route object from directions API json response
     * @param response The google directions API response json object
     * @return FullRoute object with coordinates for start & end and a polyline of the route
     */
    fun buildRoute(response: JSONObject): FullRoute {
        val directions = Gson().fromJson(response.toString(), Direction::class.java)
        val bestRoute = directions.routes[0]
        val leg = bestRoute.legs[0]
        return FullRoute(
            leg.start_address,
            leg.end_address,
            leg.start_location.lat,
            leg.start_location.lng,
            leg.end_location.lat,
            leg.end_location.lng,
            bestRoute.overview_polyline.points)
    }

    data class MidPointRoute(
        val startLat: Double,
        val startLng: Double,
        val endLat: Double,
        val endLng: Double
    )


    /**
     * Calculates & returns the middle point between 2 coordinates
     * @param start Location object with the starting coordinates
     * @param end Location object with the destination coordinates (e.g. the friend's coordinates)
     * @return Location object with the exact middle point between the start & end
     */
    fun getMiddlePoint(start: Location, end: Location): Location {
        val midLat = (start.latitude + end.latitude) / 2
        val midLng = (start.longitude + end.longitude) / 2
        val location = Location("")
        location.latitude = midLat
        location.longitude = midLng

        Log.d("DIRECTIONS", "lat: ${location.latitude}, lng: ${location.longitude}")
        return location
    }

    fun getAbsoluteMidpoint(result: Direction): LatLng? {
        Log.d("MEETING", "$result")
        val legs = result.routes[0].legs

        val path = ArrayList<LatLng>()

        for (i in 0 until legs[0].steps.size) {
            path.addAll(PolyUtil.decode(legs[0].steps[i].polyline.points))
        }

        // Get full distance between two points in meters
        val midpointInMeters = (legs[0].distance.value / 2).toDouble()

        // Calculate path's midpoint lat/lng from path's first point
        return calculateMidpointCoordinates(path, path[0], midpointInMeters)
    }

    private fun calculateMidpointCoordinates(path: List<LatLng>, origin: LatLng, distance: Double): LatLng? {
        var midpointCoordinates: LatLng? = null

        Log.d("CALCULATE", "GIVEN DISTANCE: $distance")

        // Check if given origin coordinates are found in the path's coordinates
        // withing five meters
        if (!PolyUtil.isLocationOnPath(origin, path, false, 1.0)) {
            return null
        }

        var accDistance = 0.0
        var foundStart = false
        val segment = ArrayList<LatLng>()


        // Loop through path list
        for (i in 0 until path.size - 1) {
            val segmentStart = path[i]
            val segmentEnd = path[i + 1]

            segment.clear()
            segment.add(segmentStart)
            segment.add(segmentEnd)

            var currentDistance: Double = 0.0

            //todo determine if check needed, probably not but keeping this here for now
            /*
            if (!foundStart) {
                Log.d("CALCULATE", " segment: $segment")

                // Check if given origin lies on or near segment within five meters
                if (PolyUtil.isLocationOnPath(origin, segment, false, 1.0)) {
                    Log.d("CALCULATE", "foundStart: ${PolyUtil.isLocationOnPath(origin, segment, false, 1.0)}")

                    foundStart = true


                    currentDistance = SphericalUtil.computeDistanceBetween(origin, segmentEnd)
                    Log.d("CALCULATE", " currentDistance: $currentDistance")

                    // Fix offset if current distance is greater than given distance
                    // which is full route's distance between start and end point divided by two
                    if (currentDistance > distance) {
                        Log.d("CALCULATE", "currentdist > dist")
                        val heading: Double = SphericalUtil.computeHeading(origin, segmentEnd)
                        midpointCoordinates = SphericalUtil.computeOffset(origin, distance - accDistance, heading)
                        break
                    }
                }

            // Start point found
            } else {
            */

            // Calculate length of a segment
            currentDistance = SphericalUtil.computeDistanceBetween(segmentStart, segmentEnd)

            // Current distance goes over given distance, midpoint found,
            // fix offset and set midpoint coordinates
            if (currentDistance + accDistance > distance) {
                val heading: Double = SphericalUtil.computeHeading(segmentStart, segmentEnd)
                midpointCoordinates = SphericalUtil.computeOffset(segmentStart, distance - accDistance, heading)
                break
            }

            //}

            accDistance += currentDistance
        }
        return midpointCoordinates
    }

}