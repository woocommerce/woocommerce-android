package com.woocommerce.android.ui.products.categories.selector

import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProductCategoryListHandlerTests : BaseUnitTest() {
    private lateinit var listHandler: ProductCategoryListHandler

    private val repository: ProductCategorySelectorRepository = mock {
        on { observeCategories() } doReturn flowOf(emptyList())
        onBlocking { fetchCategories(any(), any()) } doReturn Result.success(false)
    }

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        listHandler = ProductCategoryListHandler(
            repository = repository
        )
    }

    @Test
    fun `when fetching results, then return repository results as a tree`() = testBlocking {
        val productCategories = ProductTestUtils.generateProductCategories()
        setup {
            whenever(repository.observeCategories()).thenReturn(flowOf(productCategories))
        }

        val results = listHandler.categories.runAndCaptureValues {
            listHandler.fetchCategories()
        }.last()

        assertThat(results).isEqualTo(productCategories.convertToTree())
    }

    @Test
    fun `when searching for results results, then return search results as a flat tree`() = testBlocking {
        val productCategories = ProductTestUtils.generateProductCategories()

        setup {
            whenever(repository.searchCategories(any(), any(), any()))
                .thenReturn(Result.success(ProductCategorySelectorRepository.SearchResult(productCategories, false)))
        }

        val results = listHandler.categories.runAndCaptureValues {
            listHandler.fetchCategories(searchQuery = "query")
        }.last()

        assertThat(results).isEqualTo(productCategories.convertToFlatTree())
    }

    @Test
    fun `when force fetching, then refresh results`() = testBlocking {
        setup()

        listHandler.fetchCategories(forceRefresh = true)

        verify(repository).fetchCategories(eq(0), any())
    }

    @Test
    fun `given search is not active, when we load more, then load next page`() = testBlocking {
        setup {
            whenever(
                repository.fetchCategories(
                    0,
                    ProductCategoryListHandler.PAGE_SIZE
                )
            ).thenReturn(Result.success(true))
        }

        listHandler.fetchCategories(forceRefresh = true)
        listHandler.loadMore()

        verify(repository).fetchCategories(0, ProductCategoryListHandler.PAGE_SIZE)
        verify(repository).fetchCategories(ProductCategoryListHandler.PAGE_SIZE, ProductCategoryListHandler.PAGE_SIZE)
    }

    @Test
    fun `given search is active, when we load more, then search for next page`() = testBlocking {
        val searchQuery = "query"
        setup {
            whenever(repository.searchCategories(eq(searchQuery), any(), eq(ProductCategoryListHandler.PAGE_SIZE)))
                .thenReturn(
                    Result.success(
                        ProductCategorySelectorRepository.SearchResult(
                            List(ProductCategoryListHandler.PAGE_SIZE) {
                                ProductCategory(name = "Category")
                            },
                            canLoadMore = true
                        )
                    )
                )
        }

        listHandler.fetchCategories(searchQuery = "query")
        listHandler.loadMore()

        verify(repository).searchCategories("query", 0, ProductCategoryListHandler.PAGE_SIZE)
        verify(repository).searchCategories(
            "query",
            ProductCategoryListHandler.PAGE_SIZE,
            ProductCategoryListHandler.PAGE_SIZE
        )
    }
}
