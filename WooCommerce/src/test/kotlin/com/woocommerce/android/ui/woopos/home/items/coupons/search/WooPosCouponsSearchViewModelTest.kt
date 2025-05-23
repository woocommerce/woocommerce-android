package com.woocommerce.android.ui.woopos.home.items.coupons.search

import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.util.WooPosGetCachedStoreCurrency
import com.woocommerce.android.ui.woopos.util.format.WooPosCouponsFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCouponsSearchViewModelTest {
    private val dataSource: WooPosCouponsSearchDataSource = mock()
    private val childToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val couponsFormatter: WooPosCouponsFormatter = mock()
    private val getCachedStoreCurrency: WooPosGetCachedStoreCurrency = mock()
    private val emptyStateRepository: WooPosCouponsSearchEmptyStateRepository = mock()

    private val parentToChildrenEvents = MutableSharedFlow<ParentToChildrenEvent>()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: WooPosCouponsSearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEvents)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given empty state repository returns empty list, when view model is created, then initial state is empty search query`() = runTest {
        // GIVEN
        whenever(emptyStateRepository.getLastSearches()).thenReturn(emptyList())
        whenever(getCachedStoreCurrency()).thenReturn("USD")

        // WHEN
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        assert(viewModel.viewState.value is WooPosCouponsSearchViewState.EmptySearchQuery)
    }

    @Test
    fun `given search term, when recent search is clicked, then recent search selected event is sent`() = runTest {
        // GIVEN
        whenever(emptyStateRepository.getLastSearches()).thenReturn(emptyList())
        whenever(getCachedStoreCurrency()).thenReturn("USD")
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val searchTerm = "SUMMER"

        // WHEN
        viewModel.onUIEvent(WooPosCouponsSearchUiEvent.OnRecentSearchClicked(searchTerm))
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        verify(childToParentEventSender).sendToParent(
            ChildToParentEvent.SearchEvent.RecentSearchSelected(searchTerm)
        )
    }

    @Test
    fun `given coupon selection, when coupon is clicked, then child to parent event is sent`() = runTest {
        // GIVEN
        whenever(emptyStateRepository.getLastSearches()).thenReturn(emptyList())
        whenever(getCachedStoreCurrency()).thenReturn("USD")
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val coupon = WooPosItemSelectionViewState.Coupon(
            id = 1,
            name = "SUMMER10",
            summary = "10% off",
            expiredState = WooPosItemSelectionViewState.Coupon.ExpiredState.NotExpired
        )

        // WHEN
        viewModel.onUIEvent(WooPosCouponsSearchUiEvent.OnCouponClicked(coupon))
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        verify(childToParentEventSender).sendToParent(any<ChildToParentEvent.ItemClickedInProductSelector>())
    }

    @Test
    fun `when search finished event is emitted, then state is updated to empty search query`() = runTest {
        // GIVEN
        whenever(emptyStateRepository.getLastSearches()).thenReturn(emptyList())
        whenever(getCachedStoreCurrency()).thenReturn("USD")
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // WHEN
        parentToChildrenEvents.emit(ParentToChildrenEvent.SearchEvent.Finished)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        assert(viewModel.viewState.value is WooPosCouponsSearchViewState.EmptySearchQuery)
    }

    private fun createViewModel() = WooPosCouponsSearchViewModel(
        dataSource = dataSource,
        childToParentEventSender = childToParentEventSender,
        parentToChildrenEventReceiver = parentToChildrenEventReceiver,
        couponsFormatter = couponsFormatter,
        getCachedStoreCurrency = getCachedStoreCurrency,
        emptyStateRepository = emptyStateRepository,
    )
}
