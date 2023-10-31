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
import com.woocommerce.android.ui.orders.creation.OrderCreationProduct
import com.woocommerce.android.ui.orders.creation.ProductInfo
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.DiscountAmountValidationState.Invalid
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.DiscountAmountValidationState.Valid
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
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
        createProductItem(
            item = Order.Item.EMPTY.copy(
                quantity = 2F,
                subtotal = 100F.toBigDecimal(),
                total = 80F.toBigDecimal()
            )
        ),
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
    fun `given discount bigger than item's total price, when done clicked, then should return Invalid state`() =
        testBlocking {
            val item = Order.Item.EMPTY.copy(
                quantity = 2F,
                subtotal = 100F.toBigDecimal(),
                total = 100F.toBigDecimal()
            )
            val productItem = createProductItem(item)
            val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
                productItem,
                "usd"
            ).initSavedStateHandle()

            val sut = createSut(savedStateHandle)

            sut.onDiscountAmountChange(160.toBigDecimal())

            sut.viewState.test {
                val validationState = awaitItem().discountValidationState
                assertIs<Invalid>(validationState)
                assertThat(validationState.errorMessage).isEqualTo("Discount cannot be greater than the price")
            }
        }

    @Test
    fun `given discount smaller than item's total price, when done clicked, then should return Valid state`() =
        testBlocking {
            val item = Order.Item.EMPTY.copy(
                quantity = 2F,
                subtotal = 100F.toBigDecimal(),
                total = 100F.toBigDecimal()
            )
            val productItem = createProductItem(item)
            val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
                productItem,
                "usd"
            ).initSavedStateHandle()

            val sut = createSut(savedStateHandle)

            sut.onDiscountAmountChange(60.toBigDecimal())

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
            val productItem = createProductItem(item)
            val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
                productItem,
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
        val productItem = createProductItem(item)
        val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            productItem,
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
            val result = (this as MultiLiveEvent.Event.ExitWithResult<*>).data as OrderCreationProduct
            assertEquals(99F, result.item.total.toFloat())
        }
    }

    @Test
    fun `given discount amount, when switching to percentage discount, should calculate correct value`() =
        testBlocking {
            val item = Order.Item.EMPTY.copy(
                quantity = 1F,
                subtotal = 999.toBigDecimal(),
            )
            val productItem = createProductItem(item)
            val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
                productItem,
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
    fun `given percentage discount, when switching to discount amount, should calculate correct value`() =
        testBlocking {
            val item = Order.Item.EMPTY.copy(
                quantity = 1F,
                subtotal = 33.toBigDecimal(),
            )
            val productItem = createProductItem(item)
            val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
                productItem,
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
    fun `given discount amount selected, when amount provided, then calculatedPriceAfterDiscount should return discount percentage`() =
        testBlocking {
            val item = Order.Item.EMPTY.copy(
                quantity = 1F,
                subtotal = 33.toBigDecimal(),
            )
            val productItem = createProductItem(item)
            val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
                productItem,
                "usd"
            ).initSavedStateHandle()
            val sut = createSut(savedStateHandle)
            sut.onDiscountAmountChange(4.29.toBigDecimal())
            sut.viewState.test {
                val viewState = awaitItem()
                assertThat(viewState.calculatedPriceAfterDiscount).isEqualTo("13.00")
            }
        }

    @Test
    fun `given discount percentage selected, when discount provided, the calculatedPriceAfterDiscount should return discount amount`() =
        testBlocking {
            val item = Order.Item.EMPTY.copy(
                quantity = 1F,
                subtotal = 33.toBigDecimal(),
            )
            val productItem = createProductItem(item)
            val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
                productItem,
                "usd"
            ).initSavedStateHandle()
            val sut = createSut(savedStateHandle)
            sut.onPercentageDiscountSelected()
            sut.onDiscountAmountChange(13.toBigDecimal())
            sut.viewState.test {
                val viewState = awaitItem()
                assertThat(viewState.calculatedPriceAfterDiscount).isEqualTo("4.29")
            }
        }

    @Test
    fun `given discount amount provided, then show correct price after discount`() = testBlocking {
        val item = Order.Item.EMPTY.copy(
            quantity = 1F,
            subtotal = 33.toBigDecimal(),
        )
        val productItem = createProductItem(item)
        val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            productItem,
            "usd"
        ).initSavedStateHandle()
        val sut = createSut(savedStateHandle)
        sut.onDiscountAmountChange(4.29.toBigDecimal())
        sut.viewState.test {
            val viewState = awaitItem()
            assertThat(viewState.priceAfterDiscount).isEqualTo("28.71")
        }
    }

    @Test
    fun `given initial discount greater than 0, then remove discount button should be visible`() = testBlocking {
        val item = Order.Item.EMPTY.copy(
            quantity = 1F,
            subtotal = 33.toBigDecimal(),
            total = 30.toBigDecimal(),
        )
        val productItem = createProductItem(item)
        val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            productItem,
            "usd"
        ).initSavedStateHandle()
        val sut = createSut(savedStateHandle)
        sut.viewState.test {
            val viewState = awaitItem()
            assertThat(viewState.isRemoveButtonVisible).isTrue()
        }
    }

    @Test
    fun `given initial discount eq to 0, then remove discount button should not be visible`() = testBlocking {
        val item = Order.Item.EMPTY.copy(
            quantity = 1F,
            subtotal = 33.toBigDecimal(),
            total = 33.toBigDecimal(),
        )
        val productItem = createProductItem(item)
        val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            productItem,
            "usd"
        ).initSavedStateHandle()
        val sut = createSut(savedStateHandle)
        sut.viewState.test {
            val viewState = awaitItem()
            assertThat(viewState.isRemoveButtonVisible).isFalse()
        }
    }

    @Test
    fun `given percentage discount, when done clicked, then should track event`() {
        val item = Order.Item.EMPTY.copy(
            quantity = 1F,
            subtotal = 33.toBigDecimal(),
        )
        val productItem = createProductItem(item)
        val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            productItem,
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
        val productItem = createProductItem(item)
        val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            productItem,
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
        val productItem = createProductItem(item)
        val savedStateHandle: SavedStateHandle = OrderCreateEditProductDiscountFragmentArgs(
            productItem,
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
            currencySymbolFinder,
        )
    }

    private fun createOrderItem(withProductId: Long = 123, withVariationId: Long? = null) =
        if (withVariationId != null) {
            Order.Item.EMPTY.copy(
                productId = withProductId,
                itemId = (1L..1000000000L).random(),
                variationId = withVariationId,
                quantity = 1F,
            )
        } else {
            Order.Item.EMPTY.copy(
                productId = withProductId,
                itemId = (1L..1000000000L).random(),
                quantity = 1F,
            )
        }

    private fun createProductItem(item: Order.Item? = null): OrderCreationProduct {
        val orderItem = item ?: createOrderItem()
        val productInfo = ProductInfo(
            imageUrl = "",
            isStockManaged = false,
            stockQuantity = 0.0,
            stockStatus = ProductStockStatus.InStock,
            productType = ProductType.SIMPLE,
            isConfigurable = false,
            pricePreDiscount = "$10",
            priceTotal = "$30",
            priceSubtotal = "$30",
            discountAmount = "$5",
            priceAfterDiscount = "$25",
            hasDiscount = true
        )
        return OrderCreationProduct.ProductItem(
            item = orderItem,
            productInfo = productInfo
        )
    }
}
