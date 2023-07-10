package com.woocommerce.android.ui.orders.creation.product.discount

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.DiscountAmountValidationState.Invalid
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.DiscountAmountValidationState.Valid
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class OrderCreateEditProductDiscountViewModelTest : BaseUnitTest() {
    private val resourceProvider: ResourceProvider = mock {
        on { getString(R.string.order_creation_discount_invalid_number_error) } doReturn "Discount is not a valid number"
        on { getString(R.string.order_creation_discount_too_big_error) } doReturn "Discount cannot be greater than the price"
    }
    private val currencySymbolFinder: CurrencySymbolFinder = mock {
        on { findCurrencySymbol(anyString()) } doReturn "$"
    }

    @Test
    fun `given invalid format of discount, when done clicked, then should return Invalid state`() =
        testBlocking {
            val sut = createSut()

            sut.onDiscountAmountChange("Aaaa")

            sut.viewState.test {
                val validationState = awaitItem().discountValidationState
                assertIs<Invalid>(validationState)
                assertThat(validationState.errorMessage).isEqualTo("Discount is not a valid number")
            }
        }

    @Test
    fun `given discount bigger than item's price, when done clicked, then should return Invalid state`() =
        testBlocking {
            val item = Order.Item.EMPTY.copy(
                quantity = 2F,
                subtotal = 100F.toBigDecimal(),
                total = 100F.toBigDecimal()
            )
            val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            item,
            "usd"
            ).initSavedStateHandle()

            val sut = createSut(savedStateHandle)

            sut.onDiscountAmountChange("60.0")

            sut.viewState.test {
                val validationState = awaitItem().discountValidationState
                assertIs<Invalid>(validationState)
                assertThat(validationState.errorMessage).isEqualTo("Discount cannot be greater than the price")
            }
        }

    @Test
    fun `given discount smaller than item's price, when done clicked, then should return Valid state`() =
        testBlocking {
            val item = Order.Item.EMPTY.copy(
                quantity = 2F,
                subtotal = 100F.toBigDecimal(),
                total = 100F.toBigDecimal()
            )
            val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
                item,
                "usd"
            ).initSavedStateHandle()

            val sut = createSut(savedStateHandle)

            sut.onDiscountAmountChange("40.0")

            sut.viewState.test {
                assertIs<Valid>(awaitItem().discountValidationState)
            }
        }

    @Test
    fun `given discount equal to item's price, when done clicked, then should return Valid state`() =
        testBlocking {
            val item = Order.Item.EMPTY.copy(
                quantity = 2F,
                subtotal = 100F.toBigDecimal(),
                total = 100F.toBigDecimal()
            )
            val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
                item,
                "usd"
            ).initSavedStateHandle()

            val sut = createSut(savedStateHandle)

            sut.onDiscountAmountChange("50.0")

            sut.viewState.test {
                assertIs<Valid>(awaitItem().discountValidationState)
            }
        }

    private fun createSut(
        savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            item,
            "usd"
        ).initSavedStateHandle()
    ): OrderCreateEditProductDiscountViewModel {
        return OrderCreateEditProductDiscountViewModel(
            savedStateHandle,
            resourceProvider,
            CalculateItemDiscountAmount(),
            currencySymbolFinder
        )
    }

    private companion object {
        val item = Order.Item.EMPTY.copy(
            quantity = 2F,
            subtotal = 100F.toBigDecimal(),
            total = 80F.toBigDecimal()
        )
    }
}