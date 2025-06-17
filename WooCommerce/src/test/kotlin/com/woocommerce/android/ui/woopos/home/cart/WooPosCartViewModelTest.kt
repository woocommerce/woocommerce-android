package com.woocommerce.android.ui.woopos.home.cart

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosGetCouponById
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.common.data.WooPosGetVariationById
import com.woocommerce.android.ui.woopos.common.data.searchbyidentifier.WooPosSearchByIdentifier
import com.woocommerce.android.ui.woopos.common.data.searchbyidentifier.WooPosSearchByIdentifierResult
import com.woocommerce.android.ui.woopos.common.util.WooPosSoundHelper
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState.Coupon.CouponValidationState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosGetCachedStoreCurrency
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BackToCartTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.CheckoutTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ClearCartTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.InteractionWithCustomerStarted
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTrackingDataKeeper
import com.woocommerce.android.ui.woopos.util.format.WooPosCouponsFormatter
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosCartViewModelTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val parentToChildrenMutableSharedFlow = MutableSharedFlow<ParentToChildrenEvent>()
    private val soundHelper: WooPosSoundHelper = mock()
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
        on { events }.thenReturn(parentToChildrenMutableSharedFlow)
    }
    private val getProductById: WooPosGetProductById = mock()
    private val getCouponById: WooPosGetCouponById = mock {
        onBlocking { invoke(any()) }.thenReturn(
            Coupon(
                1L,
                "coupon_code",
                productIds = emptyList(),
                categoryIds = emptyList(),
                restrictions = mock()
            )
        )
    }
    private val formatCouponSummary: WooPosCouponsFormatter = mock {
        on { formatSummary(any(), any()) }
            .thenReturn("100% off everything")
    }

    private val getCachedStoreCurrency: WooPosGetCachedStoreCurrency = mock {
        onBlocking { invoke() }.thenReturn("USD")
    }

    private val getVariationsById: WooPosGetVariationById = mock()
    private val resourceProvider: ResourceProvider = mock {
        on {
            getQuantityString(
                quantity = eq(1),
                default = eq(R.string.woopos_items_in_cart_multiple),
                zero = eq(R.string.woopos_items_in_cart_multiple),
                one = eq(R.string.woopos_items_in_cart),
            )
        }.thenReturn("Item in cart: 1")
    }
    private val formatPrice: WooPosFormatPrice = mock {
        onBlocking { invoke(eq(BigDecimal("10.0"))) }.thenReturn("10.0$")
    }

    private val analyticsTracker: WooPosAnalyticsTracker = mock()

    private val savedState: SavedStateHandle = SavedStateHandle()
    private val trackerData: WooPosAnalyticsTrackingDataKeeper = WooPosAnalyticsTrackingDataKeeper()
    private val cartItemsUpdater: WooPosCartItemsUpdater = mock()
    private val searchByIdentifier: WooPosSearchByIdentifier = mock()

    @Test
    fun `given empty cart, when product clicked in product selector, then should add product to cart`() = runTest {
        // GIVEN
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        val sut = createSut()
        val states = sut.state.captureValues()

        // WHEN
        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                eventForTracking = mock()
            )
        )

        // THEN
        val itemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(itemsInCart).hasSize(1)
        assertThat((itemsInCart.first() as WooPosCartItemViewState.Product.Simple).id).isEqualTo(product.remoteId)
    }

    @Test
    fun `given empty cart, when variation clicked, then should add variation to cart`() = runTest {
        // GIVEN
        val variation = ProductTestUtils.generateProductVariation(
            productId = 23L,
            variationId = 24L,
            amount = "10.0"
        )
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        whenever(
            getVariationsById(eq(variation.remoteProductId), eq(variation.remoteVariationId))
        ).thenReturn(variation)
        whenever(getProductById(any())).thenReturn(product)
        val sut = createSut()
        val states = sut.state.captureValues()

        // WHEN
        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                WooPosItemsViewModel.ItemClickedData.Product.Variation(
                    id = variation.remoteVariationId,
                    productId = variation.remoteProductId
                ),
                eventForTracking = mock()
            )
        )

        // THEN
        val itemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(itemsInCart).hasSize(1)
        assertThat((itemsInCart.first() as WooPosCartItemViewState.Product.Variation).variationId)
            .isEqualTo(variation.remoteVariationId)
    }

    @Test
    fun `given empty cart, when coupon clicked, then should add coupon to cart`() = runTest {
        // GIVEN
        val sut = createSut()
        val states = sut.state.captureValues()

        // WHEN
        simulateCouponClicked()

        // THEN
        val itemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(itemsInCart).hasSize(1)
    }

    @Test
    fun `given product in cart, when product remove button clicked in cart, then should remove product from cart`() =
        runTest {
            // GIVEN
            val product = ProductTestUtils.generateProduct(
                productId = 23L,
                productName = "title",
                amount = "10.0"
            ).copy(firstImageUrl = "url")

            val parentToChildrenMutableSharedFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenMutableSharedFlow)
            whenever(getProductById(eq(product.remoteId))).thenReturn(product)
            val sut = createSut()
            val states = sut.state.captureValues()

            parentToChildrenMutableSharedFlow.emit(
                ParentToChildrenEvent.ItemClickedInItemsList(
                    WooPosItemsViewModel.ItemClickedData.Product.Simple(
                        id = product.remoteId
                    ),
                    eventForTracking = mock()
                )
            )

            // WHEN
            sut.onUIEvent(
                WooPosCartUIEvent.ItemRemovedFromCart(
                    WooPosCartItemViewState.Product.Simple(
                        itemNumber = 1,
                        id = product.remoteId,
                        name = product.name,
                        price = "10.0$",
                        imageUrl = product.firstImageUrl,
                        description = null,
                    )
                )
            )

            // THEN
            val itemsInCartAfterRemoveClicked =
                (states.last().body as? WooPosCartState.Body.WithItems)?.itemsInCart ?: emptyList()
            assertThat(itemsInCartAfterRemoveClicked).isEmpty()
        }

    @Test
    fun `given coupon in cart, when coupon remove button clicked in cart, then should remove coupon from cart`() =
        runTest {
            // GIVEN
            val sut = createSut()
            val states = sut.state.captureValues()

            simulateCouponClicked()

            // WHEN
            sut.onUIEvent(
                WooPosCartUIEvent.ItemRemovedFromCart(
                    WooPosCartItemViewState.Coupon(
                        id = 1L,
                        itemNumber = 1,
                        name = "coupon_code",
                        summary = "100% off everything",
                        validationState = CouponValidationState.Unknown,
                    )
                )
            )

            // THEN
            val itemsInCartAfterRemoveClicked =
                (states.last().body as? WooPosCartState.Body.WithItems)?.itemsInCart ?: emptyList()
            assertThat(itemsInCartAfterRemoveClicked).isEmpty()
        }

    @Test
    fun `given items in cart, when item remove button clicked in cart, then should track event`() = runTest {
        // GIVEN
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        val sut = createSut()

        val item = WooPosCartItemViewState.Product.Simple(
            itemNumber = 1,
            id = product.remoteId,
            name = product.name,
            price = "10.0$",
            imageUrl = product.firstImageUrl,
            description = null,
        )

        val itemClickedData = WooPosItemsViewModel.ItemClickedData.Product.Simple(
            id = item.id
        )
        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                itemClickedData,
                eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                    item = itemClickedData,
                    source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                    sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST,
                )
            )
        )

        // WHEN

        sut.onUIEvent(
            WooPosCartUIEvent.ItemRemovedFromCart(item = item)
        )

        // THEN
        verify(analyticsTracker).track(
            eq(
                WooPosAnalyticsEvent.Event.ItemRemovedFromCart(
                    source = WooPosAnalyticsEventConstant.CartSource.CART,
                    item = item
                )
            )
        )
    }

    @Test
    fun `given items in cart, when checkout tapped, then should track event`() = runTest {
        // GIVEN
        val (sut, states) = createSutWithItemsInCart()
        assertThat(states.last().body).isInstanceOf(WooPosCartState.Body.WithItems::class.java)
        simulateCouponClicked()

        // WHEN
        clearInvocations(analyticsTracker)
        sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)
        advanceUntilIdle()

        // THEN
        verify(analyticsTracker).track(CheckoutTapped(productsInCart = 1, couponsInCart = 1))
    }

    @Test
    fun `when back button tapped, then should track event`() = runTest {
        // GIVEN
        val (sut, states) = createSutWithItemsInCart()
        sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)
        assertThat(states.last().cartStatus).isEqualTo(WooPosCartStatus.CHECKOUT)

        // WHEN
        sut.onUIEvent(WooPosCartUIEvent.BackClicked)

        // THEN
        verify(analyticsTracker).track(BackToCartTapped)
    }

    @Test
    fun `given empty cart in_progress, when vm created, then toolbar state should contain shopping cart empty itemsCart and no clear all button`() =
        runTest {
            // WHEN
            val sut = createSut()
            val states = sut.state.captureValues()

            // THEN
            val toolbar = states.last().toolbar
            assertThat(toolbar.backIconVisible).isFalse()
            assertThat(toolbar.itemsCount).isNull()
            assertThat(toolbar.isClearAllButtonVisible).isFalse()
        }

    @Test
    fun `given non empty cart in_progress, when vm created, then toolbar state should contain shopping cart itemsCart title and no clear all`() =
        runTest {
            // GIVEN
            val product = ProductTestUtils.generateProduct(
                productId = 23L,
                productName = "title",
                amount = "10.0"
            ).copy(firstImageUrl = "url")

            val parentToChildrenMutableSharedFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenMutableSharedFlow)
            whenever(getProductById(eq(product.remoteId))).thenReturn(product)

            // WHEN
            val sut = createSut()
            val states = sut.state.captureValues()

            parentToChildrenMutableSharedFlow.emit(
                ParentToChildrenEvent.ItemClickedInItemsList(
                    WooPosItemsViewModel.ItemClickedData.Product.Simple(
                        id = product.remoteId
                    ),
                    eventForTracking = mock()
                )
            )

            // THEN
            val toolbar = states.last().toolbar
            assertThat(toolbar.backIconVisible).isFalse()
            assertThat(toolbar.itemsCount).isEqualTo("Item in cart: 1")
            assertThat(toolbar.isClearAllButtonVisible).isTrue()
        }

    @Test
    fun `given non empty cart checkout, when vm created, then toolbar state should contain back icon itemsCart title and no clear all`() =
        runTest {
            // GIVEN
            val (sut, states) = createSutWithItemsInCart()

            sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)

            // THEN
            val toolbar = states.last().toolbar
            assertThat(toolbar.backIconVisible).isTrue()
            assertThat(toolbar.itemsCount).isEqualTo("Item in cart: 1")
            assertThat(toolbar.isClearAllButtonVisible).isFalse()
        }

    @Suppress("LongMethod")
    @Test
    fun `given non empty cart in process, when 2 items added and the first removed and third item added, then third will have item number 2`() =
        runTest {
            // GIVEN
            val product1 = ProductTestUtils.generateProduct(
                productId = 1L,
                productName = "title",
                amount = "10.0"
            ).copy(firstImageUrl = "url")
            val product2 = ProductTestUtils.generateProduct(
                productId = 2L,
                productName = "title",
                amount = "10.0"
            ).copy(firstImageUrl = "url")
            val product3 = ProductTestUtils.generateProduct(
                productId = 3L,
                productName = "title",
                amount = "10.0"
            ).copy(firstImageUrl = "url")

            val parentToChildrenMutableSharedFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenMutableSharedFlow)
            whenever(getProductById(eq(product1.remoteId))).thenReturn(product1)
            whenever(getProductById(eq(product2.remoteId))).thenReturn(product2)
            whenever(getProductById(eq(product3.remoteId))).thenReturn(product3)

            val sut = createSut()
            val states = sut.state.captureValues()

            // WHEN
            parentToChildrenMutableSharedFlow.emit(
                ParentToChildrenEvent.ItemClickedInItemsList(
                    WooPosItemsViewModel.ItemClickedData.Product.Simple(
                        id = product1.remoteId
                    ),
                    eventForTracking = mock()
                )
            )
            parentToChildrenMutableSharedFlow.emit(
                ParentToChildrenEvent.ItemClickedInItemsList(
                    WooPosItemsViewModel.ItemClickedData.Product.Simple(
                        id = product2.remoteId
                    ),
                    eventForTracking = mock()
                )
            )

            sut.onUIEvent(
                WooPosCartUIEvent.ItemRemovedFromCart(
                    WooPosCartItemViewState.Product.Simple(
                        itemNumber = 1,
                        id = product1.remoteId,
                        name = product1.name,
                        price = "10.0$",
                        imageUrl = product1.firstImageUrl,
                        description = null,
                    )
                )
            )

            parentToChildrenMutableSharedFlow.emit(
                ParentToChildrenEvent.ItemClickedInItemsList(
                    WooPosItemsViewModel.ItemClickedData.Product.Simple(
                        id = product3.remoteId
                    ),
                    eventForTracking = mock()
                )
            )

            // THEN
            val itemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
            assertThat(itemsInCart).hasSize(2)
            assertThat(itemsInCart[0].itemNumber).isEqualTo(3)
            assertThat(itemsInCart[1].itemNumber).isEqualTo(2)
        }

    @Test
    fun `given empty cart, when created, then state should be empty`() = runTest {
        // WHEN
        val sut = createSut()
        val states = sut.state.captureValues()

        // THEN
        assertThat(states).hasSize(1)
        assertThat(states.last().body).isInstanceOf(WooPosCartState.Body.Empty::class.java)
        assertThat(states.last().cartStatus).isEqualTo(WooPosCartStatus.EMPTY)
    }

    @Test
    fun `given empty cart, when product tapped, then should track start of customer interaction event`() = runTest {
        // GIVEN
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        createSut()

        // WHEN
        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                eventForTracking = mock()
            )
        )

        // THEN
        verify(analyticsTracker).track(InteractionWithCustomerStarted)
    }

    @Test
    fun `given empty cart, when coupon tapped, then should track start of customer interaction event`() = runTest {
        // GIVEN
        createSut()

        // WHEN
        simulateCouponClicked()

        // THEN
        verify(analyticsTracker).track(InteractionWithCustomerStarted)
    }

    @Test
    fun `given non-empty cart, when all items removed, then state should be empty`() = runTest {
        // GIVEN
        val (sut, states) = createSutWithItemsInCart()

        // WHEN
        sut.onUIEvent(WooPosCartUIEvent.ClearAllClicked)

        // THEN
        assertThat(states).hasSizeGreaterThan(1)
        val finalState = states.last()
        assertThat(finalState.body).isInstanceOf(WooPosCartState.Body.Empty::class.java)
        assertThat(finalState.cartStatus).isEqualTo(WooPosCartStatus.EMPTY)
    }

    @Test
    fun `given non-empty cart, when all items removed, then should track event`() = runTest {
        // GIVEN
        val (sut, _) = createSutWithItemsInCart()

        // WHEN
        sut.onUIEvent(WooPosCartUIEvent.ClearAllClicked)

        // THEN
        verify(analyticsTracker).track(ClearCartTapped)
    }

    @Test
    fun `given non-empty cart, when all items removed individually, then state should be empty`() = runTest {
        // GIVEN
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        val sut = createSut()
        val states = sut.state.captureValues()

        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                eventForTracking = mock()
            )
        )

        // WHEN
        sut.onUIEvent(
            WooPosCartUIEvent.ItemRemovedFromCart(
                WooPosCartItemViewState.Product.Simple(
                    itemNumber = 1,
                    id = product.remoteId,
                    name = product.name,
                    price = "10.0$",
                    imageUrl = product.firstImageUrl,
                    description = null,
                )
            )
        )

        // THEN
        assertThat(states).hasSizeGreaterThan(1)
        val finalState = states.last()
        assertThat(finalState.body).isInstanceOf(WooPosCartState.Body.Empty::class.java)
        assertThat(finalState.cartStatus).isEqualTo(WooPosCartStatus.EMPTY)
    }

    @Test
    fun `given non-empty cart, when vm initialized and all items removed, then toolbar state should reflect empty cart`() =
        runTest {
            // GIVEN
            val (sut, states) = createSutWithItemsInCart()

            // WHEN
            sut.onUIEvent(WooPosCartUIEvent.ClearAllClicked)

            // THEN
            val toolbar = states.last().toolbar
            assertThat(toolbar.backIconVisible).isFalse()
            assertThat(toolbar.itemsCount).isNull()
            assertThat(toolbar.isClearAllButtonVisible).isFalse()
        }

    @Test
    fun `when item added to cart, then should track analytics event`() = runTest {
        // GIVEN
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        val sut = createSut()
        sut.state.captureValues()

        // WHEN
        val itemAddedToCartEvent = mock<WooPosAnalyticsEvent.Event.ItemAddedToCart>()
        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                eventForTracking = itemAddedToCartEvent
            )
        )

        // THEN
        verify(analyticsTracker).track(itemAddedToCartEvent)
    }

    @Test
    fun `given cart has coupons, when coupons validation fails, then coupons validation state is INVALID`() = runTest {
        // GIVEN
        val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
        val sut = createSut()
        val states = sut.state.captureValues()
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                itemData = WooPosItemsViewModel.ItemClickedData.Coupon(id = 1L, couponCode = ""),
                eventForTracking = mock(),
            )
        )
        sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)

        // WHEN
        parentToChildrenEventsMutableFlow.emit(ParentToChildrenEvent.CouponsValidationFailed)

        // THEN
        val itemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(itemsInCart).hasSize(1)
        assertThat((itemsInCart.first() as WooPosCartItemViewState.Coupon).validationState)
            .isEqualTo(CouponValidationState.Invalid)
    }

    @Test
    fun `when remove coupons event received, then should remove all coupons from cart`() = runTest {
        // GIVEN
        val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
        val sut = createSut()
        val states = sut.state.captureValues()
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                itemData = WooPosItemsViewModel.ItemClickedData.Coupon(id = 1L, couponCode = ""),
                eventForTracking = mock()
            )
        )
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                itemData = WooPosItemsViewModel.ItemClickedData.Coupon(id = 2L, couponCode = ""),
                eventForTracking = mock()
            )
        )

        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")
        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                itemData = WooPosItemsViewModel.ItemClickedData.Product.Simple(id = product.remoteId),
                eventForTracking = mock()
            )
        )
        sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)

        assertThat((states.last().body as WooPosCartState.Body.WithItems).itemsInCart).hasSize(3)

        // WHEN
        parentToChildrenEventsMutableFlow.emit(ParentToChildrenEvent.RemoveCouponsClicked)

        // THEN
        val finalItemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(finalItemsInCart).hasSize(1)
        assertThat(finalItemsInCart.first()).isInstanceOf(WooPosCartItemViewState.Product.Simple::class.java)

        verify(childrenToParentEventSender).sendToParent(any<ChildToParentEvent.CouponsRemoved>())
    }

    @Test
    fun `when remove coupons event received, then emits coupons removed event`() = runTest {
        // GIVEN
        val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
        createSut()
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                itemData = WooPosItemsViewModel.ItemClickedData.Coupon(id = 2L, couponCode = ""),
                eventForTracking = mock()
            )
        )

        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")
        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                itemData = WooPosItemsViewModel.ItemClickedData.Product.Simple(id = product.remoteId),
                eventForTracking = mock()
            )
        )

        // WHEN
        parentToChildrenEventsMutableFlow.emit(ParentToChildrenEvent.RemoveCouponsClicked)

        // THEN
        verify(childrenToParentEventSender).sendToParent(any<ChildToParentEvent.CouponsRemoved>())
    }

    @Test
    fun `when back from checkout to cart, then coupon validation states should be reset to UNKNOWN`() = runTest {
        // GIVEN
        val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
        val sut = createSut()
        val states = sut.state.captureValues()
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                itemData = WooPosItemsViewModel.ItemClickedData.Coupon(id = 1L, couponCode = ""),
                eventForTracking = mock()
            )
        )
        sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)

        // WHEN
        parentToChildrenEventsMutableFlow.emit(ParentToChildrenEvent.BackFromCheckoutToCartClicked)

        // THEN
        val finalItemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(finalItemsInCart).hasSize(1)
        assertThat((finalItemsInCart.first() as WooPosCartItemViewState.Coupon).validationState)
            .isEqualTo(CouponValidationState.Unknown)
    }

    @Test
    fun `given empty cart, then button should not be visible`() = runTest {
        // WHEN
        val sut = createSut()
        val states = sut.state.captureValues()

        // THEN
        val state = states.last()
        assertThat(state.checkoutButtonState).isEqualTo(WooPosCartState.CheckoutButtonState.Invisible)
    }

    @Test
    fun `given empty, when coupon added to cart, then checkout button should not be visible`() = runTest {
        // GIVEN
        val sut = createSut()
        val states = sut.state.captureValues()

        // WHEN
        simulateCouponClicked()

        // THEN
        val finalState = states.last()
        assertThat(finalState.checkoutButtonState).isEqualTo(WooPosCartState.CheckoutButtonState.Invisible)
    }

    @Test
    fun `given empty cart, when product added to cart, then checkout button should be visible`() = runTest {
        // GIVEN
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")
        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        val sut = createSut()
        val states = sut.state.captureValues()

        // WHEN
        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                eventForTracking = mock()
            )
        )

        // THEN
        val finalState = states.last()
        assertThat(finalState.checkoutButtonState).isEqualTo(WooPosCartState.CheckoutButtonState.Enabled)
    }

    @Test
    fun `given empty cart, when coupon and product added to cart, then checkout button should be visible`() = runTest {
        // GIVEN
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        val sut = createSut()
        val states = sut.state.captureValues()

        // WHEN
        simulateCouponClicked()

        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                eventForTracking = mock()
            )
        )

        // THEN
        val finalState = states.last()
        assertThat(finalState.checkoutButtonState).isEqualTo(WooPosCartState.CheckoutButtonState.Enabled)
    }

    @Test
    fun `given cart with products and coupon, when products removed, then checkout button disappears`() = runTest {
        // GIVEN
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        val sut = createSut()
        val states = sut.state.captureValues()
        simulateCouponClicked()
        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                eventForTracking = mock()
            )
        )

        // WHEN
        val productToRemove = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
            .filterIsInstance<WooPosCartItemViewState.Product.Simple>().first()
        sut.onUIEvent(WooPosCartUIEvent.ItemRemovedFromCart(productToRemove))

        // THEN
        val finalState = states.last()
        assertThat(finalState.checkoutButtonState).isEqualTo(WooPosCartState.CheckoutButtonState.Invisible)
    }

    @Test
    fun `given cart with products, when navigated to checkout, then checkout button not visible`() = runTest {
        // GIVEN
        val (sut, states) = createSutWithItemsInCart()
        assertThat(states.last().checkoutButtonState).isEqualTo(WooPosCartState.CheckoutButtonState.Enabled)

        // WHEN
        sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)

        // THEN
        val finalState = states.last()
        assertThat(finalState.checkoutButtonState).isEqualTo(WooPosCartState.CheckoutButtonState.Invisible)
    }

    @Test
    fun `given CHECKOUT status with products, when back clicked, then checkout button visible`() = runTest {
        // GIVEN
        val (sut, states) = createSutWithItemsInCart()
        sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)
        assertThat(states.last().checkoutButtonState).isEqualTo(WooPosCartState.CheckoutButtonState.Invisible)

        // WHEN
        sut.onUIEvent(WooPosCartUIEvent.BackClicked)

        // THEN
        val finalState = states.last()
        assertThat(finalState.checkoutButtonState).isEqualTo(WooPosCartState.CheckoutButtonState.Enabled)
    }

    @Test
    fun `given cart with products, when coupon added, then coupon added to the beginning`() = runTest {
        // GIVEN
        val sut = createSut()
        val states = sut.state.captureValues()
        simulateProductClicked(23L)

        // WHEN
        simulateCouponClicked()

        // THEN
        val itemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(itemsInCart).hasSize(2)
        assertThat(itemsInCart[0]).isInstanceOf(WooPosCartItemViewState.Coupon::class.java)
        assertThat(itemsInCart[1]).isInstanceOf(WooPosCartItemViewState.Product.Simple::class.java)
    }

    @Test
    fun `given cart with coupons, when new coupon added, then new coupon added to the beginning`() =
        runTest {
            // GIVEN
            val sut = createSut()
            val states = sut.state.captureValues()

            simulateCouponClicked(couponId = 1L)

            // WHEN
            simulateCouponClicked(couponId = 2L)

            // THEN
            val itemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
            assertThat(itemsInCart).hasSize(2)
            assertThat(itemsInCart[0]).isInstanceOf(WooPosCartItemViewState.Coupon::class.java)
            assertThat(itemsInCart[1]).isInstanceOf(WooPosCartItemViewState.Coupon::class.java)
            assertThat((itemsInCart[0] as WooPosCartItemViewState.Coupon).id).isEqualTo(2L)
            assertThat((itemsInCart[1] as WooPosCartItemViewState.Coupon).id).isEqualTo(1L)
        }

    @Test
    fun `given cart with coupons and products, when new product added, then added after coupons before products`() =
        runTest {
            // GIVEN
            val sut = createSut()
            val states = sut.state.captureValues()

            simulateCouponClicked()
            simulateProductClicked(999L)

            // WHEN
            simulateProductClicked(1L)

            // THEN
            val itemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
            assertThat(itemsInCart).hasSize(3)
            assertThat(itemsInCart[0]).isInstanceOf(WooPosCartItemViewState.Coupon::class.java)
            assertThat(itemsInCart[1]).isInstanceOf(WooPosCartItemViewState.Product.Simple::class.java)
            assertThat(itemsInCart[2]).isInstanceOf(WooPosCartItemViewState.Product.Simple::class.java)
            assertThat((itemsInCart[1] as WooPosCartItemViewState.Product.Simple).id).isEqualTo(1L)
            assertThat((itemsInCart[2] as WooPosCartItemViewState.Product.Simple).id).isEqualTo(999L)
        }

    @Test
    fun `given coupon in cart, when same coupon clicked again, then coupon not duplicated`() = runTest {
        // GIVEN
        val sut = createSut()
        val states = sut.state.captureValues()

        simulateCouponClicked(couponId = 1L)

        // WHEN
        simulateCouponClicked(couponId = 1L)

        // THEN
        val itemsAfterSecondAdd = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(itemsAfterSecondAdd).hasSize(1)
        assertThat(itemsAfterSecondAdd[0]).isInstanceOf(WooPosCartItemViewState.Coupon::class.java)
        assertThat((itemsAfterSecondAdd[0] as WooPosCartItemViewState.Coupon).id).isEqualTo(1L)
    }

    @Test
    fun `given empty cart, when barcode scanned, then loading item is added to cart`() = runTest {
        // GIVEN
        whenever(
            searchByIdentifier(any(), any())
        ).doSuspendableAnswer {
            delay(1)
            WooPosSearchByIdentifierResult.Success(
                ProductTestUtils.generateProduct(
                    amount = "10.0"
                )
            )
        }
        val sut = createSut()
        val states = sut.state.captureValues()

        // WHEN
        sut.onUIEvent(WooPosCartUIEvent.OnBarcodeScanned("123456789"))

        // THEN
        val itemsInCart = (states[1].body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(itemsInCart).hasSize(1)
        assertThat(itemsInCart.first()).isInstanceOf(WooPosCartItemViewState.Loading::class.java)
        assertThat((itemsInCart.first() as WooPosCartItemViewState.Loading).name).isEqualTo("123456789")
    }

    @Test
    fun `when barcode scanned, then item added to cart event tracked`() = runTest {
        // GIVEN
        whenever(
            searchByIdentifier(any(), any())
        ).doSuspendableAnswer {
            delay(1)
            WooPosSearchByIdentifierResult.Success(
                ProductTestUtils.generateProduct(
                    amount = "10.0"
                )
            )
        }
        val sut = createSut()

        // WHEN
        sut.onUIEvent(WooPosCartUIEvent.OnBarcodeScanned("123456789"))

        // THEN
        verify(analyticsTracker).track(
            eq(
                WooPosAnalyticsEvent.Event.ItemAddedToCart(
                    item = WooPosCartItemViewState.Loading(1, "123456789")
                )
            )
        )
    }

    @Test
    fun `given empty cart, when barcode scanned and product found, then loading item is replaced with product`() =
        runTest {
            // GIVEN
            val product = ProductTestUtils.generateProduct(
                productId = 23L,
                productName = "Scanned Product",
                amount = "10.0"
            ).copy(firstImageUrl = "url")

            whenever(searchByIdentifier(eq("123456789"), any())).thenReturn(
                WooPosSearchByIdentifierResult.Success(product)
            )

            val sut = createSut()
            val states = sut.state.captureValues()

            // WHEN
            sut.onUIEvent(WooPosCartUIEvent.OnBarcodeScanned("123456789"))
            advanceUntilIdle()

            // THEN
            val finalItemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
            assertThat(finalItemsInCart).hasSize(1)
            assertThat(finalItemsInCart.first()).isInstanceOf(WooPosCartItemViewState.Product.Simple::class.java)
            val productItem = finalItemsInCart.first() as WooPosCartItemViewState.Product.Simple
            assertThat(productItem.id).isEqualTo(product.remoteId)
            assertThat(productItem.name).isEqualTo(product.name)
        }

    @Test
    fun `when barcode scanned and product found, then track item added to cart event`() =
        runTest {
            // GIVEN
            val product = ProductTestUtils.generateProduct(
                productId = 23L,
                productName = "Scanned Product",
                amount = "10.0"
            ).copy(firstImageUrl = "url")

            whenever(searchByIdentifier(eq("123456789"), any())).thenReturn(
                WooPosSearchByIdentifierResult.Success(product)
            )

            val sut = createSut()

            // WHEN
            sut.onUIEvent(WooPosCartUIEvent.OnBarcodeScanned("123456789"))
            advanceUntilIdle()

            // THEN
            verify(analyticsTracker).track(
                eq(
                    WooPosAnalyticsEvent.Event.ItemAddedToCart(
                        item = WooPosCartItemViewState.Product.Simple(
                            itemNumber = 1,
                            id = product.remoteId,
                            name = product.name,
                            price = "$10.0",
                            imageUrl = product.firstImageUrl,
                            description = product.description,
                        )
                    )
                )
            )
        }

    @Test
    fun `given empty cart, when barcode scanned and product not found, then loading item is replaced with error`() =
        runTest {
            // GIVEN
            val errorMessage = "Product not found"
            whenever(resourceProvider.getString(R.string.woopos_cart_barcode_scan_result_product_not_found))
                .thenReturn(errorMessage)

            whenever(searchByIdentifier(eq("123456789"), any())).thenReturn(
                WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound)
            )

            val sut = createSut()
            val states = sut.state.captureValues()

            // WHEN
            sut.onUIEvent(WooPosCartUIEvent.OnBarcodeScanned("123456789"))
            advanceUntilIdle()

            // THEN
            val finalItemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
            assertThat(finalItemsInCart).hasSize(1)
            assertThat(finalItemsInCart.first()).isInstanceOf(WooPosCartItemViewState.Error::class.java)
            val errorItem = finalItemsInCart.first() as WooPosCartItemViewState.Error
            assertThat(errorItem.name).isEqualTo("123456789")
            assertThat(errorItem.message).isEqualTo(errorMessage)
        }

    @Test
    fun `given barcode scanned, when error occurs, then failure sound played`() =
        runTest {
            // GIVEN
            val errorMessage = "Unknown error"
            whenever(resourceProvider.getString(R.string.woopos_cart_barcode_scan_result_product_not_found))
                .thenReturn(errorMessage)

            whenever(searchByIdentifier(eq("123456789"), any())).thenReturn(
                WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.UnknownError(""))
            )

            val sut = createSut()

            // WHEN
            sut.onUIEvent(WooPosCartUIEvent.OnBarcodeScanned("123456789"))
            advanceUntilIdle()

            // THEN
            verify(soundHelper).playBarcodeScanFailure()
        }

    @Test
    fun `given cart with items, when barcode scanned and product found, then product is added to existing items`() =
        runTest {
            // GIVEN
            val existingProduct = ProductTestUtils.generateProduct(
                productId = 23L,
                productName = "Existing Product",
                amount = "10.0"
            ).copy(firstImageUrl = "url")

            val scannedProduct = ProductTestUtils.generateProduct(
                productId = 42L,
                productName = "Scanned Product",
                amount = "10.0"
            ).copy(firstImageUrl = "url2")

            whenever(getProductById(eq(existingProduct.remoteId))).thenReturn(existingProduct)
            whenever(searchByIdentifier(eq("123456789"), any())).thenReturn(
                WooPosSearchByIdentifierResult.Success(scannedProduct)
            )

            val sut = createSut()
            val states = sut.state.captureValues()

            parentToChildrenMutableSharedFlow.emit(
                ParentToChildrenEvent.ItemClickedInItemsList(
                    WooPosItemsViewModel.ItemClickedData.Product.Simple(id = existingProduct.remoteId),
                    eventForTracking = mock()
                )
            )

            // WHEN
            sut.onUIEvent(WooPosCartUIEvent.OnBarcodeScanned("123456789"))
            advanceUntilIdle()

            // THEN
            val finalItemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
            assertThat(finalItemsInCart).hasSize(2)

            val scannedItem = finalItemsInCart[0] as WooPosCartItemViewState.Product.Simple
            assertThat(scannedItem.id).isEqualTo(scannedProduct.remoteId)

            val existingItem = finalItemsInCart[1] as WooPosCartItemViewState.Product.Simple
            assertThat(existingItem.id).isEqualTo(existingProduct.remoteId)
        }

    @Test
    fun `given cart with items, when barcode scan fails with network error, then error message is correct`() = runTest {
        // GIVEN
        val errorMessage = "Network error occurred"
        whenever(resourceProvider.getString(R.string.woopos_cart_barcode_scan_result_network_error))
            .thenReturn(errorMessage)

        whenever(searchByIdentifier(eq("123456789"), any())).thenReturn(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NetworkError)
        )

        val sut = createSut()
        val states = sut.state.captureValues()

        // WHEN
        sut.onUIEvent(WooPosCartUIEvent.OnBarcodeScanned("123456789"))
        advanceUntilIdle()

        // THEN
        val finalItemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(finalItemsInCart).hasSize(1)
        val errorItem = finalItemsInCart.first() as WooPosCartItemViewState.Error
        assertThat(errorItem.message).isEqualTo(errorMessage)
    }

    @Test
    fun `when barcode scan fails, then item added to cart tracked`() = runTest {
        // GIVEN
        val errorMessage = "Network error occurred"
        whenever(resourceProvider.getString(R.string.woopos_cart_barcode_scan_result_network_error))
            .thenReturn(errorMessage)

        whenever(searchByIdentifier(eq("123456789"), any())).thenReturn(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NetworkError)
        )

        val sut = createSut()

        // WHEN
        sut.onUIEvent(WooPosCartUIEvent.OnBarcodeScanned("123456789"))
        advanceUntilIdle()

        // THEN
        verify(analyticsTracker).track(
            eq(
                WooPosAnalyticsEvent.Event.ItemAddedToCart(
                    item = WooPosCartItemViewState.Error(
                        itemNumber = 1,
                        name = "123456789",
                        message = "Network error occurred",
                    )
                )
            )
        )
    }

    @Test
    fun `given barcode scanned, when variation found, then variation added to cart`() = runTest {
        // GIVEN
        val productId = 100L
        val variationId = 200L
        val variation: ProductVariation = mock {
            on { remoteVariationId }.thenReturn(variationId)
            on { remoteProductId }.thenReturn(productId)
            on { getName() }.thenReturn("Red Hoodie - Size L")
            on { price }.thenReturn(BigDecimal("45.0"))
            on { stockStatus }.thenReturn(ProductStockStatus.InStock)
            on { image }.thenReturn(null)
            on { attributes }.thenReturn(emptyArray())
        }

        val product = ProductTestUtils.generateProduct(
            productId = productId,
            productName = "Red Hoodie",
            amount = "45.0"
        )

        whenever(searchByIdentifier(eq("VAR123456"), any())).thenReturn(
            WooPosSearchByIdentifierResult.VariationSuccess(variation, product)
        )
        whenever(formatPrice(eq(BigDecimal("45.0")))).thenReturn("45.0$")
        whenever(getProductById(eq(productId))).thenReturn(product)

        val sut = createSut()
        val states = sut.state.captureValues()

        // WHEN
        sut.onUIEvent(WooPosCartUIEvent.OnBarcodeScanned("VAR123456"))
        advanceUntilIdle()

        // THEN
        val itemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(itemsInCart).hasSize(1)
        assertThat(itemsInCart.first()).isInstanceOf(WooPosCartItemViewState.Product.Variation::class.java)
        val variationItem = itemsInCart.first() as WooPosCartItemViewState.Product.Variation
        assertThat(variationItem.variationId).isEqualTo(variationId)
        assertThat(variationItem.name).isEqualTo("Red Hoodie")
    }

    private suspend fun createSutWithItemsInCart(): Pair<WooPosCartViewModel, List<WooPosCartState>> {
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        val sut = createSut()
        val states = sut.state.captureValues()
        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                eventForTracking = mock()
            )
        )
        return Pair(sut, states)
    }

    private suspend fun simulateCouponClicked(couponId: Long = 1L) {
        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                itemData = WooPosItemsViewModel.ItemClickedData.Coupon(id = couponId, couponCode = ""),
                eventForTracking = mock()
            ),
        )
    }

    private suspend fun simulateProductClicked(productId: Long = 1L) {
        val product = ProductTestUtils.generateProduct(
            productId = productId,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        whenever(getProductById(eq(product.remoteId))).thenReturn(product)

        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInItemsList(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = productId
                ),
                eventForTracking = mock()
            )
        )
    }

    private fun createSut(): WooPosCartViewModel {
        return WooPosCartViewModel(
            childrenToParentEventSender,
            parentToChildrenEventReceiver,
            getProductById,
            getCouponById,
            formatCouponSummary,
            getVariationsById,
            resourceProvider,
            formatPrice,
            analyticsTracker,
            trackerData,
            cartItemsUpdater,
            getCachedStoreCurrency,
            searchByIdentifier,
            soundHelper,
            savedState
        )
    }
}
