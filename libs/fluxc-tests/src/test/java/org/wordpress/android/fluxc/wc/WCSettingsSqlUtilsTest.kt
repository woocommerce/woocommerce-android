package org.wordpress.android.fluxc.wc

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition
import org.wordpress.android.fluxc.persistence.WCSettingsSqlUtils
import org.wordpress.android.fluxc.persistence.WCSettingsSqlUtils.WCSettingsBuilder
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCSettingsSqlUtilsTest {
    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                WCSettingsBuilder::class.java,
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testInsertOrUpdateSettings() {
        val settingsModel = WCSettingsModel(
                localSiteId = 5,
                currencyCode = "CAD",
                currencyPosition = CurrencyPosition.LEFT,
                currencyThousandSeparator = ",",
                currencyDecimalSeparator = ".",
                currencyDecimalNumber = 2,
                couponsEnabled = true
        )

        // Test inserting settings
        WCSettingsSqlUtils.insertOrUpdateSettings(settingsModel)

        val returnedSettings = WellSql.select(WCSettingsBuilder::class.java).asModel.first().build()
        assertEquals(settingsModel, returnedSettings)

        // Test updating settings
        val updatedSettingsModel = WCSettingsModel(
                localSiteId = 5,
                currencyCode = "EUR",
                currencyPosition = CurrencyPosition.RIGHT_SPACE,
                currencyThousandSeparator = ".",
                currencyDecimalSeparator = ",",
                currencyDecimalNumber = 2,
                couponsEnabled = true
        )
        WCSettingsSqlUtils.insertOrUpdateSettings(updatedSettingsModel)

        val returnedUpdatedSettings = WellSql.select(WCSettingsBuilder::class.java).asModel.first().build()
        assertEquals(updatedSettingsModel, returnedUpdatedSettings)
    }
}
