package com.woocommerce.android.ui.products.variations

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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.ProductVariationsPage

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
    fun `given loading more fails, when loading more again, then the failed page is requested again`() = testBlocking {
        // GIVEN
        givenFetchResults(SUCCESS_WITH_MORE, ERROR, SUCCESS_WITH_MORE)
        sut.fetchProductVariations(PRODUCT_ID)
        sut.fetchProductVariations(PRODUCT_ID, loadMore = true)

        // WHEN
        sut.fetchProductVariations(PRODUCT_ID, loadMore = true)

        // THEN
        assertThat(requestedOffsets()).containsExactly(0, PAGE_SIZE, PAGE_SIZE)
    }

    @Test
    fun `given loading more succeeds, when loading more again, then the next page is requested`() = testBlocking {
        // GIVEN
        givenFetchResults(SUCCESS_WITH_MORE, SUCCESS_WITH_MORE, SUCCESS_WITH_MORE)
        sut.fetchProductVariations(PRODUCT_ID)
        sut.fetchProductVariations(PRODUCT_ID, loadMore = true)

        // WHEN
        sut.fetchProductVariations(PRODUCT_ID, loadMore = true)

        // THEN
        assertThat(requestedOffsets()).containsExactly(0, PAGE_SIZE, PAGE_SIZE * 2)
    }

    @Test
    fun `given fetch fails, when fetching variations, then failure is returned`() = testBlocking {
        // GIVEN
        givenFetchResults(ERROR)

        // WHEN
        val result = sut.fetchProductVariations(PRODUCT_ID)

        // THEN
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given a page keeps failing, when getting all variations, then loading stops`() = testBlocking {
        // GIVEN
        givenFetchResults(ERROR)

        // WHEN
        sut.getAllVariations(PRODUCT_ID)

        // THEN
        verify(productStore, times(1)).fetchProductVariations(any<FetchProductVariationsPayload>())
    }

    private suspend fun givenFetchResults(
        first: WooResult<ProductVariationsPage>,
        vararg next: WooResult<ProductVariationsPage>
    ) {
        whenever(productStore.fetchProductVariations(any<FetchProductVariationsPayload>())).thenReturn(first, *next)
        whenever(productStore.getVariationsForProduct(any(), any())).thenReturn(emptyList())
    }

    private suspend fun requestedOffsets(): List<Int> {
        val captor = argumentCaptor<FetchProductVariationsPayload>()
        verify(productStore, atLeastOnce()).fetchProductVariations(captor.capture())
        return captor.allValues.map { it.offset }
    }

    private companion object {
        const val PRODUCT_ID = 1L
        const val PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE
        val SUCCESS_WITH_MORE = WooResult(ProductVariationsPage(variations = emptyList(), canLoadMore = true))
        val ERROR = WooResult<ProductVariationsPage>(
            WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.SERVER_ERROR)
        )
    }
}
