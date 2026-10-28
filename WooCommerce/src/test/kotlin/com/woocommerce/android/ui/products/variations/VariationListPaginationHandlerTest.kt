package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class VariationListPaginationHandlerTest : BaseUnitTest() {
    private val repository: VariationRepository = mock()

    private val sut = VariationListPaginationHandler(repository)

    @Test
    fun `when fetching variations, then the first page is requested newest first`() = testBlocking {
        // GIVEN
        givenPageResults(listOf(PAGE_WITH_MORE))

        // WHEN
        sut.fetchVariations(PRODUCT_ID)

        // THEN
        verify(repository).fetchProductVariationsPage(PRODUCT_ID, 0, PAGE_SIZE, VariationListPaging.SORTING)
    }

    @Test
    fun `given the first page has more variations, when loading more, then the next page is requested`() =
        testBlocking {
            // GIVEN
            givenPageResults(listOf(PAGE_WITH_MORE, PAGE_WITH_MORE))
            sut.fetchVariations(PRODUCT_ID)

            // WHEN
            sut.loadMore(PRODUCT_ID)

            // THEN
            assertThat(requestedOffsets()).containsExactly(0, PAGE_SIZE)
        }

    @Test
    fun `given loading more fails, when loading more again, then the failed page is requested again`() =
        testBlocking {
            // GIVEN
            givenPageResults(listOf(PAGE_WITH_MORE, FAILED_PAGE, PAGE_WITH_MORE))
            sut.fetchVariations(PRODUCT_ID)
            sut.loadMore(PRODUCT_ID)

            // WHEN
            sut.loadMore(PRODUCT_ID)

            // THEN
            assertThat(requestedOffsets()).containsExactly(0, PAGE_SIZE, PAGE_SIZE)
            assertThat(sut.canLoadMore).isTrue()
        }

    @Test
    fun `given the last page was loaded, when loading more, then no page is requested`() = testBlocking {
        // GIVEN
        givenPageResults(listOf(LAST_PAGE))
        sut.fetchVariations(PRODUCT_ID)

        // WHEN
        val result = sut.loadMore(PRODUCT_ID)

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(sut.canLoadMore).isFalse()
        assertThat(requestedOffsets()).containsExactly(0)
    }

    @Test
    fun `given more pages were loaded, when fetching variations again, then pagination restarts`() = testBlocking {
        // GIVEN
        givenPageResults(listOf(PAGE_WITH_MORE, PAGE_WITH_MORE, PAGE_WITH_MORE))
        sut.fetchVariations(PRODUCT_ID)
        sut.loadMore(PRODUCT_ID)

        // WHEN
        sut.fetchVariations(PRODUCT_ID)

        // THEN
        assertThat(requestedOffsets()).containsExactly(0, PAGE_SIZE, 0)
    }

    @Test
    fun `given fetching variations fails, when fetching variations, then failure is returned`() = testBlocking {
        // GIVEN
        givenPageResults(listOf(FAILED_PAGE))

        // WHEN
        val result = sut.fetchVariations(PRODUCT_ID)

        // THEN
        assertThat(result.exceptionOrNull()).isEqualTo(FETCH_ERROR)
    }

    @Test
    fun `given refreshing fails after a page was loaded, when loading more, then the next page is requested`() =
        testBlocking {
            // GIVEN
            givenPageResults(listOf(PAGE_WITH_MORE, FAILED_PAGE, PAGE_WITH_MORE))
            sut.fetchVariations(PRODUCT_ID)
            sut.fetchVariations(PRODUCT_ID)

            // WHEN
            sut.loadMore(PRODUCT_ID)

            // THEN
            assertThat(requestedOffsets()).containsExactly(0, 0, PAGE_SIZE)
        }

    private suspend fun givenPageResults(results: List<Result<Boolean>>) {
        whenever(repository.fetchProductVariationsPage(any(), any(), any(), any()))
            .thenReturn(results.first(), *results.drop(1).toTypedArray())
    }

    private suspend fun requestedOffsets(): List<Int> {
        val captor = argumentCaptor<Int>()
        verify(repository, atLeastOnce()).fetchProductVariationsPage(any(), captor.capture(), any(), any())
        return captor.allValues
    }

    private companion object {
        const val PRODUCT_ID = 1L
        const val PAGE_SIZE = VariationListPaging.PAGE_SIZE
        val FETCH_ERROR = Exception()
        val PAGE_WITH_MORE = Result.success(true)
        val LAST_PAGE = Result.success(false)
        val FAILED_PAGE = Result.failure<Boolean>(FETCH_ERROR)
    }
}
