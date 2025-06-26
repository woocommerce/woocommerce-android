package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(
    tableName = "WCSettingsEntity",
    primaryKeys = ["localSiteId"]
)
data class WCSettingsEntity(
    val localSiteId: LocalId,
    val currencyCode: String = "",
    val currencyPosition: String = "",
    val currencyThousandSeparator: String = "",
    val currencyDecimalSeparator: String = "",
    val currencyDecimalNumber: Int = 2,
    val countryCode: String = "",
    val stateCode: String = "",
    val address: String = "",
    val address2: String = "",
    val city: String = "",
    val postalCode: String = "",
    val couponsEnabled: Boolean = false
)
