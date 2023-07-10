package com.woocommerce.android.ui.orders.creation.product.discount

import app.cash.turbine.test
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.DiscountAmountValidationState.Invalid
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class OrderCreateEditProductDiscountViewModelTest : BaseUnitTest() {
    private val resourceProvider: ResourceProvider = mock {
        on { getString(anyInt()) } doReturn ""
    }
    private val currencySymbolFinder: CurrencySymbolFinder = mock {
        on { findCurrencySymbol(anyString()) } doReturn "$"
    }
    @Test
    fun `given invalid format of discount, when done clicked, then should return Invalid state`() = testBlocking {
        val sut = createSut()

        sut.onDiscountAmountChange("invalid input")

        sut.viewState.test {
            assertIs<Invalid>(awaitItem().discountValidationState)
        }
    }

    private fun createSut(): OrderCreateEditProductDiscountViewModel {
        val savedStateHandle = OrderCreateEditProductDiscountFragmentArgs(item, "usd").initSavedStateHandle()
        return OrderCreateEditProductDiscountViewModel(
            savedStateHandle,
            resourceProvider,
            CalculateItemDiscountAmount(),
            currencySymbolFinder
        )
    }

    private companion object {
        val item = Order.Item.EMPTY.copy(quantity = 2F, subtotal = 100F.toBigDecimal(), total = 80F.toBigDecimal())
    }
}