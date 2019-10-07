package com.nopoint.midpoint.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.nopoint.midpoint.R
import kotlinx.android.synthetic.main.row_friend_request.view.*
import kotlinx.android.synthetic.main.row_friends_list.view.*

class FriendsListAdapter(
    private val onAcceptFriendRequestClickListener: OnRespondFriendRequestClickListener,
    private val rows: ArrayList<IRowFriend>,
    private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    interface IRowFriend
    class FriendRow(val username: String) : IRowFriend
    class FriendRequestRow(val username: String) : IRowFriend

    class FriendVH(v: View): RecyclerView.ViewHolder(v) {
        val username: TextView = v.friends_list_row_username
    }

    inner class FriendRequestVH(v: View) : RecyclerView.ViewHolder(v) {
        val username: TextView = v.row_friend_request_username
        private val buttonAccept = v.row_friend_request_btn_accept!!
        private val buttonDeny = v.row_friend_request_btn_deny!!

        fun bind(clickListener: OnRespondFriendRequestClickListener) {
            buttonAccept.setOnClickListener {
                clickListener.onAcceptClicked(buttonAccept, adapterPosition)
                removeItem(this, adapterPosition)
            }
            buttonDeny.setOnClickListener {
                clickListener.onDenyClicked(buttonDeny, adapterPosition)
                removeItem(this, adapterPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return rows.size
    }

    override fun getItemViewType(position: Int): Int =
        when (rows[position]) {
            is FriendRow -> TYPE_FRIEND
            is FriendRequestRow -> TYPE_FRIEND_REQUEST
            else -> throw IllegalArgumentException()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        TYPE_FRIEND -> FriendVH(LayoutInflater.from(context).inflate(R.layout.row_friends_list, parent, false))
        TYPE_FRIEND_REQUEST -> FriendRequestVH(LayoutInflater.from(context).inflate(R.layout.row_friend_request, parent, false))
        else -> throw IllegalArgumentException()
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        when (holder.itemViewType) {
            TYPE_FRIEND -> {
                onBindFriend(holder, rows[position] as FriendRow)
            }
            TYPE_FRIEND_REQUEST -> {
                onBindFriendRequest(holder, rows[position] as FriendRequestRow)
            }
            else -> throw IllegalArgumentException()
        }

    private fun onBindFriend(holder: RecyclerView.ViewHolder, row: FriendRow) {
        val friendRow = holder as FriendVH
        friendRow.username.text = row.username
    }

    private fun onBindFriendRequest(holder: RecyclerView.ViewHolder, row: FriendRequestRow) {
        val friendRequestRow = holder as FriendRequestVH
        holder.bind(onAcceptFriendRequestClickListener)
        friendRequestRow.username.text = row.username
    }

    private fun removeItem(holder: RecyclerView.ViewHolder, position: Int) {
        rows.removeAt(position)
        notifyDataSetChanged()
    }

    companion object {
        private const val TYPE_FRIEND = 0
        private const val TYPE_FRIEND_REQUEST = 1
    }
}

interface OnRespondFriendRequestClickListener {
    fun onAcceptClicked(button: MaterialButton, position: Int)
    fun onDenyClicked(button: MaterialButton, position: Int)

}


