package com.woocommerce.android.apifaker.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.woocommerce.android.apifaker.db.EndpointTypeConverter

@Entity
internal data class Endpoint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @field:TypeConverters(EndpointTypeConverter::class) val type: EndpointType,
    val path: String,
    val body: String?
)
