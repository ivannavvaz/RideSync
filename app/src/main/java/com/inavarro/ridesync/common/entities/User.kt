package com.inavarro.ridesync.common.entities

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String? = null,

    val fullName: String? = null,
    val email: String? = null,
    val username: String? = null,
    val profilePhoto: String? = null,
    val premium: Boolean? = false,
    val publicProfile: Boolean? = false
)
