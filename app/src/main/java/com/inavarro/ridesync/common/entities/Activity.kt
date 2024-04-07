package com.inavarro.ridesync.common.entities

import com.google.firebase.firestore.DocumentId

data class Activity(
    @DocumentId
    val id: String? = null,

    val title: String? = null,
    val description: String? = null,
    val date: Long? = null,
    val image: String? = null,
)
