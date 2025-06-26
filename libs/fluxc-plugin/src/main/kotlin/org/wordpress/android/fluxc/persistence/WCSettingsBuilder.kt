package org.wordpress.android.fluxc.persistence

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.WCSettingsModel

@Table(name = "WCSettingsModel", addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCSettingsBuilder(
    @PrimaryKey @Column private var id: Int = 0,
    @Column var localSiteId: Int = 0,
    @Column var currencyCode: String = "",
    @Column var currencyPosition: String = "",
    @Column var currencyThousandSeparator: String = "",
    @Column var currencyDecimalSeparator: String = "",
    @Column var currencyDecimalNumber: Int = 2,
    @Column var countryCode: String = "",
    @Column var stateCode: String = "",
    @Column var address: String = "",
    @Column var address2: String = "",
    @Column var city: String = "",
    @Column var postalCode: String = "",
    @Column var couponsEnabled: Boolean = false
) : Identifiable {
    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    fun build(): WCSettingsModel {
        return WCSettingsModel(
            localSiteId = localSiteId,
            currencyCode = currencyCode,
            currencyPosition = WCSettingsModel.CurrencyPosition.Companion.fromString(
                currencyPosition
            ),
            currencyThousandSeparator = currencyThousandSeparator,
            currencyDecimalSeparator = currencyDecimalSeparator,
            currencyDecimalNumber = currencyDecimalNumber,
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
