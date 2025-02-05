package org.wordpress.android.fluxc.persistence

import android.content.ContentValues
import com.wellsql.generated.WCSettingsModelTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import java.util.Locale
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition

object WCSettingsSqlUtils {
    fun insertOrUpdateSettings(settings: WCSettingsModel): Int {
        val orderResult = WellSql.select(WCSettingsBuilder::class.java)
                .where()
                .equals(WCSettingsModelTable.LOCAL_SITE_ID, settings.localSiteId)
                .endWhere()
                .asModel

        return if (orderResult.isEmpty()) {
            // Insert
            WellSql.insert(settings.toBuilder()).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = orderResult[0].id
            WellSql.update(WCSettingsBuilder::class.java).whereId(oldId)
                    .put(settings.toBuilder(), UpdateAllExceptId(WCSettingsBuilder::class.java)).execute()
        }
    }

    fun setCouponsEnabled(site: SiteModel, value: Boolean): Int {
        return WellSql.update(WCSettingsBuilder::class.java)
            .whereId(site.id)
            .put(value) {
                val cv = ContentValues()
                cv.put(WCSettingsModelTable.COUPONS_ENABLED, it)
                cv
            }.execute()
    }

    fun getSettingsForSite(site: SiteModel): WCSettingsModel? {
        return WellSql.select(WCSettingsBuilder::class.java)
                .where()
                .equals(WCSettingsModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel.firstOrNull()?.build()
    }

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
                    currencyPosition = CurrencyPosition.fromString(currencyPosition),
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

    private fun WCSettingsModel.toBuilder(): WCSettingsBuilder {
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
