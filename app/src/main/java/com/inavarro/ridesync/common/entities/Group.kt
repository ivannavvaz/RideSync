package com.inavarro.ridesync.common.entities

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentReference

data class Group(
    @DocumentId
    val id: String? = null,

    val name: String? = null,
    val description: String? = null,
    val location: String? = null,
    val members: List<DocumentReference>? = null,
    val image: String? = null,
    val lastMessageRef: DocumentReference? = null,
    val lastMessageTime: Long? = null
)
