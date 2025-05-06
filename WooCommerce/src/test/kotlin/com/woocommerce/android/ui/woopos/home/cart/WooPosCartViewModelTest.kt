package com.woocommerce.android.ui.woopos.home.cart

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosGetCouponById
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.common.data.WooPosGetVariationById
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
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ItemAddedToCart.WooPosItemSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ItemAddedToCart.WooPosItemSource.Companion.toAnalyticsString
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTrackingDataKeeper
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatCouponSummary
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
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
    private val formatCouponSummary: WooPosFormatCouponSummary = mock {
        on { invoke(any(), any()) }
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
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                source = WooPosItemSource.PRODUCT_LIST
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
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Variation(
                    id = variation.remoteVariationId,
                    productId = variation.remoteProductId
                ),
                source = WooPosItemSource.PRODUCT_LIST
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
                ParentToChildrenEvent.ItemClickedInProductSelector(
                    WooPosItemsViewModel.ItemClickedData.Product.Simple(
                        id = product.remoteId
                    ),
                    source = WooPosItemSource.PRODUCT_LIST
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
    fun `given items in cart, when item remove button clicked in cart, then should track envent`() = runTest {
        // GIVEN
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        val sut = createSut()

        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                source = WooPosItemSource.PRODUCT_LIST
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
        verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.ItemRemovedFromCart)
    }

    @Test
    fun `given items in cart, when checkout tapped, then should track event`() = runTest {
        // GIVEN
        val (sut, states) = createSutWithItemsInCart()
        assertThat(states.last().body).isInstanceOf(WooPosCartState.Body.WithItems::class.java)

        // WHEN
        val itemsInCartCount = states.last().body.amountOfItems
        sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)

        // THEN
        verify(analyticsTracker).track(
            argThat { event ->
                event is CheckoutTapped && event.properties["items_in_cart"] == "$itemsInCartCount"
            }
        )
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
                ParentToChildrenEvent.ItemClickedInProductSelector(
                    WooPosItemsViewModel.ItemClickedData.Product.Simple(
                        id = product.remoteId
                    ),
                    source = WooPosItemSource.PRODUCT_LIST
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
                ParentToChildrenEvent.ItemClickedInProductSelector(
                    WooPosItemsViewModel.ItemClickedData.Product.Simple(
                        id = product1.remoteId
                    ),
                    source = WooPosItemSource.PRODUCT_LIST
                )
            )
            parentToChildrenMutableSharedFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(
                    WooPosItemsViewModel.ItemClickedData.Product.Simple(
                        id = product2.remoteId
                    ),
                    source = WooPosItemSource.PRODUCT_LIST
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
                ParentToChildrenEvent.ItemClickedInProductSelector(
                    WooPosItemsViewModel.ItemClickedData.Product.Simple(
                        id = product3.remoteId
                    ),
                    source = WooPosItemSource.PRODUCT_LIST
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
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                source = WooPosItemSource.PRODUCT_LIST
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
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                source = WooPosItemSource.PRODUCT_LIST
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
        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                source = WooPosItemSource.PRODUCT_LIST
            )
        )

        // THEN
        verify(analyticsTracker)
            .track(
                WooPosAnalyticsEvent.Event.ItemAddedToCart(source = toAnalyticsString(WooPosItemSource.PRODUCT_LIST))
            )
    }

    @Test
    fun `when coupon added to cart, then should track analytics event`() = runTest {
        // GIVEN
        val sut = createSut()
        sut.state.captureValues()

        // WHEN
        simulateCouponClicked()

        // THEN
        verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.ItemAddedToCart(source = "coupons"))
    }

    @Test
    fun `when simple product added to cart, then should track analytics event with product type simple`() = runTest {
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
        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                source = WooPosItemSource.PRODUCT_LIST
            )
        )

        // THEN
        verify(analyticsTracker).track(
            argThat {
                this == WooPosAnalyticsEvent.Event.ItemAddedToCart() &&
                    (
                        this as WooPosAnalyticsEvent.Event.ItemAddedToCart
                        ).properties[WooPosAnalyticsEventConstant.PRODUCT_TYPE] == "simple"
            }
        )
    }

    @Test
    fun `when variation added to cart, then should track analytics event with product type variation`() = runTest {
        // GIVEN
        val variation = ProductTestUtils.generateProductVariation(
            productId = 23L,
            amount = "10.0",
        )
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        whenever(
            getVariationsById(any(), any())
        ).thenReturn(variation)
        whenever(getProductById(any())).thenReturn(product)
        val sut = createSut()
        sut.state.captureValues()

        // WHEN
        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Variation(
                    id = variation.remoteProductId,
                    productId = variation.remoteProductId
                ),
                source = WooPosItemSource.PRODUCT_LIST
            )
        )

        // THEN
        verify(analyticsTracker).track(
            argThat {
                this == WooPosAnalyticsEvent.Event.ItemAddedToCart() &&
                    (
                        this as WooPosAnalyticsEvent.Event.ItemAddedToCart
                        ).properties[WooPosAnalyticsEventConstant.PRODUCT_TYPE] == "variation"
            }
        )
    }

    @Test
    fun `when coupon added to cart, then should track analytics event with item type coupon`() = runTest {
        // GIVEN
        val sut = createSut()
        sut.state.captureValues()

        // WHEN
        simulateCouponClicked()

        // THEN
        verify(analyticsTracker).track(
            argThat {
                this == WooPosAnalyticsEvent.Event.ItemAddedToCart(source = "coupons") &&
                    (
                        this as WooPosAnalyticsEvent.Event.ItemAddedToCart
                        ).properties[WooPosAnalyticsEventConstant.PRODUCT_TYPE] == "coupon"
            }
        )
    }

    @Test
    fun `given cart has coupons, when coupons validation fails, then coupons validation state is INVALID`() = runTest {
        // GIVEN
        val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
        val sut = createSut()
        val states = sut.state.captureValues()
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Coupon(id = 1L)
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
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Coupon(id = 1L)
            )
        )
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Coupon(id = 2L)
            )
        )

        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")
        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                )
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
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Coupon(id = 2L)
            )
        )

        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")
        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                )
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
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Coupon(id = 1L)
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
        assertThat(state.isCheckoutButtonVisible).isFalse()
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
        assertThat(finalState.isCheckoutButtonVisible).isFalse()
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
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                source = WooPosItemSource.PRODUCT_LIST
            )
        )

        // THEN
        val finalState = states.last()
        assertThat(finalState.isCheckoutButtonVisible).isTrue()
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
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                source = WooPosItemSource.PRODUCT_LIST
            )
        )

        // THEN
        val finalState = states.last()
        assertThat(finalState.isCheckoutButtonVisible).isTrue()
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
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                source = WooPosItemSource.PRODUCT_LIST
            )
        )

        // WHEN
        val productToRemove = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
            .filterIsInstance<WooPosCartItemViewState.Product.Simple>().first()
        sut.onUIEvent(WooPosCartUIEvent.ItemRemovedFromCart(productToRemove))

        // THEN
        val finalState = states.last()
        assertThat(finalState.isCheckoutButtonVisible).isFalse()
    }

    private suspend fun simulateCouponClicked(couponId: Long = 1L) {
        parentToChildrenMutableSharedFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(
                itemData = WooPosItemsViewModel.ItemClickedData.Coupon(id = couponId),
                source = WooPosItemSource.COUPON_LIST
            ),

        )
    }

    @Test
    fun `given cart with products, when navigated to checkout, then checkout button not visible`() = runTest {
        // GIVEN
        val (sut, states) = createSutWithItemsInCart()
        assertThat(states.last().isCheckoutButtonVisible).isTrue()

        // WHEN
        sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)

        // THEN
        val finalState = states.last()
        assertThat(finalState.isCheckoutButtonVisible).isFalse()
    }

    @Test
    fun `given CHECKOUT status with products, when back clicked, then checkout button visible`() = runTest {
        // GIVEN
        val (sut, states) = createSutWithItemsInCart()
        sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)
        assertThat(states.last().isCheckoutButtonVisible).isFalse()

        // WHEN
        sut.onUIEvent(WooPosCartUIEvent.BackClicked)

        // THEN
        val finalState = states.last()
        assertThat(finalState.isCheckoutButtonVisible).isTrue()
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
            ParentToChildrenEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(
                    id = product.remoteId
                ),
                source = WooPosItemSource.PRODUCT_LIST
            )
        )
        return Pair(sut, states)
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
            savedState
        )
    }
}
