package com.woocommerce.android.apifaker.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Endpoint::class,
            parentColumns = ["id"],
            childColumns = ["endpointId"]
        )
    ]
)
internal data class FakeResponse(
    @PrimaryKey val endpointId: Long = 0,
    val statusCode: Int,
    val body: String?,
)

