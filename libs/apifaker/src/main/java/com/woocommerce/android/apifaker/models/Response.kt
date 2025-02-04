package com.woocommerce.android.apifaker.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Request::class,
            parentColumns = ["id"],
            childColumns = ["endpointId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
internal data class Response(
    @PrimaryKey val endpointId: Long = 0,
    val statusCode: Int,
    val body: String? = null
)
