package com.woocommerce.android.apifaker.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
internal data class Request(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ApiType,
    val httpMethod: HttpMethod?,
    val path: String,
    val body: String?
)
