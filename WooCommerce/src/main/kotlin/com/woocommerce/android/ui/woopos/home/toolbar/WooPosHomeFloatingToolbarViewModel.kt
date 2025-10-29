package com.woocommerce.android.ui.woopos.home.toolbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connecting
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.featureflags.WooPosHistoricalOrdersM1Enabled
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarUIEvent.MenuItemClicked
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarUIEvent.OnCardReaderStatusClicked
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarUIEvent.OnOutsideOfToolbarMenuClicked
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarUIEvent.OnToolbarMenuClicked
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.GoToOrdersTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ExitTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosHomeFloatingToolbarViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val networkStatus: WooPosNetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val wooPosHistoricalOrdersM1Enabled: WooPosHistoricalOrdersM1Enabled,
) : ViewModel() {
    private val _state = MutableStateFlow(
        WooPosHomeFloatingToolbarState(
            cardReaderStatus = WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.NotConnected,
            menu = WooPosHomeFloatingToolbarState.Menu.Hidden,
        )
    )
    val state: StateFlow<WooPosHomeFloatingToolbarState> = _state

    init {
        viewModelScope.launch {
            cardReaderFacade.readerStatus.collect {
                _state.value = _state.value.copy(
                    cardReaderStatus = mapCardReaderStatusToUiState(it)
                )
            }
        }
    }

    fun onUiEvent(event: WooPosHomeFloatingToolbarUIEvent) {
        val currentState = _state.value
        if (currentState.menu is WooPosHomeFloatingToolbarState.Menu.Visible && event !is MenuItemClicked) {
            hideMenu()
            return
        }

        when (event) {
            is OnToolbarMenuClicked -> {
                _state.value = currentState.copy(
                    menu = WooPosHomeFloatingToolbarState.Menu.Visible(toolbarMenuItems)
                )
            }

            OnCardReaderStatusClicked -> handleOnCardReaderStatusClicked()

            is MenuItemClicked -> handleMenuItemClicked(event)

            is OnOutsideOfToolbarMenuClicked -> {
                // Do nothing as the menu is hidden already, but we need to pass the event here anyway
            }
        }
    }

    private fun handleMenuItemClicked(event: MenuItemClicked) {
        hideMenu()

        when (event.menuItem.title) {
            R.string.woopos_orders_title -> {
                viewModelScope.launch {
                    childrenToParentEventSender.sendToParent(ChildToParentEvent.NavigationEvent.ToOrders)
                    analyticsTracker.track(GoToOrdersTapped)
                }
            }
            R.string.woopos_settings_title -> {
                viewModelScope.launch {
                    childrenToParentEventSender.sendToParent(ChildToParentEvent.NavigationEvent.ToSettings)
                }
            }

            R.string.woopos_exit_confirmation_title ->
                viewModelScope.launch {
                    childrenToParentEventSender.sendToParent(ChildToParentEvent.ExitPosClicked)
                }
        }
    }

    private fun hideMenu() {
        _state.value = _state.value.copy(menu = WooPosHomeFloatingToolbarState.Menu.Hidden)
    }

    private fun handleOnCardReaderStatusClicked() {
        when (_state.value.cardReaderStatus) {
            WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected -> {
                viewModelScope.launch {
                    cardReaderFacade.disconnectFromReader()
                }
            }

            WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.NotConnected -> {
                if (!networkStatus.isConnected()) {
                    viewModelScope.launch {
                        childrenToParentEventSender.sendToParent(
                            ChildToParentEvent.ToastMessageDisplayed(
                                message = resourceProvider.getString(R.string.woopos_no_internet_message)
                            )
                        )
                    }
                } else {
                    cardReaderFacade.connectToReader()
                }
            }
        }
    }

    private fun mapCardReaderStatusToUiState(status: CardReaderStatus) = when (status) {
        is Connected -> WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected
        is NotConnected, Connecting -> WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.NotConnected
    }

    private val toolbarMenuItems by lazy {
        buildList {
            if (wooPosHistoricalOrdersM1Enabled()) {
                add(
                    WooPosHomeFloatingToolbarState.Menu.MenuItem(
                        title = R.string.woopos_orders_title,
                        icon = Icons.Default.Description,
                    )
                )
            }

            addAll(
                listOf(
                    WooPosHomeFloatingToolbarState.Menu.MenuItem(
                        title = R.string.woopos_settings_title,
                        icon = Icons.Default.Settings,
                    ),
                    WooPosHomeFloatingToolbarState.Menu.MenuItem(
                        title = R.string.woopos_exit_confirmation_title,
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                    ),
                )
            )
        }
    }
}
