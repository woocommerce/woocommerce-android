package com.woocommerce.android.ui.woopos.home.toolbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connecting
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosToolbarUIEvent.ConnectToAReaderClicked
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosToolbarUIEvent.MenuItemClicked
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosToolbarUIEvent.OnOutsideOfToolbarMenuClicked
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosToolbarUIEvent.OnToolbarMenuClicked
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosToolbarViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
) : ViewModel() {
    private val _state = MutableStateFlow(
        WooPosToolbarState(
            cardReaderStatus = WooPosToolbarState.WooPosCardReaderStatus.NotConnected,
            menu = WooPosToolbarState.Menu.Hidden,
        )
    )
    val state: StateFlow<WooPosToolbarState> = _state

    init {
        viewModelScope.launch {
            cardReaderFacade.readerStatus.collect {
                _state.value = _state.value.copy(
                    cardReaderStatus = mapCardReaderStatusToUiState(it)
                )
            }
        }
    }

    fun onUiEvent(event: WooPosToolbarUIEvent) {
        val currentState = _state.value
        if (currentState.menu is WooPosToolbarState.Menu.Visible && event !is MenuItemClicked) {
            hideMenu()
            return
        }

        when (event) {
            is OnToolbarMenuClicked -> {
                _state.value = currentState.copy(
                    menu = WooPosToolbarState.Menu.Visible(toolbarMenuItems)
                )
            }

            ConnectToAReaderClicked -> handleConnectToReaderButtonClicked()

            is MenuItemClicked -> handleMenuItemClicked(event)

            is OnOutsideOfToolbarMenuClicked -> {
                // Do nothing as the menu is hidden already, but we need to pass the event here anyway
            }
        }
    }

    private fun handleMenuItemClicked(event: MenuItemClicked) {
        hideMenu()

        when (event.menuItem.title) {
            R.string.woopos_get_support_title -> TODO()
            R.string.woopos_exit_confirmation_title ->
                viewModelScope.launch {
                    childrenToParentEventSender.sendToParent(ChildToParentEvent.ExitPosClicked)
                }
        }
    }

    private fun hideMenu() {
        _state.value = _state.value.copy(menu = WooPosToolbarState.Menu.Hidden)
    }

    private fun handleConnectToReaderButtonClicked() {
        if (_state.value.cardReaderStatus != WooPosToolbarState.WooPosCardReaderStatus.Connected) {
            cardReaderFacade.connectToReader()
        }
    }

    private fun mapCardReaderStatusToUiState(status: CardReaderStatus): WooPosToolbarState.WooPosCardReaderStatus {
        return when (status) {
            is Connected -> WooPosToolbarState.WooPosCardReaderStatus.Connected
            is NotConnected, Connecting -> WooPosToolbarState.WooPosCardReaderStatus.NotConnected
        }
    }

    private companion object {
        val toolbarMenuItems = listOf(
            WooPosToolbarState.Menu.MenuItem(
                title = R.string.woopos_get_support_title,
                icon = R.drawable.woopos_ic_get_support,
            ),
            WooPosToolbarState.Menu.MenuItem(
                title = R.string.woopos_exit_confirmation_title,
                icon = R.drawable.woopos_ic_exit_pos,
            ),
        )
    }
}
