package com.woocommerce.android.ui.woopos.home.toolbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connecting
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Reconnecting
import com.woocommerce.android.cardreader.connection.event.BatteryStatus
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderConnectionController
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderConnectionControllerFactory
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosRemoteReaderSession
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarUIEvent.MenuItemClicked
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarUIEvent.OnCardReaderStatusClicked
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarUIEvent.OnOutsideOfToolbarMenuClicked
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarUIEvent.OnToolbarMenuClicked
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ExitTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.GoToOrdersTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WooPosHomeFloatingToolbarViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val networkStatus: WooPosNetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val remoteReaderSession: WooPosRemoteReaderSession,
    controllerFactory: WooPosCardReaderConnectionControllerFactory,
) : ViewModel() {

    private val controller: WooPosCardReaderConnectionController by lazy {
        controllerFactory.create(viewModelScope)
    }

    private val _state = MutableStateFlow(
        WooPosHomeFloatingToolbarState(
            cardReaderStatus = WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.NotConnected,
            menu = WooPosHomeFloatingToolbarState.Menu.Hidden,
        )
    )
    val state: StateFlow<WooPosHomeFloatingToolbarState> = _state

    init {
        viewModelScope.launch {
            combine(
                cardReaderFacade.readerStatus,
                remoteReaderSession.state,
            ) { bt, remote -> toEffectiveReaderStatus(bt, remote) }
                .distinctUntilChanged()
                .flatMapLatest { effective ->
                    when (effective) {
                        EffectiveReaderStatus.RemoteConnected -> flowOf(
                            WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected(
                                batteryState = WooPosHomeFloatingToolbarState.BatteryState.NOMINAL
                            )
                        )
                        EffectiveReaderStatus.BluetoothConnected ->
                            cardReaderFacade.batteryStatus
                                .map { batteryStatus ->
                                    WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected(
                                        batteryState = mapBatteryState(batteryStatus)
                                    )
                                }
                        EffectiveReaderStatus.Reconnecting -> flowOf(
                            WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Reconnecting
                        )
                        EffectiveReaderStatus.Connecting,
                        EffectiveReaderStatus.Disconnected -> flowOf(
                            WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.NotConnected
                        )
                    }
                }
                .collect { cardReaderStatus ->
                    _state.value = _state.value.copy(cardReaderStatus = cardReaderStatus)
                }
        }
    }

    private fun toEffectiveReaderStatus(
        bt: CardReaderStatus,
        remote: WooPosRemoteReaderSession.State,
    ): EffectiveReaderStatus = when {
        remote is WooPosRemoteReaderSession.State.Connected -> EffectiveReaderStatus.RemoteConnected
        bt is Connected -> EffectiveReaderStatus.BluetoothConnected
        bt is Reconnecting -> EffectiveReaderStatus.Reconnecting
        bt is Connecting || remote is WooPosRemoteReaderSession.State.Connecting ->
            EffectiveReaderStatus.Connecting
        else -> EffectiveReaderStatus.Disconnected
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
                    analyticsTracker.track(ExitTapped)
                }
        }
    }

    private fun hideMenu() {
        _state.value = _state.value.copy(menu = WooPosHomeFloatingToolbarState.Menu.Hidden)
    }

    private fun handleOnCardReaderStatusClicked() {
        when (_state.value.cardReaderStatus) {
            is WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected -> {
                viewModelScope.launch {
                    controller.disconnect()
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
                    viewModelScope.launch {
                        childrenToParentEventSender.sendToParent(ChildToParentEvent.ShowCardReaderConnectionDialog)
                    }
                }
            }

            WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Reconnecting -> {
                cardReaderFacade.cancelReconnection()
            }
        }
    }

    private fun mapBatteryState(status: CardReaderBatteryStatus): WooPosHomeFloatingToolbarState.BatteryState {
        return when (status) {
            is CardReaderBatteryStatus.StatusChanged -> when (status.batteryStatus) {
                BatteryStatus.CRITICAL -> WooPosHomeFloatingToolbarState.BatteryState.CRITICAL
                BatteryStatus.LOW -> WooPosHomeFloatingToolbarState.BatteryState.LOW
                BatteryStatus.NOMINAL, BatteryStatus.UNKNOWN -> WooPosHomeFloatingToolbarState.BatteryState.NOMINAL
            }
            CardReaderBatteryStatus.Warning -> WooPosHomeFloatingToolbarState.BatteryState.LOW
            CardReaderBatteryStatus.Unknown -> WooPosHomeFloatingToolbarState.BatteryState.NOMINAL
        }
    }

    private val toolbarMenuItems by lazy {
        buildList {
            addAll(
                listOf(
                    WooPosHomeFloatingToolbarState.Menu.MenuItem(
                        title = R.string.woopos_orders_title,
                        icon = R.drawable.ic_description_filled_24dp,
                    ),
                    WooPosHomeFloatingToolbarState.Menu.MenuItem(
                        title = R.string.woopos_settings_title,
                        icon = R.drawable.ic_settings_filled_24dp,
                    ),
                    WooPosHomeFloatingToolbarState.Menu.MenuItem(
                        title = R.string.woopos_exit_confirmation_title,
                        icon = R.drawable.ic_exit_to_app_24dp,
                    ),
                )
            )
        }
    }

    private enum class EffectiveReaderStatus {
        RemoteConnected,
        BluetoothConnected,
        Reconnecting,
        Connecting,
        Disconnected,
    }
}
