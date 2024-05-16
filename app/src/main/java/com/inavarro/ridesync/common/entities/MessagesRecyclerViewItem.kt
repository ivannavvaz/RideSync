package com.inavarro.ridesync.common.entities

sealed class MessagesRecyclerViewItem {
    data class ReceiverMessage(
        val text: String? = null,
        val senderId: String? = null,
        val sendTime: Long? = null,
    ) : MessagesRecyclerViewItem()

    data class TransmitterMessage(
        val text: String? = null,
        val senderId: String? = null,
        val sendTime: Long? = null,
    ) : MessagesRecyclerViewItem()
}