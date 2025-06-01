package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingConfigDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.Item
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.UpdateShipmentsResponse
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SplitShipmentTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock { doReturn(SiteModel().apply { id = 1 }).whenever(it).getOrNull() }
    private val wooShippingLabelRepository: WooShippingLabelRepository = mock()
    private val configDataStore: WooShippingConfigDataStore = mock {
        doReturn(flowOf(null)).whenever(it).observeConfig(any())
    }

    private val sut = SplitShipment(selectedSite, wooShippingLabelRepository, configDataStore)

    @Test
    fun `when shipment has single quantity, endpoint should be called with empty subItems`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder()
        whenever(wooShippingLabelRepository.updateShipments(any(), eq(order.id), any())).doReturn(
            WooResult(UpdateShipmentsResponse(true, emptyMap()))
        )

        sut.invoke(order.id, listOf(ShipmentUIModel(id = "0", items = listOf(singleItem))))

        val expectedShipmentMap = mapOf("0" to listOf(Item(singleItem.itemId, emptyList())))
        verify(wooShippingLabelRepository).updateShipments(any(), eq(order.id), eq(expectedShipmentMap))
    }

    @Test
    fun `when shipment has multiple quantity, endpoint should be called with valid format`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder()
        whenever(wooShippingLabelRepository.updateShipments(any(), eq(order.id), any())).doReturn(
            WooResult(UpdateShipmentsResponse(true, emptyMap()))
        )

        sut.invoke(order.id, listOf(ShipmentUIModel(id = "0", items = listOf(multipleQuantityItem))))

        val expectedSubItems = listOf("1000-sub-0", "1000-sub-1", "1000-sub-2")
        val expectedShipmentMap = mapOf("0" to listOf(Item(multipleQuantityItem.itemId, expectedSubItems)))
        verify(wooShippingLabelRepository).updateShipments(any(), eq(order.id), eq(expectedShipmentMap))
    }

    private val singleItem = ShippableItemModel(
        itemId = 1000,
        productId = 10,
        title = "Product",
        price = BigDecimal(100),
        quantity = 1f,
        weight = 1f,
        currency = "USD",
        width = 1f,
        height = 1f,
        length = 1f
    )

    private val multipleQuantityItem = singleItem.copy(quantity = 3f)
}
