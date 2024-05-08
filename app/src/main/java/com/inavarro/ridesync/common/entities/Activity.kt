package com.inavarro.ridesync.common.entities

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class Activity(
    @DocumentId
    val id: String? = null,

    val title: String? = null,
    val description: String? = null,
    val date: Long? = null,
    val location: GeoPoint? = null,
    val direction: String? = null,
    val image: String? = null,
)
