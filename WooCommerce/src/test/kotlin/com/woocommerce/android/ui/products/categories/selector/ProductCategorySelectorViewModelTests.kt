package com.woocommerce.android.ui.products.categories.selector

import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.categories.selector.ProductCategorySelectorViewModel.LoadingState
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProductCategorySelectorViewModelTests : BaseUnitTest() {
    private lateinit var viewModel: ProductCategorySelectorViewModel

    private val categoriesStateFlow = MutableStateFlow(emptyList<ProductCategoryTreeItem>())

    private val productCategoryListHandler: ProductCategoryListHandler = mock {
        on { categories } doReturn categoriesStateFlow
    }

    private val defaultProductCategoriesList = ProductTestUtils.generateProductCategories().convertToTree()

    suspend fun setup(
        selectedCategories: LongArray = longArrayOf(),
        prepareMocks: suspend () -> Unit = {}
    ) {
        prepareMocks()
        viewModel = ProductCategorySelectorViewModel(
            savedState = ProductCategorySelectorFragmentArgs(selectedCategories).toSavedStateHandle(),
            listHandler = productCategoryListHandler
        )
    }

    @Test
    fun `when screen is loaded, then fetch product categories`() = testBlocking {
        setup {
            whenever(productCategoryListHandler.fetchCategories(true)).doSuspendableAnswer {
                delay(1L)
                return@doSuspendableAnswer Result.success(Unit)
            }
        }

        val fetchingState = viewModel.viewState.captureValues().last()

        verify(productCategoryListHandler).fetchCategories(forceRefresh = true)
        assertThat(fetchingState.loadingState).isEqualTo(LoadingState.Loading)

        advanceUntilIdle()
        val idleState = viewModel.viewState.captureValues().last()
        assertThat(idleState.loadingState).isEqualTo(LoadingState.Idle)
    }

    @Test
    fun `when screen is loaded, then load saved categories`() = testBlocking {
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            categoriesStateFlow.value = defaultProductCategoriesList
            advanceUntilIdle()
        }.last()

        assertThat(state.categories.map { it.id })
            .isEqualTo(defaultProductCategoriesList.map { it.productCategory.remoteCategoryId })
        assertThat(state.loadingState).isEqualTo(LoadingState.Idle)
    }

    @Test
    fun `when search query is entered, then search for categories`() = testBlocking {
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onSearchQueryChanged("Search")
            advanceUntilIdle()
        }.last()

        assertThat(state.searchQuery).isEqualTo("Search")
        verify(productCategoryListHandler).fetchCategories(searchQuery = "Search")
    }

    @Test
    fun `when load more is requested, then load next page`() = testBlocking {
        setup()

        viewModel.onLoadMore()

        verify(productCategoryListHandler).loadMore()
    }

    @Test
    fun `when fetching coupons fails, then show an error`() = testBlocking {
        setup {
            whenever(productCategoryListHandler.fetchCategories(forceRefresh = true)).thenReturn(
                Result.failure(
                    Exception()
                )
            )
        }

        val event = viewModel.event.captureValues().filterIsInstance<MultiLiveEvent.Event.ShowSnackbar>().last()

        assertThat(event.message).isEqualTo(R.string.product_category_selector_loading_failed)
    }

    @Test
    fun `when searching coupons fails, then show an error`() = testBlocking {
        setup {
            whenever(
                productCategoryListHandler.fetchCategories(searchQuery = "Search")
            ).thenReturn(Result.failure(Exception()))
        }

        viewModel.onSearchQueryChanged("Search")
        advanceUntilIdle()
        val event = viewModel.event.captureValues().filterIsInstance<MultiLiveEvent.Event.ShowSnackbar>().last()

        assertThat(event.message).isEqualTo(R.string.product_category_selector_search_failed)
    }

    @Test
    fun `when loading next page, then show an error`() = testBlocking {
        setup {
            whenever(productCategoryListHandler.loadMore()).thenReturn(Result.failure(Exception()))
        }

        viewModel.onLoadMore()
        advanceUntilIdle()
        val event = viewModel.event.captureValues().filterIsInstance<MultiLiveEvent.Event.ShowSnackbar>().last()

        assertThat(event.message).isEqualTo(R.string.product_category_selector_loading_failed)
    }
}
