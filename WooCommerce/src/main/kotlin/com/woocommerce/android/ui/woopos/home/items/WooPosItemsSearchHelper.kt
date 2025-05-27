package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewState.SearchState
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
    private lateinit var viewStateFlow: MutableStateFlow<WooPosItemsViewState>

    fun initialize(
        coroutineScope: CoroutineScope,
        viewStateFlow: MutableStateFlow<WooPosItemsViewState>
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
                    is ParentToChildrenEvent.ItemClickedInProductSelector -> Unit
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

        val currentState = getCurrentContentState()

        if (newQuery.isEmpty()) {
            updateToInitialOpenState()
        } else {
            val currentOpenState = getCurrentSearchOpenState() ?: return
            viewStateFlow.value = currentState.copy(
                search = SearchState.Visible(
                    state = WooPosSearchInputState.Open(
                        input = WooPosSearchInputState.Open.Input.Query(newQuery, cursorPosition),
                        isLoading = false,
                        hasAnimationPlayed = currentOpenState.hasAnimationPlayed
                    )
                )
            )
        }
    }

    fun onCloseSearchClicked() {
        coroutineScope.launch {
            childToParentEventSender.sendToParent(ChildToParentEvent.SearchEvent.QueryChanged(query = ""))
        }
        val currentState = getCurrentContentState()
        viewStateFlow.value = currentState.copy(
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

    @Suppress("ReturnCount")
    fun onAnimationComplete() {
        val currentState = getCurrentContentState()
        val searchState = getCurrentSearchVisibleState() ?: return
        val openState = getCurrentSearchOpenState() ?: return

        viewStateFlow.value = currentState.copy(
            search = searchState.copy(
                state = openState.copy(
                    hasAnimationPlayed = true
                )
            )
        )
    }

    fun isSearchOpen(): Boolean {
        val searchState = getCurrentSearchVisibleState() ?: return false
        return searchState.state is WooPosSearchInputState.Open
    }

    private fun updateToInitialOpenState() {
        val currentState = getCurrentContentState()
        val searchHintStringRes = when (currentState) {
            is WooPosItemsViewState.ProductList -> R.string.woopos_search_products
            is WooPosItemsViewState.CouponList -> R.string.woopos_search_coupons
        }

        viewStateFlow.value = currentState.copy(
            search = SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Hint(resourceProvider.getString(searchHintStringRes)),
                    isLoading = false,
                    hasAnimationPlayed = false
                )
            )
        )
    }

    fun getInitialSearchState(): SearchState =
        SearchState.Visible(
            state = WooPosSearchInputState.Closed
        )

    @Suppress("ReturnCount")
    private fun updateLoadingState(isLoading: Boolean) {
        val currentState = getCurrentContentState()
        val searchState = getCurrentSearchVisibleState() ?: return
        val searchStateValue = getCurrentSearchOpenState() ?: return

        viewStateFlow.value = currentState.copy(
            search = searchState.copy(
                state = searchStateValue.copy(
                    isLoading = isLoading
                )
            )
        )
    }

    private fun getCurrentContentState(): WooPosItemsViewState {
        return viewStateFlow.value
    }

    private fun getCurrentSearchVisibleState(): SearchState.Visible? {
        val currentState = getCurrentContentState()
        return currentState.search as? SearchState.Visible
    }

    private fun getCurrentSearchOpenState(): WooPosSearchInputState.Open? {
        val searchState = getCurrentSearchVisibleState() ?: return null
        return searchState.state as? WooPosSearchInputState.Open
    }

    private fun WooPosItemsViewState.copy(search: SearchState.Visible): WooPosItemsViewState {
        return when (this) {
            is WooPosItemsViewState.ProductList -> this.copy(search = search)
            is WooPosItemsViewState.CouponList -> this.copy(search = search)
        }
    }
}
