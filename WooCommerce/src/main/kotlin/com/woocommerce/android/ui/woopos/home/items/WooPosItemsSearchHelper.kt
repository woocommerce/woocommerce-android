package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.SearchState
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosItemsSearchHelper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val childToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
) {
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var viewStateFlow: MutableStateFlow<WooPosItemsToolbarViewState>

    private var wasLastStateClosed = true

    fun initialize(
        coroutineScope: CoroutineScope,
        viewStateFlow: MutableStateFlow<WooPosItemsToolbarViewState>
    ) {
        this.coroutineScope = coroutineScope
        this.viewStateFlow = viewStateFlow
        listenEventsFromParent()
    }

    @Suppress("CyclomaticComplexMethod")
    private fun listenEventsFromParent() {
        coroutineScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    ParentToChildrenEvent.SearchEvent.Started -> {
                        updateLoadingState(isLoading = true)
                    }

                    ParentToChildrenEvent.SearchEvent.Finished -> {
                        updateLoadingState(isLoading = false)
                    }

                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> {
                        onCloseSearchClicked()
                    }

                    is ParentToChildrenEvent.SearchEvent.RecentSearchSelected -> {
                        onSearchChanged(event.query, event.query.length)
                    }
                    is ParentToChildrenEvent.RefreshProductList -> {
                        if (isSearchOpen()) {
                            onSearchChanged("", 0)
                        }
                    }

                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> Unit
                    is ParentToChildrenEvent.ItemClickedInItemsList -> Unit
                    is ParentToChildrenEvent.CheckoutClicked -> Unit
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery -> Unit
                    is ParentToChildrenEvent.OrderCreated -> Unit
                    is ParentToChildrenEvent.CouponsRemoved -> Unit
                    is ParentToChildrenEvent.RemoveCouponsClicked -> Unit
                    is ParentToChildrenEvent.CouponsValidationFailed -> Unit
                }
            }
        }
    }

    fun onSearchChanged(newQuery: String, cursorPosition: Int) {
        coroutineScope.launch {
            childToParentEventSender.sendToParent(
                ChildToParentEvent.SearchEvent.QueryChanged(query = newQuery)
            )
        }

        if (newQuery.isEmpty()) {
            updateToInitialOpenState()
        } else {
            updateToOpenStateWithQuery(newQuery, cursorPosition)
        }
    }

    fun onCloseSearchClicked() {
        wasLastStateClosed = true
        viewStateFlow.value = viewStateFlow.value.copy(
            search = SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )
        )
    }

    fun onClearSearchClicked() {
        coroutineScope.launch {
            childToParentEventSender.sendToParent(
                ChildToParentEvent.SearchEvent.QueryChanged(query = "")
            )
        }
        updateToInitialOpenState()
    }

    fun isSearchOpen(): Boolean {
        val searchState = getCurrentSearchVisibleState() ?: return false
        return searchState.state is WooPosSearchInputState.Open
    }

    private fun updateToInitialOpenState() {
        val shouldRequestFocus = wasLastStateClosed
        wasLastStateClosed = false

        val searchHintStringRes = when (viewStateFlow.value) {
            is WooPosItemsToolbarViewState.ProductList -> R.string.woopos_search_products
            is WooPosItemsToolbarViewState.CouponList -> R.string.woopos_search_coupons
            is WooPosItemsToolbarViewState.VariationList -> error("Search is not applicable for variations list")
        }

        viewStateFlow.value = viewStateFlow.value.copy(
            search = SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Hint(resourceProvider.getString(searchHintStringRes)),
                    isLoading = false,
                    requestFocus = shouldRequestFocus,
                )
            )
        )
    }

    private fun updateToOpenStateWithQuery(query: String, cursorPosition: Int) {
        val shouldRequestFocus = wasLastStateClosed
        wasLastStateClosed = false

        viewStateFlow.value = viewStateFlow.value.copy(
            search = SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query(query, cursorPosition),
                    isLoading = false,
                    requestFocus = shouldRequestFocus,
                )
            )
        )
    }

    fun getInitialSearchState(): SearchState =
        SearchState.Visible(
            state = WooPosSearchInputState.Closed,
        )

    fun updateLoadingState(isLoading: Boolean) {
        val searchState = getCurrentSearchVisibleState() ?: return
        val searchStateValue = getCurrentSearchOpenState() ?: return

        val shouldShowLoading = if (viewStateFlow.value is WooPosItemsToolbarViewState.CouponList) {
            false
        } else {
            isLoading
        }

        viewStateFlow.value = viewStateFlow.value.copy(
            search = searchState.copy(
                state = searchStateValue.copy(
                    isLoading = shouldShowLoading
                )
            )
        )
    }

    private fun getCurrentSearchVisibleState(): SearchState.Visible? {
        val currentState = viewStateFlow.value
        return currentState.search as? SearchState.Visible
    }

    private fun getCurrentSearchOpenState(): WooPosSearchInputState.Open? {
        val searchState = getCurrentSearchVisibleState() ?: return null
        return searchState.state as? WooPosSearchInputState.Open
    }

    private fun WooPosItemsToolbarViewState.copy(search: SearchState.Visible): WooPosItemsToolbarViewState {
        return when (this) {
            is WooPosItemsToolbarViewState.ProductList -> this.copy(search = search)
            is WooPosItemsToolbarViewState.CouponList -> this.copy(search = search)
            is WooPosItemsToolbarViewState.VariationList -> this
        }
    }
}
