package org.wordpress.android.fluxc.model.data

import androidx.room.Entity

@Entity(
    tableName = "LocationEntity",
    primaryKeys = ["parentCode", "code"]
)
data class WCLocationModel(
    val parentCode: String = "",
    val code: String = "",
    val name: String = ""
)
