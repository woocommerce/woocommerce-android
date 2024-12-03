package com.woocommerce.android.ui.orders.wooshippinglabels

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.WooShippingViewState
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingLabelCreationViewModelTest : BaseUnitTest() {
    private val orderId = 1L
    private val defaultShippableItems = List(3) {
        ShippableItemModel(
            itemId = it.toLong(),
            productId = it.toLong(),
            title = "Product $it",
            price = BigDecimal(it),
            quantity = it.toFloat(),
            weight = it.toFloat(),
            currency = "USD",
            imageUrl = "https://example.com/image.jpg",
            width = it.toFloat(),
            height = it.toFloat(),
            length = it.toFloat()
        )
    }
    private val defaultShippingLines = List(3) {
        Order.ShippingLine(
            methodTitle = "Shipping Line $it",
            total = BigDecimal(it),
            methodId = it.toString(),
            itemId = it.toLong(),
            totalTax = BigDecimal.ZERO,
        )
    }
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val getShippableItems: GetShippableItems = mock()
    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(any<BigDecimal>(), any(), any()) } doAnswer {
            val amount = it.getArgument(0) as BigDecimal
            "$ ${amount.toPlainString()}"
        }
    }
    private val savedState: SavedStateHandle =
        WooShippingLabelCreationFragmentArgs(orderId = orderId).toSavedStateHandle()

    private lateinit var sut: WooShippingLabelCreationViewModel

    fun createViewModel() {
        sut = WooShippingLabelCreationViewModel(
            orderDetailRepository = orderDetailRepository,
            getShippableItems = getShippableItems,
            currencyFormatter = currencyFormatter,
            savedState = savedState
        )
    }

    @Test
    fun `when the order NO contains shipping lines, then NO shipping lines summary is displayed`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = emptyList()
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShippableItems(any())) doReturn defaultShippableItems

        createViewModel()

        val currentViewState = sut.viewState.value
        assert(currentViewState is WooShippingViewState.DataState)
        val dataState = currentViewState as WooShippingViewState.DataState
        assert(dataState.shippingLines.isEmpty())
    }

    @Test
    fun `when the order contains shipping lines, then shipping lines summary is displayed`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShippableItems(any())) doReturn defaultShippableItems

        createViewModel()

        val currentViewState = sut.viewState.value
        assert(currentViewState is WooShippingViewState.DataState)
        val dataState = currentViewState as WooShippingViewState.DataState
        assert(dataState.shippingLines.isNotEmpty())
        assertEquals(dataState.shippingLines.size, defaultShippingLines.size)
    }
}
