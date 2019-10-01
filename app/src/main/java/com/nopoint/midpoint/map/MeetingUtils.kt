package com.nopoint.midpoint.map

import com.nopoint.midpoint.models.*

object MeetingUtils {

    fun sortRequests(unsortedRequests: List<MeetingRequest>,currentUser: User): MutableList<MeetingRequestRow> {
        val map = HashMap<MeetingType, ArrayList<MeetingRequest>>()
        for (req in unsortedRequests) {
            val title = getHeaderTitle(req, currentUser)
            var requests = map[title]
            if (requests == null) {
                requests = ArrayList()
                map[title] = requests
            }
            requests.add(req)
        }
        map.toSortedMap(compareBy {it.ordinal}) //sorts by importance
        val requests = ArrayList<MeetingRequestRow>()
        for ((key, value) in map) {
            requests.add(MeetingRequestRow(null, key, RowType.HEADER))
            value.mapTo(requests) { MeetingRequestRow(it, key, RowType.REQUEST) }
        }
        return requests
    }

    private fun getHeaderTitle(req: MeetingRequest, currentUser: User): MeetingType {
        return when {
            req.status == 2 -> MeetingType.REJECTED
            req.status != 0 -> MeetingType.ACTIVE
            req.receiver == currentUser.id -> MeetingType.INCOMING
            req.requester == currentUser.id -> MeetingType.OUTGOING
            else -> MeetingType.ACTIVE
        }
    }
}