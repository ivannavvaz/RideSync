package com.inavarro.ridesync.common.entities

import com.google.firebase.Timestamp
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.firestore.DocumentId

@IgnoreExtraProperties
data class Photo(
    //@get:Exclude
    @DocumentId
    var id: String = "",

    var photoUrl: String = " ",
    val date: Timestamp? = null
)
