package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
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
            parentToChildrenEventReceiver = mockParentToChildrenEventReceiver
        )
    }

    @Test
    fun `given products search enabled, when getInitialSearchState called, then returns visible closed state`() {
        // GIVEN
        val isProductsSearchEnabled = true

        // WHEN
        val result = searchHelper.getInitialSearchState(isProductsSearchEnabled)

        // THEN
        assertThat(result).isInstanceOf(WooPosItemsViewState.Content.SearchState.Visible::class.java)
        val visibleState = result as WooPosItemsViewState.Content.SearchState.Visible
        assertThat(visibleState.state).isInstanceOf(WooPosSearchInputState.Closed::class.java)
    }

    @Test
    fun `given products search disabled, when getInitialSearchState called, then returns hidden state`() {
        // GIVEN
        val isProductsSearchEnabled = false

        // WHEN
        val result = searchHelper.getInitialSearchState(isProductsSearchEnabled)

        // THEN
        assertThat(result).isInstanceOf(WooPosItemsViewState.Content.SearchState.Hidden::class.java)
    }

    @Test
    fun `given search input, when onSearchChanged called, then sends changed query event`() = runTest {
        // GIVEN
        val searchQuery = "test query"
        searchHelper.initialize(this, viewStateFlow)

        // WHEN
        searchHelper.onSearchChanged(searchQuery)
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
        searchHelper.initialize(this, viewStateFlow)

        // WHEN
        searchHelper.onSearchChanged(searchQuery)

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.Content
        val searchState = currentState.search as WooPosItemsViewState.Content.SearchState.Visible
        val openState = searchState.state as WooPosSearchInputState.Open
        assertThat(openState.input).isInstanceOf(WooPosSearchInputState.Open.Input.Query::class.java)
        val queryInput = openState.input as WooPosSearchInputState.Open.Input.Query
        assertThat(queryInput.text).isEqualTo(searchQuery)
    }

    @Test
    fun `given empty search input, when onSearchChanged called, then resets to initial open state`() = runTest {
        // GIVEN
        val emptyQuery = ""
        searchHelper.initialize(this, viewStateFlow)

        // WHEN
        searchHelper.onSearchChanged(emptyQuery)

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.Content
        val searchState = currentState.search as WooPosItemsViewState.Content.SearchState.Visible
        val openState = searchState.state as WooPosSearchInputState.Open
        assertThat(openState.input).isInstanceOf(WooPosSearchInputState.Open.Input.Hint::class.java)
    }

    @Test
    fun `given open search state, when onCloseSearchClicked called, then updates to closed state`() = runTest {
        // GIVEN
        searchHelper.initialize(this, viewStateFlow)
        searchHelper.onSearchChanged("initial query")

        // WHEN
        searchHelper.onCloseSearchClicked()

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.Content
        val searchState = currentState.search as WooPosItemsViewState.Content.SearchState.Visible
        assertThat(searchState.state).isInstanceOf(WooPosSearchInputState.Closed::class.java)
    }

    @Test
    fun `given search with query, when onClearSearchClicked called, then resets to initial open state with hint`() =
        runTest {
            // GIVEN
            searchHelper.initialize(this, viewStateFlow)
            searchHelper.onSearchChanged("initial query")

            // WHEN
            searchHelper.onClearSearchClicked()

            // THEN
            val currentState = viewStateFlow.value as WooPosItemsViewState.Content
            val searchState = currentState.search as WooPosItemsViewState.Content.SearchState.Visible
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
        val currentState = viewStateFlow.value as WooPosItemsViewState.Content
        val searchState = currentState.search as WooPosItemsViewState.Content.SearchState.Visible
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
        val currentState = viewStateFlow.value as WooPosItemsViewState.Content
        val searchState = currentState.search as WooPosItemsViewState.Content.SearchState.Visible
        val openState = searchState.state as WooPosSearchInputState.Open
        assertThat(openState.isLoading).isFalse
    }

    private fun createContentState(): WooPosItemsViewState.Content {
        return WooPosItemsViewState.Content(
            search = WooPosItemsViewState.Content.SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Hint("Search products"),
                    isLoading = false
                )
            ),
            items = emptyList(),
            bannerState = WooPosItemsViewState.Content.BannerState(
                isBannerHiddenByUser = false,
                title = R.string.app_name,
                message = R.string.app_name,
                icon = R.drawable.ic_woo
            ),
            paginationState = WooPosPaginationState.None,
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            couponsEnabled = false
        )
    }

    @Test
    fun `when closed search, then pull to refresh is enabled`() = runTest {
        // GIVEN
        searchHelper.initialize(this, viewStateFlow)

        // WHEN
        searchHelper.onCloseSearchClicked()

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.Content
        assertThat(currentState.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
    }

    @Test
    fun `when search changed, then pull to refresh is disabled`() = runTest {
        // GIVEN
        searchHelper.initialize(this, viewStateFlow)

        // WHEN
        searchHelper.onSearchChanged("test query")

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.Content
        assertThat(currentState.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Disabled)
    }

    @Test
    fun `when hidden search state, then pull to refresh is enabled`() = runTest {
        // GIVEN
        searchHelper.initialize(this, viewStateFlow)
        val contentState = createContentState().copy(
            search = WooPosItemsViewState.Content.SearchState.Hidden
        )

        // WHEN
        viewStateFlow.value = contentState

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.Content
        assertThat(currentState.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
    }

    @Test
    fun `when animation completes, then hasAnimationPlayed flag is set to true`() = runTest {
        // GIVEN
        searchHelper.initialize(this, viewStateFlow)

        // WHEN
        searchHelper.onAnimationComplete()

        // THEN
        val currentState = viewStateFlow.value as WooPosItemsViewState.Content
        val searchState = currentState.search as WooPosItemsViewState.Content.SearchState.Visible
        val openState = searchState.state as WooPosSearchInputState.Open
        assertThat(openState.hasAnimationPlayed).isTrue
    }

    @Test
    fun `given animation completed, when search changed, then hasAnimationPlayed is preserved`() = runTest {
        // GIVEN
        searchHelper.initialize(this, viewStateFlow)
        searchHelper.onAnimationComplete()

        // WHEN

        searchHelper.onSearchChanged("test")
        advanceUntilIdle()

        // THEN
        val afterReopenState = viewStateFlow.value as WooPosItemsViewState.Content
        val afterReopenSearchState = afterReopenState.search as WooPosItemsViewState.Content.SearchState.Visible
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
        val currentState = viewStateFlow.value as WooPosItemsViewState.Content
        val searchState = currentState.search as WooPosItemsViewState.Content.SearchState.Visible
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
        val currentState = viewStateFlow.value as WooPosItemsViewState.Content
        val searchState = currentState.search as WooPosItemsViewState.Content.SearchState.Visible
        assertThat(searchState.state).isInstanceOf(WooPosSearchInputState.Closed::class.java)
    }
}
