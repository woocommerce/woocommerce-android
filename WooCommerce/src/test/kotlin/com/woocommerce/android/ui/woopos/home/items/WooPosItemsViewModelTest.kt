package com.woocommerce.android.ui.woopos.home.items

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsProductsSearchEnabled
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SearchButtonTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosItemsViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val tabs = listOf(
        WooPosItemsViewState.Tab(
            R.string.woopos_products_screen_title,
            WooPosItemsViewState.Tab.HighlightLevel.Full
        ),
        WooPosItemsViewState.Tab(
            R.string.woopos_coupons_screen_title,
            WooPosItemsViewState.Tab.HighlightLevel.Normal
        )
    )

    private val wooPosItemsNavigator: WooPosItemsNavigator = mock()

    private val isProductsSearchEnabled: WooPosIsProductsSearchEnabled = mock()
    private val searchHelper: WooPosItemsSearchHelper = mock()
    private val tabsHelper: WooPosItemsTabsHelper = mock {
        on { defaultTabs }.thenReturn(tabs)
    }

    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private lateinit var _viewState: MutableStateFlow<WooPosItemsViewState>

    @Before
    fun setup() {
        whenever(searchHelper.getInitialSearchState(any())).thenReturn(
            WooPosItemsViewState.SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )
        )

        _viewState = MutableStateFlow<WooPosItemsViewState>(
            WooPosItemsViewState.ProductList(
                tabs = tabsHelper.defaultTabs,
                search = searchHelper.getInitialSearchState(isProductsSearchEnabled()),
            )
        )
    }

    @Test
    fun `given variations screen, when clicked back, then trigger proper event`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.BackButtonClicked)

        // THEN
        verify(wooPosItemsNavigator).sendNavigationEvent(
            WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen
        )
    }

    @Test
    fun `given products search feature enabled, when view model created, then search state is visible`() = runTest {
        // GIVEN
        whenever(isProductsSearchEnabled()).thenReturn(true)
        whenever(searchHelper.getInitialSearchState(true)).thenReturn(
            WooPosItemsViewState.SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val contentState = awaitItem() as WooPosItemsViewState.ProductList
            assertThat(contentState.search).isInstanceOf(WooPosItemsViewState.SearchState.Visible::class.java)
            val searchState = contentState.search as WooPosItemsViewState.SearchState.Visible
            assertThat(searchState.state).isEqualTo(WooPosSearchInputState.Closed)
        }
    }

    @Test
    fun `given products search feature disabled, when view model created, then search state is hidden`() = runTest {
        // GIVEN
        whenever(isProductsSearchEnabled()).thenReturn(false)
        whenever(searchHelper.getInitialSearchState(false)).thenReturn(
            WooPosItemsViewState.SearchState.Hidden
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.viewState.test {
            val contentState = awaitItem() as WooPosItemsViewState.ProductList
            assertThat(contentState.search).isInstanceOf(WooPosItemsViewState.SearchState.Hidden::class.java)
        }
    }

    @Test
    fun `given search visible, when close search clicked, then search state is closed`() = runTest {
        // GIVEN
        whenever(isProductsSearchEnabled()).thenReturn(true)

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsUIEvent.CloseSearchClicked)

        // THEN
        verify(searchHelper).onCloseSearchClicked()
    }

    @Test
    fun `given search visible, when search text changed, then search helper is called`() = runTest {
        val query = "test query"
        val viewModel = createViewModel()

        viewModel.onUIEvent(WooPosItemsUIEvent.SearchChanged(query, 0))

        verify(searchHelper).onSearchChanged(query, 0)
    }

    @Test
    fun `given search visible, when clear search clicked, then search helper is called`() = runTest {
        val viewModel = createViewModel()

        viewModel.onUIEvent(WooPosItemsUIEvent.ClearSearchClicked)

        verify(searchHelper).onClearSearchClicked()
    }

    @Test
    fun `given search visible, when close search clicked, then search helper is called`() = runTest {
        whenever(isProductsSearchEnabled()).thenReturn(true)

        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosItemsUIEvent.CloseSearchClicked)

        verify(searchHelper).onCloseSearchClicked()
    }

    @Test
    fun `when tab clicked, then tab is selected and state is updated`() = runTest {
        // GIVEN
        val couponsTab = WooPosItemsViewState.Tab(
            R.string.woopos_coupons_screen_title,
            WooPosItemsViewState.Tab.HighlightLevel.Normal
        )

        whenever(tabsHelper.selectTab(any(), eq(couponsTab))).thenReturn(
            listOf(
                WooPosItemsViewState.Tab(
                    R.string.woopos_products_screen_title,
                    WooPosItemsViewState.Tab.HighlightLevel.Normal
                ),
                WooPosItemsViewState.Tab(
                    R.string.woopos_coupons_screen_title,
                    WooPosItemsViewState.Tab.HighlightLevel.Full
                )
            )
        )

        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosItemsUIEvent.OnTabClicked(couponsTab))

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosItemsViewState.CouponList::class.java)
        }
    }

    @Test
    fun `when search state changes from closed to open, then SearchButtonTapped event is tracked`() = runTest {
        // GIVEN
        createViewModel()

        val initialState = WooPosItemsViewState.ProductList(
            tabs = tabsHelper.defaultTabs,
            search = WooPosItemsViewState.SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )
        )
        _viewState.value = initialState

        // WHEN - Change state to open
        val newState = WooPosItemsViewState.ProductList(
            tabs = tabsHelper.defaultTabs,
            search = WooPosItemsViewState.SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Hint(""),
                    isLoading = false
                )
            )
        )
        _viewState.value = newState
        advanceUntilIdle()

        // THEN
        verify(analyticsTracker).track(SearchButtonTapped)
    }

    @Test
    fun `when search state changes but not from closed to open, event is not tracked`() = runTest {
        // GIVEN
        createViewModel()

        val initialState = WooPosItemsViewState.ProductList(
            tabs = tabsHelper.defaultTabs,
            search = WooPosItemsViewState.SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Hint(""),
                    isLoading = false
                )
            )
        )
        _viewState.value = initialState

        // WHEN
        val newState = WooPosItemsViewState.ProductList(
            tabs = tabsHelper.defaultTabs,
            search = WooPosItemsViewState.SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query("test", 4),
                    isLoading = false
                )
            )
        )
        _viewState.value = newState
        advanceUntilIdle()

        // THEN
        verify(analyticsTracker, never()).track(SearchButtonTapped)
    }

    private fun createViewModel() =
        WooPosItemsViewModel(
            wooPosItemsNavigator,
            searchHelper,
            isProductsSearchEnabled,
            tabsHelper,
        )
}
