package com.woocommerce.android.ui.orders.creation.product.discount

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_PRODUCT_DISCOUNT_ADD
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_PRODUCT_DISCOUNT_REMOVE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ORDER_DISCOUNT_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_ORDER_DISCOUNT_TYPE_FIXED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_ORDER_DISCOUNT_TYPE_PERCENTAGE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.DiscountAmountValidationState.Invalid
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.DiscountAmountValidationState.Valid
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.CurrencyFormattingParameters
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.WCSettingsModel
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class OrderCreateEditProductDiscountViewModelTest : BaseUnitTest() {
    private val resourceProvider: ResourceProvider = mock {
        on { getString(R.string.order_creation_discount_too_big_error) } doReturn
            "Discount cannot be greater than the price"
    }
    private val currencySymbolFinder: CurrencySymbolFinder = mock {
        on { findCurrencySymbol(anyString()) } doReturn "$"
    }

    private val savedState = OrderCreateEditProductDiscountFragmentArgs(
        item,
        "usd"
    ).initSavedStateHandle()

    private val siteParams = SiteParameters(
        currencyCode = "USD",
        currencySymbol = "$",
        currencyFormattingParameters = CurrencyFormattingParameters(
            "", "", 2, WCSettingsModel.CurrencyPosition.LEFT
        ),
        weightUnit = "kg",
        dimensionUnit = "cm",
        gmtOffset = 0f
    )

    private val parameterRepository: ParameterRepository = mock {
        on(it.getParameters(any(), any())).thenReturn(siteParams)
    }

    private val tracker: AnalyticsTrackerWrapper = mock()

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

            sut.onDiscountAmountChange(60.toBigDecimal())

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

            sut.onDiscountAmountChange(40.toBigDecimal())

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

            sut.onDiscountAmountChange(50.toBigDecimal())

            sut.viewState.test {
                assertIs<Valid>(awaitItem().discountValidationState)
            }
        }

    @Test
    fun `given valid discount, when done clicked, then should navigate up with correct result`() {
        val item = Order.Item.EMPTY.copy(
            quantity = 1F,
            subtotal = 100F.toBigDecimal(),
        )
        val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            item,
            "usd"
        ).initSavedStateHandle()

        val sut = createSut(savedStateHandle)
        var lastEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever {
            lastEvent = it
        }
        sut.onDiscountAmountChange(1.toBigDecimal())
        sut.onDoneClicked()
        with(lastEvent) {
            assertThat(this).isNotNull
            assertThat(this).isInstanceOf(MultiLiveEvent.Event.ExitWithResult::class.java)
            val result = (this as MultiLiveEvent.Event.ExitWithResult<*>).data as Order.Item
            assertEquals(99F, result.total.toFloat())
        }
    }

    @Test
    fun `given discount amount, when switching to percentage discount, should calculate correct value`() = testBlocking {
        val item = Order.Item.EMPTY.copy(
            quantity = 1F,
            subtotal = 999.toBigDecimal(),
        )
        val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            item,
            "usd"
        ).initSavedStateHandle()
        val sut = createSut(savedStateHandle)
        sut.onDiscountAmountChange(99.toBigDecimal())
        sut.onPercentageDiscountSelected()
        sut.viewState.test {
            val viewState = awaitItem()
            assertThat(viewState.discountType)
                .isInstanceOf(OrderCreateEditProductDiscountViewModel.DiscountType.Percentage::class.java)
            assertThat(viewState.discountAmount).isEqualTo("9.90990991")
        }
    }

    @Test
    fun `given percentage discount, when switching to discount amount, should calculate correct value`() = testBlocking {
        val item = Order.Item.EMPTY.copy(
            quantity = 1F,
            subtotal = 33.toBigDecimal(),
        )
        val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            item,
            "usd"
        ).initSavedStateHandle()
        val sut = createSut(savedStateHandle)
        sut.onPercentageDiscountSelected()
        sut.onDiscountAmountChange(13.toBigDecimal())
        sut.onAmountDiscountSelected()
        sut.viewState.test {
            val viewState = awaitItem()
            assertThat(viewState.discountType)
                .isInstanceOf(OrderCreateEditProductDiscountViewModel.DiscountType.Amount::class.java)
            assertThat(viewState.discountAmount).isEqualTo("4.29")
        }
    }

    @Test
    fun `given percentage discount, when done clicked, then should track event`() {
        val item = Order.Item.EMPTY.copy(
            quantity = 1F,
            subtotal = 33.toBigDecimal(),
        )
        val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            item,
            "usd"
        ).initSavedStateHandle()
        val sut = createSut(savedStateHandle)
        sut.onPercentageDiscountSelected()
        sut.onDoneClicked()
        verify(tracker).track(
            ORDER_PRODUCT_DISCOUNT_ADD,
            mapOf(KEY_ORDER_DISCOUNT_TYPE to VALUE_ORDER_DISCOUNT_TYPE_PERCENTAGE)
        )
    }

    @Test
    fun `given fixed amount discount, when done clicked, then should track event`() {
        val item = Order.Item.EMPTY.copy(
            quantity = 1F,
            subtotal = 33.toBigDecimal(),
        )
        val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            item,
            "usd"
        ).initSavedStateHandle()
        val sut = createSut(savedStateHandle)
        sut.onAmountDiscountSelected()
        sut.onDoneClicked()
        verify(tracker).track(
            ORDER_PRODUCT_DISCOUNT_ADD,
            mapOf(KEY_ORDER_DISCOUNT_TYPE to VALUE_ORDER_DISCOUNT_TYPE_FIXED)
        )
    }

    @Test
    fun `when remove discount clicked, then should track event`() {
        val item = Order.Item.EMPTY.copy(
            quantity = 1F,
            subtotal = 33.toBigDecimal(),
        )
        val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            item,
            "usd"
        ).initSavedStateHandle()
        val sut = createSut(savedStateHandle)
        sut.onDiscountRemoveClicked()
        verify(tracker).track(ORDER_PRODUCT_DISCOUNT_REMOVE)
    }

    private fun createSut(
        savedStateHandle: SavedStateHandle = savedState
    ): OrderCreateEditProductDiscountViewModel {
        return OrderCreateEditProductDiscountViewModel(
            savedStateHandle,
            resourceProvider,
            CalculateItemDiscountAmount(),
            tracker,
            parameterRepository,
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
