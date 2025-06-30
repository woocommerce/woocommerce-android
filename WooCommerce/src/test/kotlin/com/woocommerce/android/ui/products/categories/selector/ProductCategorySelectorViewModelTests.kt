package com.woocommerce.android.ui.products.categories.selector

import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.categories.selector.ProductCategorySelectorViewModel.LoadingState
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@ExperimentalCoroutinesApi
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
        // GIVEN
        setup {
            whenever(productCategoryListHandler.fetchCategories(true)).doSuspendableAnswer {
                delay(1L)
                return@doSuspendableAnswer Result.success(Unit)
            }
        }

        // WHEN
        val fetchingState = viewModel.viewState.captureValues().last()

        verify(productCategoryListHandler).fetchCategories(forceRefresh = true)
        assertThat(fetchingState.loadingState).isEqualTo(LoadingState.Loading)

        advanceUntilIdle()

        // THEN
        val idleState = viewModel.viewState.captureValues().last()
        assertThat(idleState.loadingState).isEqualTo(LoadingState.Idle)
    }

    @Test
    fun `when screen is loaded, then load saved categories`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val state = viewModel.viewState.runAndCaptureValues {
            categoriesStateFlow.value = defaultProductCategoriesList
            advanceUntilIdle()
        }.last()

        // THEN
        assertThat(state.categories.map { it.id })
            .isEqualTo(defaultProductCategoriesList.map { it.productCategory.remoteCategoryId })
        assertThat(state.loadingState).isEqualTo(LoadingState.Idle)
    }

    @Test
    fun `when search query is entered, then search for categories`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onSearchQueryChanged("Search")
            advanceUntilIdle()
        }.last()

        // THEN
        assertThat(state.searchQuery).isEqualTo("Search")
        verify(productCategoryListHandler).fetchCategories(searchQuery = "Search")
    }

    @Test
    fun `when load more is requested, then load next page`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        viewModel.onLoadMore()

        // THEN
        verify(productCategoryListHandler).loadMore()
    }

    @Test
    fun `when fetching coupons fails, then show an error`() = testBlocking {
        // GIVEN
        setup {
            whenever(productCategoryListHandler.fetchCategories(forceRefresh = true)).thenReturn(
                Result.failure(
                    Exception()
                )
            )
        }

        // WHEN
        val event = viewModel.event.captureValues().filterIsInstance<MultiLiveEvent.Event.ShowSnackbar>().last()

        // THEN
        assertThat(event.message).isEqualTo(R.string.product_category_selector_loading_failed)
    }

    @Test
    fun `when searching coupons fails, then show an error`() = testBlocking {
        // GIVEN
        setup {
            whenever(
                productCategoryListHandler.fetchCategories(searchQuery = "Search")
            ).thenReturn(Result.failure(Exception()))
        }

        // WHEN
        viewModel.onSearchQueryChanged("Search")
        advanceUntilIdle()
        val event = viewModel.event.captureValues().filterIsInstance<MultiLiveEvent.Event.ShowSnackbar>().last()

        // THEN
        assertThat(event.message).isEqualTo(R.string.product_category_selector_search_failed)
    }

    @Test
    fun `when loading next page, then show an error`() = testBlocking {
        // GIVEN
        setup {
            whenever(productCategoryListHandler.loadMore()).thenReturn(Result.failure(Exception()))
        }

        // WHEN
        viewModel.onLoadMore()
        advanceUntilIdle()
        val event = viewModel.event.captureValues().filterIsInstance<MultiLiveEvent.Event.ShowSnackbar>().last()

        // THEN
        assertThat(event.message).isEqualTo(R.string.product_category_selector_loading_failed)
    }

    @Test
    fun `when done is clicked, then exit with selected categories`() = testBlocking {
        // GIVEN
        val selectedCategories = longArrayOf(1L, 3L)
        setup(selectedCategories = selectedCategories)
        // WHEN
        viewModel.onDoneClick()
        val event = viewModel.event.captureValues()
            .filterIsInstance<MultiLiveEvent.Event.ExitWithResult<*>>()
            .last()

        // THEN
        val returnedCategories = event.data as Set<*>
        assertThat(returnedCategories).containsExactlyInAnyOrder(1L, 3L)
    }

    @Test
    fun `when back is pressed, then exit without result`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        viewModel.onBackPressed()

        // THEN
        val event = viewModel.event.captureValues()
            .filterIsInstance<MultiLiveEvent.Event.Exit>()
            .last()

        assertThat(event).isNotNull
    }

    @Test
    fun `when category is selected, then add to selection`() = testBlocking {
        // GIVEN
        setup()

        val initialState = viewModel.viewState.runAndCaptureValues {
            categoriesStateFlow.value = defaultProductCategoriesList
            advanceUntilIdle()
        }.last()

        // WHEN
        initialState.categories.first().onItemClick()

        // THEN
        val updatedState = viewModel.viewState.captureValues().last()
        defaultProductCategoriesList.first().productCategory.remoteCategoryId
        assertThat(updatedState.selectedCategoriesCount).isEqualTo(1)
        assertThat(updatedState.categories.first().isSelected).isTrue()
    }

    @Test
    fun `when clear selection is clicked, then remove all selected categories`() = testBlocking {
        // GIVEN
        val selectedCategories = longArrayOf(1L, 3L)
        setup(selectedCategories = selectedCategories)

        val initialState = viewModel.viewState.runAndCaptureValues {
            categoriesStateFlow.value = defaultProductCategoriesList
            advanceUntilIdle()
        }.last()

        assertThat(initialState.selectedCategoriesCount).isEqualTo(2)

        // WHEN
        viewModel.onClearSelectionClick()

        // THEN
        val updatedState = viewModel.viewState.captureValues().last()
        assertThat(updatedState.selectedCategoriesCount).isEqualTo(0)
        assertThat(updatedState.categories.all { !it.isSelected }).isTrue()
    }
}
