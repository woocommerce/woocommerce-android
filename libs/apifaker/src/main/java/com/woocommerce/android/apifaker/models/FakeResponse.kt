package com.woocommerce.android.apifaker.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
internal data class FakeResponse(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val statusCode: Int,
    val body: String?,
    val endpointId: Int
)

