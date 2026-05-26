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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
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
        whenever(restClient.addToCart(any(), any(), any(), any())) doReturn WooPayload(Unit)
        whenever(restClient.checkout(site)) doReturn WooPayload(checkoutResponse(orderId = 42L))
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
            variation = eq(emptyList())
        )
        verify(restClient).addToCart(
            site = eq(site),
            productId = eq(2L),
            quantity = eq(1),
            variation = eq(emptyList())
        )
    }

    @Test
    fun `given add-item fails, when invoked, then checkout is not called`() = runTest {
        whenever(restClient.addToCart(any(), any(), any(), any())) doReturn
            WooPayload(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.UNKNOWN, "boom"))

        val result = sut(listOf(WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L)))

        assertThat(result.isFailure).isTrue
        verify(restClient, never()).checkout(any())
    }

    @Test
    fun `given checkout fails, when invoked, then order fetch is not attempted`() = runTest {
        whenever(restClient.addToCart(any(), any(), any(), any())) doReturn WooPayload(Unit)
        whenever(restClient.checkout(site)) doReturn
            WooPayload(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.UNKNOWN, "checkout boom"))

        val result = sut(listOf(WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L)))

        assertThat(result.isFailure).isTrue
        verify(orderStore, never()).fetchSingleOrderSync(any(), any())
    }

    @Test
    fun `given checkout succeeds, when invoked, then returns mapped Order`() = runTest {
        val mappedOrder: Order = mock()
        whenever(restClient.addToCart(any(), any(), any(), any())) doReturn WooPayload(Unit)
        whenever(restClient.checkout(site)) doReturn WooPayload(checkoutResponse(orderId = 99L))
        givenOrderFetchSucceeds(orderId = 99L, mapsTo = mappedOrder)

        val result = sut(listOf(WooPosItemsViewModel.ItemClickedData.Product.Simple(id = 1L)))

        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrNull()).isSameAs(mappedOrder)
    }

    private fun checkoutResponse(orderId: Long) =
        CheckoutResponseDto(orderId = orderId, status = "pending", orderKey = "wc_order_$orderId")

    private suspend fun givenOrderFetchSucceeds(orderId: Long, mapsTo: Order = mock()) {
        val entity: OrderEntity = mock()
        whenever(orderStore.fetchSingleOrderSync(site, orderId)) doReturn WooResult(entity)
        whenever(orderMapper.toAppModel(entity)) doReturn mapsTo
    }
}
