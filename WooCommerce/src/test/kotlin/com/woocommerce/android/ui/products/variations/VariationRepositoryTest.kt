package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.ProductVariationsPage
import org.wordpress.android.fluxc.store.WCProductStore.VariationSorting

@OptIn(ExperimentalCoroutinesApi::class)
class VariationRepositoryTest : BaseUnitTest() {
    private val productStore: WCProductStore = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }

    private val sut = VariationRepository(
        productStore = productStore,
        wooCommerceStore = mock(),
        selectedSite = selectedSite,
        dispatchers = coroutinesTestRule.testDispatchers,
    )

    @Test
    fun `when fetching a page, then the requested page is fetched and whether more can be loaded is returned`() =
        testBlocking {
            // GIVEN
            givenFetchResults(SUCCESS_WITH_MORE)

            // WHEN
            val result = sut.fetchProductVariationsPage(
                remoteProductId = PRODUCT_ID,
                offset = 50,
                pageSize = 10,
                sorting = VariationSorting.MENU_ORDER
            )

            // THEN
            val payload = requestedPayloads().single()
            assertThat(payload.remoteProductId).isEqualTo(PRODUCT_ID)
            assertThat(payload.offset).isEqualTo(50)
            assertThat(payload.pageSize).isEqualTo(10)
            assertThat(payload.sorting).isEqualTo(VariationSorting.MENU_ORDER)
            assertThat(result.getOrThrow()).isTrue()
        }

    @Test
    fun `given fetch fails, when fetching a page, then failure is returned`() = testBlocking {
        // GIVEN
        givenFetchResults(ERROR)

        // WHEN
        val result = sut.fetchProductVariationsPage(PRODUCT_ID, 0, PAGE_SIZE, VariationSorting.MENU_ORDER)

        // THEN
        assertThat(result.exceptionOrNull()).isInstanceOf(WooException::class.java)
    }

    @Test
    fun `given a page keeps failing, when getting all variations, then loading stops`() = testBlocking {
        // GIVEN
        givenFetchResults(ERROR)

        // WHEN
        val result = sut.getAllVariations(PRODUCT_ID)

        // THEN
        verify(productStore, times(1)).fetchProductVariations(any<FetchProductVariationsPayload>())
        assertThat(result.exceptionOrNull()).isInstanceOf(WooException::class.java)
    }

    @Test
    fun `when getting all variations, then every page is requested from the start`() =
        testBlocking {
            // GIVEN
            givenFetchResults(SUCCESS_WITH_MORE, SUCCESS_WITH_MORE, SUCCESS_LAST_PAGE)

            // WHEN
            val result = sut.getAllVariations(PRODUCT_ID)

            // THEN
            assertThat(requestedOffsets()).containsExactly(0, PAGE_SIZE, PAGE_SIZE * 2)
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `given all pages succeed, when getting all variations, then variations from the database are returned`() =
        testBlocking {
            // GIVEN
            givenFetchResults(SUCCESS_WITH_MORE, SUCCESS_LAST_PAGE)
            val storedVariations = listOf(1L, 2L).map {
                WCProductVariationModel(remoteProductId = RemoteId(PRODUCT_ID), remoteVariationId = RemoteId(it))
            }
            whenever(productStore.getVariationsForProduct(any(), any())).thenReturn(storedVariations)

            // WHEN
            val result = sut.getAllVariations(PRODUCT_ID)

            // THEN
            assertThat(result.getOrThrow().map { it.remoteVariationId }).containsExactlyInAnyOrder(1L, 2L)
        }

    @Test
    fun `given variations were already fetched, when getting all variations, then it starts from the first page`() =
        testBlocking {
            // GIVEN
            givenFetchResults(SUCCESS_LAST_PAGE, SUCCESS_LAST_PAGE)
            sut.getAllVariations(PRODUCT_ID)

            // WHEN
            sut.getAllVariations(PRODUCT_ID)

            // THEN
            assertThat(requestedOffsets()).containsExactly(0, 0)
        }

    @Test
    fun `when getting all variations, then pages are sorted by id`() = testBlocking {
        // GIVEN
        givenFetchResults(SUCCESS_WITH_MORE, SUCCESS_LAST_PAGE)

        // WHEN
        sut.getAllVariations(PRODUCT_ID)

        // THEN
        assertThat(requestedPayloads().map { it.sorting }).containsOnly(VariationSorting.ID)
    }

    @Test
    fun `given a middle page fails, when getting all variations, then failure is returned`() = testBlocking {
        // GIVEN
        givenFetchResults(SUCCESS_WITH_MORE, ERROR)

        // WHEN
        val result = sut.getAllVariations(PRODUCT_ID)

        // THEN
        assertThat(requestedOffsets()).containsExactly(0, PAGE_SIZE)
        assertThat(result.exceptionOrNull()).isInstanceOf(WooException::class.java)
    }

    private suspend fun givenFetchResults(
        first: WooResult<ProductVariationsPage>,
        vararg next: WooResult<ProductVariationsPage>
    ) {
        whenever(productStore.fetchProductVariations(any<FetchProductVariationsPayload>())).thenReturn(first, *next)
        whenever(productStore.getVariationsForProduct(any(), any())).thenReturn(emptyList())
    }

    private suspend fun requestedOffsets(): List<Int> = requestedPayloads().map { it.offset }

    private suspend fun requestedPayloads(): List<FetchProductVariationsPayload> {
        val captor = argumentCaptor<FetchProductVariationsPayload>()
        verify(productStore, atLeastOnce()).fetchProductVariations(captor.capture())
        return captor.allValues
    }

    private companion object {
        const val PRODUCT_ID = 1L
        const val PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE
        val SUCCESS_WITH_MORE = WooResult(ProductVariationsPage(variations = emptyList(), canLoadMore = true))
        val SUCCESS_LAST_PAGE = WooResult(ProductVariationsPage(variations = emptyList(), canLoadMore = false))
        val ERROR = WooResult<ProductVariationsPage>(
            WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.SERVER_ERROR)
        )
    }
}
