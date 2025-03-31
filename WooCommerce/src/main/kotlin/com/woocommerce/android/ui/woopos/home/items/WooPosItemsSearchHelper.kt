package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class WooPosItemsSearchHelper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val childToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver
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

                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> Unit
                    is ParentToChildrenEvent.ItemClickedInProductSelector -> Unit
                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> Unit
                    is ParentToChildrenEvent.CheckoutClicked -> Unit
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery -> Unit
                }
            }
        }
    }

    fun onSearchChanged(newQuery: String) {
        coroutineScope.launch {
            childToParentEventSender.sendToParent(
                ChildToParentEvent.SearchEvent.ChangedQuery(query = newQuery)
            )
        }

        val currentState = getCurrentContentState() ?: return

        if (newQuery.isEmpty()) {
            updateToInitialOpenState()
        } else {
            updateSearchState(
                currentState.copy(
                    search = WooPosItemsViewState.Content.SearchState.Visible(
                        state = WooPosSearchInputState.Open(
                            input = WooPosSearchInputState.Open.Input.Query(newQuery),
                            isLoading = false,
                        )
                    )
                )
            )
        }
    }

    fun onCloseSearchClicked() {
        val currentState = getCurrentContentState() ?: return
        updateSearchState(
            currentState.copy(
                search = WooPosItemsViewState.Content.SearchState.Visible(
                    state = WooPosSearchInputState.Closed
                )
            )
        )
    }

    fun onClearSearchClicked() {
        coroutineScope.launch {
            childToParentEventSender.sendToParent(
                ChildToParentEvent.SearchEvent.ChangedQuery(query = "")
            )
        }
        updateToInitialOpenState()
    }

    private fun updateToInitialOpenState() {
        val currentState = getCurrentContentState() ?: return
        updateSearchState(
            currentState.copy(
                search = WooPosItemsViewState.Content.SearchState.Visible(
                    state = WooPosSearchInputState.Open(
                        input = WooPosSearchInputState.Open.Input.Hint(
                            resourceProvider.getString(R.string.woopos_search_products)
                        ),
                        isLoading = false,
                    )
                )
            )
        )
    }

    fun getInitialSearchState(isProductsSearchEnabled: Boolean): WooPosItemsViewState.Content.SearchState {
        return when (isProductsSearchEnabled) {
            true -> WooPosItemsViewState.Content.SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )

            false -> WooPosItemsViewState.Content.SearchState.Hidden
        }
    }

    @Suppress("ReturnCount")
    private fun updateLoadingState(isLoading: Boolean) {
        val currentState = getCurrentContentState() ?: return
        val searchState = getCurrentSearchVisibleState() ?: return
        val searchStateValue = getCurrentSearchOpenState() ?: return

        updateSearchState(
            currentState.copy(
                search = searchState.copy(
                    state = searchStateValue.copy(
                        isLoading = isLoading,
                    )
                )
            )
        )
    }

    private fun updateSearchState(newState: WooPosItemsViewState.Content) {
        viewStateFlow.value = newState
    }

    private fun getCurrentContentState(): WooPosItemsViewState.Content? {
        return viewStateFlow.value as? WooPosItemsViewState.Content
    }

    private fun getCurrentSearchVisibleState(): WooPosItemsViewState.Content.SearchState.Visible? {
        val currentState = getCurrentContentState() ?: return null
        return currentState.search as? WooPosItemsViewState.Content.SearchState.Visible
    }

    private fun getCurrentSearchOpenState(): WooPosSearchInputState.Open? {
        val searchState = getCurrentSearchVisibleState() ?: return null
        return searchState.state as? WooPosSearchInputState.Open
    }
}
