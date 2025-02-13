package org.wordpress.android.fluxc.model.taxes

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(tableName = "TaxBasedOnSetting")
data class TaxBasedOnSettingEntity(
    @PrimaryKey val localSiteId: LocalId,
    val selectedOption: String,
)
