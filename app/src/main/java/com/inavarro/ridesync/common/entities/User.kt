package com.inavarro.ridesync.common.entities

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String? = null,

    val name: String? = null,
    val email: String? = null,
    val image: String? = null,
    //val groups: List<String>? = null
)
