package org.wordpress.android.fluxc.persistence.dao

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.wc.order.OrderTestUtils

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
internal class OrderShipmentProvidersDaoTest {

    private lateinit var sut: OrderShipmentProvidersDao

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        sut = databaseRule.db.orderShipmentProvidersDao
    }

    @Test
    fun testGetOrderShipmentProvidersForOrder() = runTest {
        val siteModel = SiteModel().apply { id = 1 }
        val json = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order-shipment-providers.json")

        // When
        val providers = OrderTestUtils.getOrderShipmentProvidersFromJson(json, siteModel.id).apply {
            assertThat(size).isEqualTo(54)
        }
        sut.replaceAll(siteModel.localId(), providers)

        // Then
        assertThat(
            sut.observeOrderShipmentProviders(siteModel.localId()).first()
        ).containsExactlyInAnyOrderElementsOf(providers)

        // When
        val updatedProviders = providers.plus(OrderTestUtils.generateOrderShipmentProvider(siteModel.id))
        sut.replaceAll(siteModel.localId(), updatedProviders)

        // Then
        assertThat(
            sut.observeOrderShipmentProviders(siteModel.localId()).first()
        ).containsExactlyInAnyOrderElementsOf(updatedProviders)
    }
}
