package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.entity.WCSettingsModel

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class SettingsDaoTest {
    private lateinit var sut: SettingsDao
    private lateinit var db: WCAndroidDatabase

    private val siteId = LocalId(1)
    private val sampleSettings = generateSettingsModel(siteId)

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sut = db.settingsDao
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `test upsertSettings and getSettings`() = runTest {
        // when
        sut.upsertSettings(sampleSettings)
        val result = sut.getSettings(siteId)

        // then
        assertThat(result).isEqualTo(sampleSettings)
    }

    @Test
    fun `test upsertSettings updates existing settings`() = runTest {
        // given
        sut.upsertSettings(sampleSettings)

        // when
        val updatedSettings = sampleSettings.copy(
            currencyCode = "EUR",
            countryCode = "FR"
        )
        sut.upsertSettings(updatedSettings)
        val result = sut.getSettings(siteId)

        // then
        assertThat(result).isEqualTo(updatedSettings)
    }

    @Test
    fun `test setCouponsEnabled updates couponsEnabled flag`() = runTest {
        // given
        sut.upsertSettings(sampleSettings)

        // when
        sut.setCouponsEnabled(siteId, true)
        val result = sut.getSettings(siteId)

        // then
        assertThat(result?.couponsEnabled).isTrue()

        // when
        sut.setCouponsEnabled(siteId, false)
        val updatedResult = sut.getSettings(siteId)

        // then
        assertThat(updatedResult?.couponsEnabled).isFalse()
    }

    companion object {
        fun generateSettingsModel(siteId: LocalId) = WCSettingsModel(
            localSiteId = siteId,
            currencyCode = "USD",
            currencyPosition = CurrencyPosition.LEFT,
            currencyThousandSeparator = ",",
            currencyDecimalSeparator = ".",
            currencyDecimalNumber = 2,
            countryCode = "US",
            stateCode = "CA",
            address = "123 Main St",
            address2 = "Apt 4",
            city = "San Francisco",
            postalCode = "94105",
            couponsEnabled = false
        )
    }
}
