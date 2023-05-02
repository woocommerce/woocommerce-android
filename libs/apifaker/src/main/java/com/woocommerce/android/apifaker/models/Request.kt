package com.woocommerce.android.apifaker.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
internal data class Request(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ApiType,
    val path: String,
    val httpMethod: HttpMethod? = null,
    val queryParameters: List<QueryParameter> = emptyList(),
    val body: String? = null
)
