package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue
import org.wordpress.android.fluxc.model.order.LineItem
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosGetLineItemBookingIdsTest : BaseUnitTest() {

    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val siteModel: SiteModel = mock()

    private val sut = WooPosGetLineItemBookingIds(orderStore, selectedSite)

    @Test
    fun `given line items with booking metadata, when invoked, then returns mapping of itemId to bookingId`() =
        testBlocking {
            whenever(selectedSite.get()).thenReturn(siteModel)
            val orderEntity = mock<OrderEntity>()
            whenever(orderStore.getOrderByIdAndSite(eq(1L), any())).thenReturn(orderEntity)
            whenever(orderEntity.getLineItemList()).thenReturn(
                listOf(
                    LineItem(
                        id = 10L,
                        metaData = listOf(
                            WCMetaData(id = 1L, key = "_booking_id", value = WCMetaDataValue("42"))
                        )
                    ),
                    LineItem(
                        id = 20L,
                        metaData = listOf(
                            WCMetaData(id = 2L, key = "some_other_key", value = WCMetaDataValue("99"))
                        )
                    )
                )
            )

            val result = sut(1L)

            assertThat(result).containsExactlyEntriesOf(mapOf(10L to 42L))
        }

    @Test
    fun `given no booking metadata, when invoked, then returns empty map`() = testBlocking {
        whenever(selectedSite.get()).thenReturn(siteModel)
        val orderEntity = mock<OrderEntity>()
        whenever(orderStore.getOrderByIdAndSite(eq(1L), any())).thenReturn(orderEntity)
        whenever(orderEntity.getLineItemList()).thenReturn(
            listOf(
                LineItem(id = 10L, metaData = emptyList())
            )
        )

        val result = sut(1L)

        assertThat(result).isEmpty()
    }

    @Test
    fun `given order not found, when invoked, then returns empty map`() = testBlocking {
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(orderStore.getOrderByIdAndSite(eq(1L), any())).thenReturn(null)

        val result = sut(1L)

        assertThat(result).isEmpty()
    }
}
