package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.settings.CurrencyPosition

@Entity(
    tableName = "SettingsEntity",
    primaryKeys = ["localSiteId"]
)
data class WCSettingsModel(
    val localSiteId: LocalId,
    val currencyCode: String,
    val currencyPosition: CurrencyPosition,
    val currencyThousandSeparator: String,
    val currencyDecimalSeparator: String,
    val currencyDecimalNumber: Int,
    val countryCode: String,
    val stateCode: String,
    val address: String,
    val address2: String,
    val city: String,
    val postalCode: String,
    val couponsEnabled: Boolean
)
