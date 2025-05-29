package com.woocommerce.android.ui.woopos.home.items.coupons.search

import app.cash.turbine.test
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosCouponsSearchViewModelTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val mockViewStateManager: WooPosCouponsListViewStateManager = mock()
    private val mockChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val mockParentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val mockEmptyStateRepository: WooPosCouponsSearchEmptyStateRepository = mock()

    private val testCoupon = WooPosItemSelectionViewState.Coupon(
        id = 1L,
        name = "TESTCOUPON",
        summary = "Test summary",
        expiredState = WooPosItemSelectionViewState.Coupon.ExpiredState.NotExpired
    )

    @Before
    fun setup() = runTest {
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(flowOf())
        whenever(mockViewStateManager.viewState).thenReturn(
            flowOf(WooPosCouponsViewState.Empty())
        )
        whenever(mockViewStateManager.getCurrentSearchQuery()).thenReturn(null)
        whenever(mockViewStateManager.getRecentSearches()).thenReturn(emptyList())
        whenever(mockEmptyStateRepository.getLastSearches()).thenReturn(emptyList())
    }

    @Test
    fun `given view model initialization, when view model created, then initial state is EmptySearchQuery with recent searches`() = runTest {
        // GIVEN
        val recentSearches = listOf("SUMMER", "DISCOUNT")
        whenever(mockViewStateManager.getRecentSearches()).thenReturn(recentSearches)
        whenever(mockEmptyStateRepository.getLastSearches()).thenReturn(recentSearches)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosCouponsSearchViewState.EmptySearchQuery::class.java)
            val emptyState = value as WooPosCouponsSearchViewState.EmptySearchQuery
            assertThat(emptyState.recentSearches).isEqualTo(recentSearches)
        }

        verify(mockViewStateManager).setRecentSearches(recentSearches)
    }

    @Test
    fun `given empty state repository returns empty list, when view model is created, then initial state is empty search query`() = runTest {
        // GIVEN
        whenever(mockEmptyStateRepository.getLastSearches()).thenReturn(emptyList())

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosCouponsSearchViewState.EmptySearchQuery::class.java)
            val emptyState = value as WooPosCouponsSearchViewState.EmptySearchQuery
            assertThat(emptyState.recentSearches).isEmpty()
        }
    }

    @Test
    fun `given search term, when recent search is clicked, then recent search selected event is sent`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        val searchTerm = "SUMMER"

        // WHEN
        viewModel.onUIEvent(WooPosCouponsSearchUiEvent.OnRecentSearchClicked(searchTerm))
        advanceUntilIdle()

        // THEN
        verify(mockChildToParentEventSender).sendToParent(
            ChildToParentEvent.SearchEvent.RecentSearchSelected(searchTerm)
        )
    }

    @Test
    fun `given coupon selection, when coupon is clicked, then child to parent event is sent and recent search stored`() = runTest {
        // GIVEN
        val searchQuery = "DISCOUNT"

        whenever(mockViewStateManager.getCurrentSearchQuery()).thenReturn(searchQuery)
        whenever(mockViewStateManager.viewState).thenReturn(
            flowOf(
                WooPosCouponsViewState.Content(
                    items = listOf(testCoupon),
                    paginationState = WooPosPaginationState.None
                )
            )
        )

        val viewModel = createViewModel()

        // Wait for the view state to be initialized and emit the Content state
        viewModel.viewState.test {
            val initialState = awaitItem()
            assertThat(initialState).isInstanceOf(WooPosCouponsSearchViewState.Content::class.java)

            // WHEN
            viewModel.onUIEvent(WooPosCouponsSearchUiEvent.OnCouponClicked(testCoupon))
            advanceUntilIdle()

            // THEN
            verify(mockChildToParentEventSender).sendToParent(any<ChildToParentEvent.ItemClickedInItemsList>())
            verify(mockEmptyStateRepository).addRecentSearch(searchQuery)
        }
    }

    @Test
    fun `when search finished event is emitted, then state is updated to empty search query with recent searches`() = runTest {
        // GIVEN
        val recentSearches = listOf("WINTER", "SALE")
        val mockEvents = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(mockEvents)
        whenever(mockEmptyStateRepository.getLastSearches()).thenReturn(recentSearches)
        createViewModel()
        advanceUntilIdle()

        // WHEN
        mockEvents.emit(ParentToChildrenEvent.SearchEvent.Finished)
        advanceUntilIdle()

        // THEN
        verify(mockViewStateManager, atLeast(2)).setRecentSearches(recentSearches)
    }

    @Test
    fun `given search query changed event, when received, then setSearchQuery called on ViewStateManager`() = runTest {
        // GIVEN
        val searchQuery = "test coupon"
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.ChangedQuery(searchQuery))
        )

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        verify(mockViewStateManager).setSearchQuery(eq(searchQuery), any())
    }

    @Test
    fun `given OnNextPageRequested event, when triggered, then endOfListReached called on ViewStateManager`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosCouponsSearchUiEvent.OnNextPageRequested)
        advanceUntilIdle()

        // THEN
        verify(mockViewStateManager).endOfListReached(any())
    }

    @Test
    fun `given LoadingErrorRetryButtonClicked event, when triggered, then fetchCoupons with RETRY called on ViewStateManager`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosCouponsSearchUiEvent.LoadingErrorRetryButtonClicked)
        advanceUntilIdle()

        // THEN
        verify(mockViewStateManager).fetchCoupons(
            any(),
            eq(WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.RETRY)
        )
    }

    @Test
    fun `given content state with search query, when mapped to search view state, then returns Content with query`() = runTest {
        // GIVEN
        val searchQuery = "test"
        val contentState = WooPosCouponsViewState.Content(
            items = listOf(testCoupon),
            paginationState = WooPosPaginationState.None
        )

        whenever(mockViewStateManager.getCurrentSearchQuery()).thenReturn(searchQuery)
        whenever(mockViewStateManager.viewState).thenReturn(flowOf(contentState))

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosCouponsSearchViewState.Content::class.java)

            val content = value as WooPosCouponsSearchViewState.Content
            assertThat(content.searchQuery).isEqualTo(searchQuery)
            assertThat(content.items).hasSize(1)
            assertThat(content.items[0]).isEqualTo(testCoupon)
        }
    }

    @Test
    fun `given active search query, when coupon is clicked, then recent search is stored`() = runTest {
        // GIVEN
        val searchQuery = "SUMMER20"
        whenever(mockViewStateManager.getCurrentSearchQuery()).thenReturn(searchQuery)

        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosCouponsSearchUiEvent.OnCouponClicked(testCoupon))
        advanceUntilIdle()

        // THEN
        verify(mockChildToParentEventSender).sendToParent(any<ChildToParentEvent.ItemClickedInItemsList>())
        verify(mockEmptyStateRepository).addRecentSearch(searchQuery)
    }

    @Test
    fun `given no active search query, when coupon is clicked, then no recent search is stored`() = runTest {
        // GIVEN
        whenever(mockViewStateManager.getCurrentSearchQuery()).thenReturn(null)

        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosCouponsSearchUiEvent.OnCouponClicked(testCoupon))
        advanceUntilIdle()

        // THEN
        verify(mockChildToParentEventSender).sendToParent(any<ChildToParentEvent.ItemClickedInItemsList>())
        verify(mockEmptyStateRepository, never()).addRecentSearch(any())
    }

    private fun createViewModel() = WooPosCouponsSearchViewModel(
        viewStateManager = mockViewStateManager,
        childToParentEventSender = mockChildToParentEventSender,
        parentToChildrenEventReceiver = mockParentToChildrenEventReceiver,
        emptyStateRepository = mockEmptyStateRepository
    )
}
