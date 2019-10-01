package com.nopoint.midpoint

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nopoint.midpoint.models.MeetingRequest
import com.nopoint.midpoint.models.MeetingRequestRow
import com.nopoint.midpoint.models.MeetingType
import com.nopoint.midpoint.models.RowType
import kotlinx.android.synthetic.main.request_header.view.*
import kotlinx.android.synthetic.main.request_row.view.*

class MeetingRequestsAdapter(
    private val requests: List<MeetingRequestRow>,
    private val context: Context,
    private val respond: (meetingRequest: MeetingRequest) -> Unit,
    private val showOnMap: (meetingRequest: MeetingRequest) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {

    override fun getItemCount(): Int = requests.size

    // Inflates the item views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(context)
        val inflatedView: View = when (viewType) {
            RowType.REQUEST.ordinal -> layoutInflater.inflate(R.layout.request_row, parent, false)
            else -> layoutInflater.inflate(R.layout.request_header, parent, false)
        }
        return ViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        if (request.rowType == RowType.REQUEST) {
            when(request.type) {
                MeetingType.REJECTED -> holder.userName!!.text = request.meetingRequest!!.receiverUsername + " rejected"
                MeetingType.ACTIVE -> {
                    holder.userName!!.text = request.meetingRequest!!.receiverUsername + " accepted"
                    holder.meetBtn!!.text = "Show route"
                    holder.meetBtn.setOnClickListener { showOnMap(request.meetingRequest) }
                }
                MeetingType.INCOMING -> {
                    holder.userName!!.text = request.meetingRequest!!.receiverUsername + " incoming"
                    holder.meetBtn!!.setOnClickListener { respond(request.meetingRequest) }
                }
                MeetingType.OUTGOING -> {
                    holder.userName!!.text = request.meetingRequest!!.receiverUsername + " sent"
                    holder.meetBtn!!.visibility = View.GONE
                }
            }
        } else {
            holder.headerTxt!!.text = request.type.toString()
        }
    }

    override fun getItemViewType(position: Int) =
        requests[position].rowType.ordinal
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    // Holds the TextView that will add each animal to
    val userName: TextView? = view.receiver_txt
    val meetBtn: Button? = view.meet_btn
    val headerTxt: TextView? = view.header_txt
}