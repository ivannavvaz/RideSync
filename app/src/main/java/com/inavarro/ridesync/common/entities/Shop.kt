package com.inavarro.ridesync.common.entities

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class Shop(
    @DocumentId
    val id: String? = null,

    val name: String? = null,
    val description: String? = null,
    val city: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val web: String? = null,
    val photo: String? = null,
    val location: GeoPoint? = null,
    val type: String? = null,
)
