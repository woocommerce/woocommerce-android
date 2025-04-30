package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewState.SearchState
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SearchButtonTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosItemsSearchHelper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val childToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val analyticsTracker: WooPosAnalyticsTracker,
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
        observeAndTrackSearchInputStateOpen(viewStateFlow, coroutineScope)
    }

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

                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> Unit
                    is ParentToChildrenEvent.ItemClickedInProductSelector -> Unit
                    is ParentToChildrenEvent.CheckoutClicked -> Unit
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery -> Unit
                    is ParentToChildrenEvent.OrderCreated -> Unit
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
        viewStateFlow.value = currentState.copy(
            search = SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Hint(
                        resourceProvider.getString(R.string.woopos_search_products)
                    ),
                    isLoading = false,
                    hasAnimationPlayed = false
                )
            )
        )
    }

    fun getInitialSearchState(isProductsSearchEnabled: Boolean): SearchState {
        return when (isProductsSearchEnabled) {
            true -> SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )

            false -> SearchState.Hidden
        }
    }

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

    private fun observeAndTrackSearchInputStateOpen(
        viewStateFlow: MutableStateFlow<WooPosItemsViewState>,
        coroutineScope: CoroutineScope
    ) {
        viewStateFlow
            .map { it.search }
            .distinctUntilChanged()
            .map { it is SearchState.Visible && it.state is WooPosSearchInputState.Open }
            .distinctUntilChanged()
            .filter { it }
            .onEach {
                val event = SearchButtonTapped.apply {
                    addProperties(mapOf("item_list_type" to "products"))
                }
                analyticsTracker.track(event)
            }
            .launchIn(coroutineScope)
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
            is WooPosItemsViewState.CouponList -> this
        }
    }
}
