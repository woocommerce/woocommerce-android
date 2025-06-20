package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.wc.order.OrderTestUtils
import kotlin.test.assertEquals

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
internal class OrderShipmentProvidersDaoTest {

    private lateinit var sut: OrderShipmentProvidersDao
    private lateinit var database: WCAndroidDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java).allowMainThreadQueries().build()
        sut = database.orderShipmentProvidersDao
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testGetOrderShipmentProvidersForOrder() = runTest {
        val siteModel = SiteModel().apply { id = 1 }
        val json = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order-shipment-providers.json")
        val providers = OrderTestUtils.getOrderShipmentProvidersFromJson(json, siteModel.id).toMutableList()
        assertEquals(54, providers.size)

        sut.upsertOrderShipmentProviders(providers)

        assertThat(
            sut.getOrderShipmentProvidersForSite(siteModel.localId())
        ).containsExactlyInAnyOrderElementsOf(providers)

        val updatedProviders = providers.plus(OrderTestUtils.generateOrderShipmentProvider(siteModel.id))
        sut.upsertOrderShipmentProviders(updatedProviders)

        assertThat(
            sut.getOrderShipmentProvidersForSite(siteModel.localId())
        ).containsExactlyInAnyOrderElementsOf(updatedProviders)
    }
}
