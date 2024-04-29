package com.inavarro.ridesync.common.entities

data class Message(
    val text: String? = null,
    val senderId: String? = null,
    val senderName: String? = null,
    val sendTime: Long? = null,
)
