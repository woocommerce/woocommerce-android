package com.woocommerce.android.apifaker.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
internal data class Endpoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EndpointType,
    val path: String,
    val body: String?
)
