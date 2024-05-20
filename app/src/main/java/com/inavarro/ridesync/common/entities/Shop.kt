package com.inavarro.ridesync.common.entities

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class Shop(
    @DocumentId
    val id: String = "",

    val name: String = "",
    val city: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val web: String = "",
    val photo: String = "",
    val location: GeoPoint? = null,
    val type: String = "",
)
