package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosItemsSearchHelperTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val mockResourceProvider: ResourceProvider = mock()
    private val mockChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val mockParentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val mockAnalyticsTracker: WooPosAnalyticsTracker = mock()

    private lateinit var viewStateFlow: MutableStateFlow<WooPosItemsViewState>
    private lateinit var searchHelper: WooPosItemsSearchHelper

    @Before
    fun setup() {
        whenever(mockResourceProvider.getString(any())).thenReturn("Search products")
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(flowOf())

        viewStateFlow = MutableStateFlow(createContentState())
        searchHelper = WooPosItemsSearchHelper(
            resourceProvider = mockResourceProvider,
            childToParentEventSender = mockChildToParentEventSender,
            parentToChildrenEventReceiver = mockParentToChildrenEventReceiver,
            analyticsTracker = mockAnalyticsTracker,
        )
    }

    @Test
    fun `given products search enabled, when getInitialSearchState called, then returns visible closed state`() {
        // GIVEN
        val isProductsSearchEnabled = true

        // WHEN
        val result = searchHelper.getInitialSearchState(isProductsSearchEnabled)

        // THEN
        assertThat(result).isInstanceOf(WooPosItemsViewState.SearchState.Visible::class.java)
        val visibleState = result as WooPosItemsViewState.SearchState.Visible
        assertThat(visibleState.state).isInstanceOf(WooPosSearchInputState.Closed::class.java)
    }

    @Test
    fun `given products search disabled, when getInitialSearchState called, then returns hidden state`() {
        // GIVEN
        val isProductsSearchEnabled = false

        // WHEN
        val result = searchHelper.getInitialSearchState(isProductsSearchEnabled)

        // THEN
        assertThat(result).isInstanceOf(WooPosItemsViewState.SearchState.Hidden::class.java)
    }

    @Test
    fun `given search input, when onSearchChanged called, then sends changed query event`() = runTest {
        // GIVEN
        val searchQuery = "test query"
        val cursorPosition = searchQuery.length
        searchHelper.initialize(this, viewStateFlow)

        // WHEN
        searchHelper.onSearchChanged(searchQuery, cursorPosition)
        advanceUntilIdle()

        // THEN
        verify(mockChildToParentEventSender).sendToParent(
            ChildToParentEvent.SearchEvent.QueryChanged(searchQuery)
        )
    }

    @Test
    fun `given non-empty search input, when onSearchChanged called, then updates view state with query`() = runTest {
        // GIVEN
        val searchQuery = "test query"
        val cursorPosition = searchQuery.length
        searchHelper.initialize(this, viewStateFlow)

        // WHEN
        searchHelper.onSearchChanged(searchQuery, cursorPosition)

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.ProductList
        val searchState = currentState.search as WooPosItemsViewState.SearchState.Visible
        val openState = searchState.state as WooPosSearchInputState.Open
        assertThat(openState.input).isInstanceOf(WooPosSearchInputState.Open.Input.Query::class.java)
        val queryInput = openState.input as WooPosSearchInputState.Open.Input.Query
        assertThat(queryInput.text).isEqualTo(searchQuery)
        assertThat(queryInput.cursorPosition).isEqualTo(cursorPosition)
    }

    @Test
    fun `given empty search input, when onSearchChanged called, then resets to initial open state`() = runTest {
        // GIVEN
        val emptyQuery = ""
        val cursorPosition = 0
        searchHelper.initialize(this, viewStateFlow)

        // WHEN
        searchHelper.onSearchChanged(emptyQuery, cursorPosition)

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.ProductList
        val searchState = currentState.search as WooPosItemsViewState.SearchState.Visible
        val openState = searchState.state as WooPosSearchInputState.Open
        assertThat(openState.input).isInstanceOf(WooPosSearchInputState.Open.Input.Hint::class.java)
    }

    @Test
    fun `given open search state, when onCloseSearchClicked called, then updates to closed state`() = runTest {
        // GIVEN
        searchHelper.initialize(this, viewStateFlow)
        searchHelper.onSearchChanged("initial query", "initial query".length)

        // WHEN
        searchHelper.onCloseSearchClicked()

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.ProductList
        val searchState = currentState.search as WooPosItemsViewState.SearchState.Visible
        assertThat(searchState.state).isInstanceOf(WooPosSearchInputState.Closed::class.java)
    }

    @Test
    fun `given search with query, when onClearSearchClicked called, then resets to initial open state with hint`() =
        runTest {
            // GIVEN
            searchHelper.initialize(this, viewStateFlow)
            searchHelper.onSearchChanged("initial query", "initial query".length)

            // WHEN
            searchHelper.onClearSearchClicked()

            // THEN
            val currentState = viewStateFlow.value as WooPosItemsViewState.ProductList
            val searchState = currentState.search as WooPosItemsViewState.SearchState.Visible
            val openState = searchState.state as WooPosSearchInputState.Open
            assertThat(openState.input).isInstanceOf(WooPosSearchInputState.Open.Input.Hint::class.java)
        }

    @Test
    fun `given search event started, when received, then updates loading state to true`() = runTest {
        // GIVEN
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.Started)
        )

        // WHEN
        searchHelper.initialize(this, viewStateFlow)
        advanceUntilIdle()

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.ProductList
        val searchState = currentState.search as WooPosItemsViewState.SearchState.Visible
        val openState = searchState.state as WooPosSearchInputState.Open
        assertThat(openState.isLoading).isTrue
    }

    @Test
    fun `given search event finished, when received, then updates loading state to false`() = runTest {
        // GIVEN
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.SearchEvent.Finished)
        )

        // WHEN
        searchHelper.initialize(this, viewStateFlow)

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.ProductList
        val searchState = currentState.search as WooPosItemsViewState.SearchState.Visible
        val openState = searchState.state as WooPosSearchInputState.Open
        assertThat(openState.isLoading).isFalse
    }

    @Test
    fun `when animation completes, then hasAnimationPlayed flag is set to true`() = runTest {
        // GIVEN
        searchHelper.initialize(this, viewStateFlow)

        // WHEN
        searchHelper.onAnimationComplete()

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.ProductList
        val searchState = currentState.search as WooPosItemsViewState.SearchState.Visible
        val openState = searchState.state as WooPosSearchInputState.Open
        assertThat(openState.hasAnimationPlayed).isTrue
    }

    @Test
    fun `given animation completed, when search changed, then hasAnimationPlayed is preserved`() = runTest {
        // GIVEN
        searchHelper.initialize(this, viewStateFlow)
        searchHelper.onAnimationComplete()

        // WHEN

        searchHelper.onSearchChanged("test", "test".length)
        advanceUntilIdle()

        // THEN
        val afterReopenState = viewStateFlow.value as WooPosItemsViewState.ProductList
        val afterReopenSearchState = afterReopenState.search as WooPosItemsViewState.SearchState.Visible
        assertThat(afterReopenSearchState.state).isInstanceOf(WooPosSearchInputState.Open::class.java)

        val openState = afterReopenSearchState.state as WooPosSearchInputState.Open
        assertThat(openState.hasAnimationPlayed).isTrue
    }

    @Test
    fun `given animation has played, when onClearSearchClicked, then hasAnimationPlayed is false`() = runTest {
        // GIVEN
        searchHelper.initialize(this, viewStateFlow)

        searchHelper.onAnimationComplete()

        // WHEN
        searchHelper.onClearSearchClicked()

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.ProductList
        val searchState = currentState.search as WooPosItemsViewState.SearchState.Visible
        val openState = searchState.state as WooPosSearchInputState.Open
        assertThat(openState.hasAnimationPlayed).isFalse
    }

    @Test
    fun `given animation complete, when order successful paid, then state is closed`() = runTest {
        // GIVEN
        searchHelper.initialize(this, viewStateFlow)
        searchHelper.onAnimationComplete()

        // WHEN
        whenever(mockParentToChildrenEventReceiver.events).thenReturn(
            flowOf(
                ParentToChildrenEvent.OrderSuccessfullyPaid(
                    ParentToChildrenEvent.OrderSuccessfullyPaid.PaymentMethod.CARD
                )
            )
        )
        searchHelper.initialize(this, viewStateFlow)
        advanceUntilIdle()

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.ProductList
        val searchState = currentState.search as WooPosItemsViewState.SearchState.Visible
        assertThat(searchState.state).isInstanceOf(WooPosSearchInputState.Closed::class.java)
    }

    private fun createContentState(): WooPosItemsViewState.ProductList {
        return WooPosItemsViewState.ProductList(
            search = WooPosItemsViewState.SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Hint("Search products"),
                    isLoading = false
                )
            ),
            tabs = listOf(
                WooPosItemsViewState.Tab(
                    R.string.woopos_products_screen_title,
                    highlightLevel = WooPosItemsViewState.Tab.HighlightLevel.Full
                ),
                WooPosItemsViewState.Tab(
                    R.string.woopos_coupons_screen_title,
                    highlightLevel = WooPosItemsViewState.Tab.HighlightLevel.Normal
                )
            )
        )
    }
}
