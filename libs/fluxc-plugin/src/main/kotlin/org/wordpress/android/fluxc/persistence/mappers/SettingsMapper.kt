package org.wordpress.android.fluxc.persistence.mappers

import java.util.Locale
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.persistence.WCSettingsBuilder

object SettingsMapper {

    fun WCSettingsModel.toBuilder(): WCSettingsBuilder {
        return WCSettingsBuilder(
            localSiteId = this.localSiteId,
            currencyCode = this.currencyCode,
            currencyPosition = this.currencyPosition.name.lowercase(Locale.getDefault()),
            currencyThousandSeparator = this.currencyThousandSeparator,
            currencyDecimalSeparator = this.currencyDecimalSeparator,
            currencyDecimalNumber = this.currencyDecimalNumber,
            countryCode = countryCode,
            stateCode = stateCode,
            address = address,
            address2 = address2,
            city = city,
            postalCode = postalCode,
            couponsEnabled = couponsEnabled
        )
    }
}
