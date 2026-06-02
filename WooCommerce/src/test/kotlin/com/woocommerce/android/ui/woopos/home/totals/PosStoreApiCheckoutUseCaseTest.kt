package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosGetVariationById
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.Header
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pos.PosStoreApiRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pos.PosStoreApiRestClient.CheckoutResponseDto
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore

class PosStoreApiCheckoutUseCaseTest {

    private val site: SiteModel = mock()
    private val restClient: PosStoreApiRestClient = mock()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() } doReturn site
    }
    private val getVariationById: WooPosGetVariationById = mock()
    private val orderStore: WCOrderStore = mock()
    private val orderMapper: OrderMapper = mock()

    private val sut = PosStoreApiCheckoutUseCase(
        restClient,
        selectedSite,
        getVariationById,
        orderStore,
        orderMapper,
    )

    @Test
    fun `given no selected site, when invoked, then returns failure`() = runTest {
        whenever(selectedSite.getOrNull()) doReturn null

        val result = sut(listOf(WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L)))

        assertThat(result.isFailure).isTrue
    }

    @Test
    fun `given simple products, when invoked, then add-item called once per distinct id with summed quantity`() = runTest {
        givenAddToCartSucceeds()
        givenCheckoutSucceeds(orderId = 42L)
        givenOrderFetchSucceeds(orderId = 42L)

        sut(
            listOf(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L),
                WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L),
                WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 2L),
            )
        )

        verify(restClient).addToCart(
            site = eq(site),
            productId = eq(1L),
            quantity = eq(2),
            variation = eq(emptyList()),
            cartToken = anyOrNull(),
        )
        verify(restClient).addToCart(
            site = eq(site),
            productId = eq(2L),
            quantity = eq(1),
            variation = eq(emptyList()),
            cartToken = anyOrNull(),
        )
    }

    @Test
    fun `given coupons in cart, when invoked, then apply-coupon is called once per coupon code`() = runTest {
        givenAddToCartSucceeds()
        givenApplyCouponSucceeds()
        givenCheckoutSucceeds(orderId = 42L)
        givenOrderFetchSucceeds(orderId = 42L)

        sut(
            listOf(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L),
                WooPosItemsViewModel.ItemClickedData.Coupon(id = 10L, couponCode = "SAVE10"),
                WooPosItemsViewModel.ItemClickedData.Coupon(id = 11L, couponCode = "FREESHIP"),
            )
        )

        verify(restClient).applyCoupon(site = eq(site), code = eq("SAVE10"), cartToken = anyOrNull())
        verify(restClient).applyCoupon(site = eq(site), code = eq("FREESHIP"), cartToken = anyOrNull())
    }

    @Test
    fun `given a coupon, when invoked, then it is applied after items and before checkout`() = runTest {
        givenAddToCartSucceeds()
        givenApplyCouponSucceeds()
        givenCheckoutSucceeds(orderId = 42L)
        givenOrderFetchSucceeds(orderId = 42L)

        sut(
            listOf(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L),
                WooPosItemsViewModel.ItemClickedData.Coupon(id = 10L, couponCode = "SAVE10"),
            )
        )

        inOrder(restClient) {
            verify(restClient).addToCart(any(), eq(1L), any(), anyOrNull(), anyOrNull())
            verify(restClient).applyCoupon(eq(site), eq("SAVE10"), anyOrNull())
            verify(restClient).checkout(eq(site), anyOrNull())
        }
    }

    @Test
    fun `given apply-coupon fails, when invoked, then checkout is not called`() = runTest {
        givenAddToCartSucceeds()
        whenever(restClient.applyCoupon(any(), any(), anyOrNull())) doReturn
            WooPayload(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.UNKNOWN, "invalid coupon"))

        val result = sut(
            listOf(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L),
                WooPosItemsViewModel.ItemClickedData.Coupon(id = 10L, couponCode = "BAD"),
            )
        )

        assertThat(result.isFailure).isTrue
        verify(restClient, never()).checkout(any(), anyOrNull())
    }

    @Test
    fun `given no coupons, when invoked, then apply-coupon is never called`() = runTest {
        givenAddToCartSucceeds()
        givenCheckoutSucceeds(orderId = 42L)
        givenOrderFetchSucceeds(orderId = 42L)

        sut(listOf(WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L)))

        verify(restClient, never()).applyCoupon(any(), any(), anyOrNull())
    }

    @Test
    fun `given add-item fails, when invoked, then checkout is not called`() = runTest {
        whenever(restClient.addToCart(any(), any(), any(), any(), anyOrNull())) doReturn
            WooPayload(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.UNKNOWN, "boom"))

        val result = sut(listOf(WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L)))

        assertThat(result.isFailure).isTrue
        verify(restClient, never()).checkout(any(), anyOrNull())
    }

    @Test
    fun `given checkout fails, when invoked, then order fetch is not attempted`() = runTest {
        givenAddToCartSucceeds()
        whenever(restClient.checkout(eq(site), anyOrNull())) doReturn
            WooPayload(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.UNKNOWN, "checkout boom"))

        val result = sut(listOf(WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L)))

        assertThat(result.isFailure).isTrue
        verify(orderStore, never()).fetchSingleOrderSync(any(), any())
    }

    @Test
    fun `given checkout succeeds, when invoked, then returns mapped Order`() = runTest {
        val mappedOrder: Order = mock()
        givenAddToCartSucceeds()
        givenCheckoutSucceeds(orderId = 99L)
        givenOrderFetchSucceeds(orderId = 99L, mapsTo = mappedOrder)

        val result = sut(listOf(WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L)))

        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrNull()).isSameAs(mappedOrder)
    }

    @Test
    fun `given first add-item returns cart token, when invoked, then it is replayed on subsequent calls and checkout`() = runTest {
        // First add-item returns Cart-Token in response headers; subsequent add-item
        // returns the same (or a refreshed) token; checkout should be called with the
        // latest captured token.
        whenever(
            restClient.addToCart(
                site = eq(site),
                productId = eq(1L),
                quantity = any(),
                variation = anyOrNull(),
                cartToken = anyOrNull(),
            )
        ) doReturn WooPayload(Unit, listOf(Header("Cart-Token", "token-from-first")))

        whenever(
            restClient.addToCart(
                site = eq(site),
                productId = eq(2L),
                quantity = any(),
                variation = anyOrNull(),
                cartToken = anyOrNull(),
            )
        ) doReturn WooPayload(Unit, listOf(Header("Cart-Token", "token-from-second")))

        givenCheckoutSucceeds(orderId = 7L, cartTokenHeaderValue = "token-from-second")
        givenOrderFetchSucceeds(orderId = 7L)

        sut(
            listOf(
                WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L),
                WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 2L),
            )
        )

        inOrder(restClient) {
            // First add-item: no token to send yet.
            verify(restClient).addToCart(
                site = eq(site),
                productId = eq(1L),
                quantity = eq(1),
                variation = eq(emptyList()),
                cartToken = eq(null),
            )
            // Second add-item: replays the token captured from the first response.
            verify(restClient).addToCart(
                site = eq(site),
                productId = eq(2L),
                quantity = eq(1),
                variation = eq(emptyList()),
                cartToken = eq("token-from-first"),
            )
            // Checkout: replays the latest captured token (from the second response).
            verify(restClient).checkout(eq(site), eq("token-from-second"))
        }
    }

    private suspend fun givenAddToCartSucceeds() {
        whenever(restClient.addToCart(any(), any(), any(), any(), anyOrNull())) doReturn WooPayload(Unit)
    }

    private suspend fun givenApplyCouponSucceeds() {
        whenever(restClient.applyCoupon(any(), any(), anyOrNull())) doReturn WooPayload(Unit)
    }

    private suspend fun givenCheckoutSucceeds(
        orderId: Long,
        cartTokenHeaderValue: String? = null,
    ) {
        val headers = if (cartTokenHeaderValue != null) {
            listOf(Header("Cart-Token", cartTokenHeaderValue))
        } else {
            emptyList()
        }
        whenever(restClient.checkout(eq(site), anyOrNull())) doReturn
            WooPayload(checkoutResponse(orderId), headers)
    }

    private fun checkoutResponse(orderId: Long) =
        CheckoutResponseDto(orderId = orderId, status = "pending", orderKey = "wc_order_$orderId")

    private suspend fun givenOrderFetchSucceeds(orderId: Long, mapsTo: Order = mock()) {
        val entity: OrderEntity = mock()
        whenever(orderStore.fetchSingleOrderSync(site, orderId)) doReturn WooResult(entity)
        whenever(orderMapper.toAppModel(entity)) doReturn mapsTo
    }
}
