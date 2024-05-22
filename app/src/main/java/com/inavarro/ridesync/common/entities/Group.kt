package com.inavarro.ridesync.common.entities

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentReference

data class Group(
    @DocumentId
    val id: String? = null,

    val name: String? = null,
    val description: String? = null,
    val location: String? = null,
    val users: List<String>? = null,
    val photo: String? = null,
    val lastMessageRef: DocumentReference? = null,
    val lastMessageTime: Long? = null,
    val private: Boolean? = false
)
