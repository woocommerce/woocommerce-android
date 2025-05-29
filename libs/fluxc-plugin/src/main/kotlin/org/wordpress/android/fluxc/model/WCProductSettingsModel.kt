package org.wordpress.android.fluxc.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "ProductSettingsEntity",
)
data class WCProductSettingsModel(
    // note that there are many more product settings than this, but for now these are all we need
    @PrimaryKey val localSiteId: Int = 0,
    val weightUnit: String = "",
    val dimensionUnit: String = "",
)
