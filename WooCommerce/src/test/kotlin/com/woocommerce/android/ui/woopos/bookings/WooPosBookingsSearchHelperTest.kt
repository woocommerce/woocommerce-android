package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

class WooPosBookingsSearchHelperTest {
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { "Search bookings" }
    }

    private lateinit var searchHelper: WooPosBookingsSearchHelper
    private lateinit var stateFlow: MutableStateFlow<WooPosBookingsState>

    @Before
    fun setUp() {
        searchHelper = WooPosBookingsSearchHelper(resourceProvider)
        stateFlow = MutableStateFlow(
            WooPosBookingsState.Content(
                items = WooPosBookingsState.Content.Items.NothingFound(title = "", message = ""),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                dateSelectorState = DateSelectorState(formattedDate = "19 Feb, Wed", selectedDateMillis = 0L),
                searchInputState = WooPosSearchInputState.Closed,
                selectedDetails = null,
                paginationState = WooPosPaginationState.None,
                dialogState = WooPosBookingsState.Content.DialogState.Hidden
            )
        )
        searchHelper.initialize(stateFlow)
    }

    @Test
    fun `when search icon clicked, then state is Open with Hint and requestFocus`() {
        // WHEN
        searchHelper.onSearchIconClicked()

        // THEN
        val searchState = stateFlow.value.searchInputState as WooPosSearchInputState.Open
        assertThat(searchState.input).isInstanceOf(WooPosSearchInputState.Open.Input.Hint::class.java)
        assertThat(searchState.requestFocus).isTrue()
        assertThat(searchState.isLoading).isFalse()
    }

    @Test
    fun `when search changed with query, then state is Open with Query`() {
        // GIVEN
        searchHelper.onSearchIconClicked()

        // WHEN
        searchHelper.onSearchChanged("haircut", 7)

        // THEN
        val searchState = stateFlow.value.searchInputState as WooPosSearchInputState.Open
        val query = searchState.input as WooPosSearchInputState.Open.Input.Query
        assertThat(query.query).isEqualTo("haircut")
        assertThat(query.cursorPosition).isEqualTo(7)
    }

    @Test
    fun `when clear clicked, then state is Open with Hint and requestFocus`() {
        // GIVEN
        searchHelper.onSearchIconClicked()
        searchHelper.onSearchChanged("haircut", 7)

        // WHEN
        searchHelper.onClearSearchClicked()

        // THEN
        val searchState = stateFlow.value.searchInputState as WooPosSearchInputState.Open
        assertThat(searchState.input).isInstanceOf(WooPosSearchInputState.Open.Input.Hint::class.java)
        assertThat(searchState.requestFocus).isTrue()
    }

    @Test
    fun `when close clicked, then state is Closed`() {
        // GIVEN
        searchHelper.onSearchIconClicked()

        // WHEN
        searchHelper.onCloseSearchClicked()

        // THEN
        assertThat(stateFlow.value.searchInputState).isEqualTo(WooPosSearchInputState.Closed)
    }

    @Test
    fun `when updateLoadingState true, then isLoading is true`() {
        // GIVEN
        searchHelper.onSearchIconClicked()

        // WHEN
        searchHelper.updateLoadingState(true)

        // THEN
        val searchState = stateFlow.value.searchInputState as WooPosSearchInputState.Open
        assertThat(searchState.isLoading).isTrue()
    }

    @Test
    fun `when updateLoadingState false, then isLoading is false`() {
        // GIVEN
        searchHelper.onSearchIconClicked()
        searchHelper.updateLoadingState(true)

        // WHEN
        searchHelper.updateLoadingState(false)

        // THEN
        val searchState = stateFlow.value.searchInputState as WooPosSearchInputState.Open
        assertThat(searchState.isLoading).isFalse()
    }

    @Test
    fun `when getCurrentSearchQuery with query, then returns query`() {
        // GIVEN
        searchHelper.onSearchIconClicked()
        searchHelper.onSearchChanged("haircut", 7)

        // WHEN
        val query = searchHelper.getCurrentSearchQuery()

        // THEN
        assertThat(query).isEqualTo("haircut")
    }

    @Test
    fun `when getCurrentSearchQuery with hint, then returns null`() {
        // GIVEN
        searchHelper.onSearchIconClicked()

        // WHEN
        val query = searchHelper.getCurrentSearchQuery()

        // THEN
        assertThat(query).isNull()
    }

    @Test
    fun `when getCurrentSearchQuery with closed, then returns null`() {
        // WHEN
        val query = searchHelper.getCurrentSearchQuery()

        // THEN
        assertThat(query).isNull()
    }

    @Test
    fun `when isSearchOpen with open state, then returns true`() {
        // GIVEN
        searchHelper.onSearchIconClicked()

        // WHEN & THEN
        assertThat(searchHelper.isSearchOpen()).isTrue()
    }

    @Test
    fun `when isSearchOpen with closed state, then returns false`() {
        // WHEN & THEN
        assertThat(searchHelper.isSearchOpen()).isFalse()
    }
}
