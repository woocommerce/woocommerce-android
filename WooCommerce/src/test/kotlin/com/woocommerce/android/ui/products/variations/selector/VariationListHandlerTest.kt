package com.woocommerce.android.ui.products.variations.selector

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCProductStore.VariationSorting

@OptIn(ExperimentalCoroutinesApi::class)
class VariationListHandlerTest : BaseUnitTest() {
    private val repository: VariationSelectorRepository = mock()

    private val sut = VariationListHandler(repository)

    @Test
    fun `given default sorting and page size, when fetching variations, then the first page is sorted by menu order`() =
        testBlocking {
            // GIVEN
            givenPageResults(listOf(LAST_PAGE))

            // WHEN
            sut.fetchVariations(PRODUCT_ID, forceRefresh = true)

            // THEN
            verify(repository).fetchVariations(
                productId = eq(PRODUCT_ID),
                offset = eq(0),
                pageSize = eq(DEFAULT_PAGE_SIZE),
                filterOptions = anyOrNull(),
                orderCurrency = anyOrNull(),
                sorting = eq(VariationSorting.MENU_ORDER)
            )
        }

    @Test
    fun `given newest first paging, when fetching variations, then the first page uses that paging`() =
        testBlocking {
            // GIVEN
            givenPageResults(listOf(LAST_PAGE))

            // WHEN
            sut.fetchVariations(
                PRODUCT_ID,
                forceRefresh = true,
                sorting = VariationSorting.DATE_DESC,
                pageSize = PAGE_SIZE
            )

            // THEN
            verify(repository).fetchVariations(
                productId = eq(PRODUCT_ID),
                offset = eq(0),
                pageSize = eq(PAGE_SIZE),
                filterOptions = anyOrNull(),
                orderCurrency = anyOrNull(),
                sorting = eq(VariationSorting.DATE_DESC)
            )
        }

    @Test
    fun `given newest first paging and more pages, when loading more, then the next page keeps that paging`() =
        testBlocking {
            // GIVEN
            givenPageResults(listOf(PAGE_WITH_MORE, LAST_PAGE))
            sut.fetchVariations(
                PRODUCT_ID,
                forceRefresh = true,
                sorting = VariationSorting.DATE_DESC,
                pageSize = PAGE_SIZE
            )

            // WHEN
            sut.loadMore(PRODUCT_ID)

            // THEN
            verify(repository).fetchVariations(
                productId = eq(PRODUCT_ID),
                offset = eq(PAGE_SIZE),
                pageSize = eq(PAGE_SIZE),
                filterOptions = anyOrNull(),
                orderCurrency = anyOrNull(),
                sorting = eq(VariationSorting.DATE_DESC)
            )
        }

    @Test
    fun `given the last page was loaded, when loading more, then no page is requested`() = testBlocking {
        // GIVEN
        givenPageResults(listOf(LAST_PAGE))
        sut.fetchVariations(PRODUCT_ID, forceRefresh = true)

        // WHEN
        sut.loadMore(PRODUCT_ID)

        // THEN
        assertThat(requestedOffsets()).containsExactly(0)
    }

    @Test
    fun `given loading more fails, when loading more again, then the failed page is requested again`() =
        testBlocking {
            // GIVEN
            givenPageResults(listOf(PAGE_WITH_MORE, FAILED_PAGE, PAGE_WITH_MORE))
            sut.fetchVariations(PRODUCT_ID, forceRefresh = true, pageSize = PAGE_SIZE)
            sut.loadMore(PRODUCT_ID)

            // WHEN
            sut.loadMore(PRODUCT_ID)

            // THEN
            assertThat(requestedOffsets()).containsExactly(0, PAGE_SIZE, PAGE_SIZE)
        }

    @Test
    fun `given a page size, when checking if more can be loaded, then the page size is used`() = testBlocking {
        // GIVEN
        sut.fetchVariations(PRODUCT_ID, pageSize = PAGE_SIZE)

        // WHEN
        val canLoadMoreBelowPageSize = sut.canLoadMore(PAGE_SIZE)
        val canLoadMoreAbovePageSize = sut.canLoadMore(PAGE_SIZE + 1)

        // THEN
        assertThat(canLoadMoreBelowPageSize).isFalse()
        assertThat(canLoadMoreAbovePageSize).isTrue()
    }

    private suspend fun givenPageResults(results: List<Result<Boolean>>) {
        whenever(repository.fetchVariations(any(), any(), any(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(results.first(), *results.drop(1).toTypedArray())
    }

    private suspend fun requestedOffsets(): List<Int> {
        val captor = argumentCaptor<Int>()
        verify(repository, atLeastOnce()).fetchVariations(
            any(),
            captor.capture(),
            any(),
            anyOrNull(),
            anyOrNull(),
            any()
        )
        return captor.allValues
    }

    private companion object {
        const val PRODUCT_ID = 1L
        const val PAGE_SIZE = 100
        const val DEFAULT_PAGE_SIZE = 25
        val PAGE_WITH_MORE = Result.success(true)
        val LAST_PAGE = Result.success(false)
        val FAILED_PAGE = Result.failure<Boolean>(Exception())
    }
}
