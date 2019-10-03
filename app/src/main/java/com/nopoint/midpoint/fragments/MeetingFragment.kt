package com.nopoint.midpoint.fragments

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.model.LatLng
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.nopoint.midpoint.*
import com.nopoint.midpoint.map.Directions
import com.nopoint.midpoint.map.MeetingUtils
import com.nopoint.midpoint.map.models.Direction
import com.nopoint.midpoint.models.*
import com.nopoint.midpoint.networking.API
import com.nopoint.midpoint.networking.APIController
import com.nopoint.midpoint.networking.ServiceVolley
import com.nopoint.midpoint.networking.*
import kotlinx.android.synthetic.main.fragment_meeting.view.*
import org.json.JSONObject
import java.io.IOException

/**
 * A simple [Fragment] subclass.
 */
class MeetingFragment : Fragment() {
    private val service = ServiceVolley()
    private val apiController = APIController(service)
    var currentLocation: Location? = null
    private var meetingRequests = mutableListOf<MeetingRequestRow>()
    private lateinit var localUser: LocalUser

    private lateinit var meetingUsernameInput: TextInputEditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_meeting, container, false)
        // view.request_btn.setOnClickListener { sendRequest(view.friend_username.text.toString()) }
        view.refresh_btn.setOnClickListener { getRequests() }
        meetingUsernameInput = view.findViewById(R.id.meeting_username_input)

        view.request_btn.setOnClickListener {

            if(meetingUsernameInput.text!!.isNotEmpty()) {
                val reqString = meetingUsernameInput.text.toString()
                sendRequest(reqString)
            }
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        localUser = CurrentUser.getLocalUser(activity!!)!!
        getRequests()
    }


    // RESPOND TO MEETING REQUEST
    private fun onResponseSent(meetingRequest: MeetingRequest) {
        val body = JSONObject()
        val requesterLoc = Location("")
        requesterLoc.longitude = meetingRequest.requesterLongitude
        requesterLoc.latitude = meetingRequest.requesterLatitude

        val latestLocation = currentLocation

        val fullRouteURL = Directions.buildUrlFromLatLng(
            LatLng(latestLocation!!.latitude, latestLocation.longitude),
            LatLng(requesterLoc.latitude, requesterLoc.longitude)
        )

        Log.d("MEETING", "$fullRouteURL")

        // Get full route
        apiController.get(API.DIRECTIONS, fullRouteURL) { response ->
            if (response != null) {
                val result = Gson().fromJson(response.toString(), Direction::class.java)
                Log.d("MEETING", "$result")
                var midpointLatLng: LatLng? = Directions.getAbsoluteMidpoint(result)

                // Midpoint calculation success
                if (midpointLatLng != null) {
                    body.put("requestId", meetingRequest.id)
                    body.put("lat", latestLocation.latitude)
                    body.put("lng", latestLocation.longitude)
                    body.put("middleLat", midpointLatLng.latitude)
                    body.put("middleLng", midpointLatLng.longitude)
                    body.put("response", 1) //TODO allow setting different response types

                    Log.d("MEETING", "$body")

                    val midpointURL = Directions.buildUrlFromLatLng(
                        LatLng(latestLocation.latitude, latestLocation.longitude),
                        LatLng(midpointLatLng.latitude, midpointLatLng.longitude)
                    )

                    // Respond to requester
                    apiController.post(API.LOCAL_API, MEETING_RESPOND_URL, body, localUser.token) { response ->
                        try {
                            val map = parentFragment as MapFragment
                            map.getDirectionsToAbsoluteMidpoint(midpointURL)
                        } catch (e: IOException) {
                            Log.e("MEETING", "$e")
                        }
                    }
                }
            }
        }
    }

    private fun showOnMap(meetingRequest: MeetingRequest) {
        val middlePoint = Location("")
        middlePoint.latitude = meetingRequest.meetingPointLatitude!!
        middlePoint.longitude = meetingRequest.meetingPointLongitude!!

        val fullRoute = Location("")
        fullRoute.latitude = meetingRequest.requesterLatitude
        fullRoute.longitude = meetingRequest.requesterLongitude
        val map = parentFragment as MapFragment
        map.getDirections(destinationCoord = fullRoute)
    }

    private fun getRequests() {
        val path = "/meeting-request/all"
        apiController.get(API.LOCAL_API, path, localUser.token) { response ->
            try {
                Log.d("RES", "$response")
                val meetingResponse =
                    Gson().fromJson(response.toString(), MeetingRequestResponse::class.java)
                meetingRequests =
                    MeetingUtils.sortRequests(meetingResponse.requests, localUser.user)
                // Was causing a crash
                if (view != null && activity != null) {
                    initializeRecyclerView()
                }
            } catch (e: IOException) {
                Log.e("MEETING", "$e")
            }
        }
    }

    private fun initializeRecyclerView() {
        view!!.requests_view.adapter =
            MeetingRequestsAdapter(
                meetingRequests,
                (activity as MainActivity),
                ::onResponseSent,
                ::showOnMap
            )
        view!!.requests_view.layoutManager = LinearLayoutManager((activity as MainActivity))
        val dividerItemDecoration = DividerItemDecoration(
            view!!.requests_view.context,
            (view!!.requests_view.layoutManager as LinearLayoutManager).orientation
        )
        view!!.requests_view.addItemDecoration(dividerItemDecoration)

        val swipeHandler = object : SwipeToDeleteCallback(activity!!) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = view!!.requests_view.adapter as MeetingRequestsAdapter
                val removed = adapter.removeAt(viewHolder.adapterPosition)
                deleteRequest(removed.meetingRequest!!)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(view!!.requests_view)
    }

    private fun sendRequest(username: String = "testpete") {
        val params = JSONObject()
        val path = "/meeting-request/request"
        params.put("receiver", username)
        params.put("lat", currentLocation!!.latitude)
        params.put("lng", currentLocation!!.longitude)
        apiController.post(API.LOCAL_API, path, params, localUser.token) { response ->
            try {
                //TODO Refresh recycler view with new request
                Log.d("RES", "$response")
                val msg = if (response != null) {
                    "Meeting request sent"
                } else "Bugg"
                Snackbar.make(
                    activity!!.findViewById(android.R.id.content),
                    msg ?: "Big bugg",
                    Snackbar.LENGTH_LONG
                ).show()

            } catch (e: IOException) {
                Log.e("MEETING", "$e")
            }
        }
    }

    private fun deleteRequest(meetingRequest: MeetingRequest) {
        val params = JSONObject()
        val path = "/meeting-request/delete"
        params.put("requestId", meetingRequest.id)
        apiController.post(API.LOCAL_API, path, params, localUser.token) { response ->
            try {
                Log.d("RES", "$response")
                val msg =
                    if (response?.optString("msg").isNullOrEmpty()) {
                        response?.getString("errors")
                    } else response?.getString("msg")
                Snackbar.make(
                    activity!!.findViewById(android.R.id.content),
                    msg!!,
                    Snackbar.LENGTH_LONG
                ).show()
            } catch (e: IOException) {
                Log.e("MEETING", "$e")
            }
        }
    }

}
