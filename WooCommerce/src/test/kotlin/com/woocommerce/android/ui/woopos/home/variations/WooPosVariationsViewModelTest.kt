package com.woocommerce.android.ui.woopos.home.variations

import app.cash.turbine.test
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.PaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosVariationsViewState
import com.woocommerce.android.ui.woopos.home.items.variations.FetchResult
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsDataSource
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsUIEvents
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsViewModel
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosVariationsViewModelTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val getProductById: WooPosGetProductById = mock()
    private val variationsDataSource: WooPosVariationsDataSource = mock()
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val priceFormat: WooPosFormatPrice = mock {
        onBlocking { invoke(any()) }.thenReturn("$10.0")
    }

    @Test
    fun `given variations from data source, when view model created, then view state updated correctly`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "10.0"),
            ProductTestUtils.generateProductVariation(2, 1, "20.0")
        )
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flowOf(FetchResult.Remote(Result.success(variations)))
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.init(1L)

        viewModel.viewState.test {
            // THEN
            val state = awaitItem() as WooPosVariationsViewState.Content
            assertThat(state.items).hasSize(2)
            assertThat(state.items[0].id).isEqualTo(1)
            assertThat(state.items[0].price).isEqualTo("$10.0")
        }
    }

    @Test
    fun `given empty variations list returned, when view model created, then view state is empty`() = runTest {
        // GIVEN
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flowOf(FetchResult.Remote(Result.success(emptyList())))
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.init(1L)
        viewModel.viewState.test {
            // THEN
            assertThat(awaitItem()).isEqualTo(WooPosVariationsViewState.Empty())
        }
    }

    @Test
    fun `given error fetching variations, when view model created, then view state is error`() = runTest {
        // GIVEN
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flowOf(FetchResult.Remote(Result.failure(Exception("Fetch error"))))
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.init(1L)

        viewModel.viewState.test {
            // THEN
            assertThat(awaitItem()).isEqualTo(WooPosVariationsViewState.Error())
        }
    }

    @Test
    fun `given variations, when pull to refresh triggered, then fetchFirstPage is called`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "10.0"),
            ProductTestUtils.generateProductVariation(2, 1, "20.0")
        )
        whenever(variationsDataSource.fetchFirstPage(any(), eq(true))).thenReturn(
            flowOf(FetchResult.Remote(Result.success(variations)))
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosVariationsUIEvents.PullToRefreshTriggered(123L))

        // THEN
        verify(variationsDataSource).fetchFirstPage(eq(123L), eq(true))
    }

    @Test
    fun `given no more variations, when end of list reached, then pagination state is none`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "10.0"),
            ProductTestUtils.generateProductVariation(2, 1, "20.0")
        )
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flowOf(FetchResult.Remote(Result.success(variations)))
        )
        whenever(variationsDataSource.canLoadMore(any())).thenReturn(false)
        whenever(variationsDataSource.loadMore(any())).thenReturn(Result.success(emptyList()))

        val viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosVariationsUIEvents.EndOfItemsListReached(123L, 10))
        advanceUntilIdle()
        // THEN
        viewModel.viewState.test {
            val state = awaitItem() as WooPosVariationsViewState.Content
            assertThat(state.paginationState).isEqualTo(PaginationState.None)
        }
    }

    @Test
    fun `given variations, when load more succeeds, then pagination state is updated`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "10.0"),
        )
        whenever(variationsDataSource.loadMore(any())).thenReturn(Result.success(variations))
        whenever(variationsDataSource.canLoadMore(any())).thenReturn(true)
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flow {
                emit(
                    FetchResult.Remote(
                        Result.success(
                            listOf(
                                ProductTestUtils.generateProductVariation(1, 1, "10.0"),
                                ProductTestUtils.generateProductVariation(2, 1, "20.0")
                            )
                        )
                    )
                )
            }
        )

        val viewModel = createViewModel()
        viewModel.init(1L)
        viewModel.onUIEvent(WooPosVariationsUIEvents.EndOfItemsListReached(123L, 10))

        // THEN
        viewModel.viewState.test {
            val state = awaitItem() as WooPosVariationsViewState.Content
            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].id).isEqualTo(1)
        }
    }

    @Test
    fun `given load more fails, when end of list reached, then pagination state is error`() = runTest {
        // GIVEN
        whenever(variationsDataSource.loadMore(any())).thenReturn(Result.failure(Exception()))
        whenever(variationsDataSource.canLoadMore(any())).thenReturn(true)
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flow {
                emit(
                    FetchResult.Remote(
                        Result.success(
                            listOf(
                                ProductTestUtils.generateProductVariation(1, 1, "10.0"),
                                ProductTestUtils.generateProductVariation(2, 1, "20.0")
                            )
                        )
                    )
                )
            }
        )

        val viewModel = createViewModel()
        viewModel.init(1L)
        advanceUntilIdle()
        viewModel.onUIEvent(WooPosVariationsUIEvents.EndOfItemsListReached(123L, 10))
        advanceUntilIdle()

        // THEN
        viewModel.viewState.test {
            val state = awaitItem() as WooPosVariationsViewState.Content
            assertThat(state.paginationState).isEqualTo(PaginationState.Error)
        }
    }

    @Test
    fun `given fetching variations first page and load more call is also happening, when view model created, then view state updated correctly`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "10.0"),
            ProductTestUtils.generateProductVariation(2, 1, "20.0")
        )
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flowOf(FetchResult.Remote(Result.success(variations)))
        )

        // WHEN
        val viewModel = createViewModel()
        val activeJob = Job()
        viewModel.loadMoreJob = activeJob
        viewModel.init(1L)
        viewModel.onUIEvent(WooPosVariationsUIEvents.EndOfItemsListReached(1L, 10))

        viewModel.viewState.test {
            // THEN
            val state = awaitItem() as WooPosVariationsViewState.Content
            assertThat(state.paginationState).isEqualTo(PaginationState.Loading)
        }
    }

    @Test
    fun `given fetching variations first page and load more call is not happening, when view model created, then view state updated correctly`() = runTest {
        // GIVEN
        val variations = listOf(
            ProductTestUtils.generateProductVariation(1, 1, "10.0"),
            ProductTestUtils.generateProductVariation(2, 1, "20.0")
        )
        whenever(variationsDataSource.fetchFirstPage(any(), any())).thenReturn(
            flowOf(FetchResult.Remote(Result.success(variations)))
        )

        // WHEN
        val viewModel = createViewModel()
        viewModel.init(1L)

        viewModel.viewState.test {
            // THEN
            val state = awaitItem() as WooPosVariationsViewState.Content
            assertThat(state.paginationState).isEqualTo(PaginationState.None)
        }
    }

    @Test
    fun `given variation clicked, when item clicked, then send event to parent`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosVariationsUIEvents.OnItemClicked(123L, 1L))

        // THEN
        verify(fromChildToParentEventSender).sendToParent(
            ChildToParentEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Variation(123L, 1L)
            )
        )
    }

    private fun createViewModel() =
        WooPosVariationsViewModel(
            fromChildToParentEventSender,
            getProductById,
            variationsDataSource,
            priceFormat
        )
}
