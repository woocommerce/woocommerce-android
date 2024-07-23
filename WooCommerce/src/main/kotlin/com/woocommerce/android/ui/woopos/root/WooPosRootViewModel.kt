package com.woocommerce.android.ui.woopos.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connecting
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.root.WooPosRootScreenState.Menu.MenuItem
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.ConnectToAReaderClicked
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.ExitConfirmationDialogDismissed
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.MenuItemClicked
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.OnBackFromHomeClicked
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.OnToolbarMenuClicked
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosRootViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade,
) : ViewModel() {
    private val _rootScreenState = MutableStateFlow(
        WooPosRootScreenState(
            cardReaderStatus = WooPosRootScreenState.WooPosCardReaderStatus.NotConnected,
            menu = WooPosRootScreenState.Menu.Hidden,
            exitConfirmationDialog = null,
        )
    )
    val rootScreenState: StateFlow<WooPosRootScreenState> = _rootScreenState

    init {
        viewModelScope.launch {
            cardReaderFacade.readerStatus.collect {
                _rootScreenState.value = _rootScreenState.value.copy(
                    cardReaderStatus = mapCardReaderStatusToUiState(it)
                )
            }
        }
    }

    fun onUiEvent(event: WooPosRootUIEvent) {
        val currentState = _rootScreenState.value
        if (currentState.menu is WooPosRootScreenState.Menu.Visible && event !is MenuItemClicked) {
            hideMenu()
            return
        }

        when (event) {
            is OnToolbarMenuClicked -> {
                _rootScreenState.value = currentState.copy(
                    menu = WooPosRootScreenState.Menu.Visible(toolbarMenuItems)
                )
            }

            ConnectToAReaderClicked -> handleConnectToReaderButtonClicked()

            ExitConfirmationDialogDismissed -> _rootScreenState.value = currentState.copy(
                exitConfirmationDialog = null
            )

            OnBackFromHomeClicked -> _rootScreenState.value = currentState.copy(
                exitConfirmationDialog = WooPosRootScreenState.WooPosExitConfirmationDialog
            )

            is MenuItemClicked -> handleMenuItemClicked(event)

            is WooPosRootUIEvent.OnOutsideOfToolbarMenuClicked -> {
                // Do nothing as the menu is hidden already, but we need to pass the event here anyway
            }
        }
    }

    private fun handleMenuItemClicked(event: MenuItemClicked) {
        hideMenu()

        when (event.menuItem.title) {
            R.string.woopos_get_support_title -> TODO()
            R.string.woopos_exit_confirmation_title ->
                _rootScreenState.value = _rootScreenState.value.copy(
                    exitConfirmationDialog = WooPosRootScreenState.WooPosExitConfirmationDialog
                )
        }
    }

    private fun hideMenu() {
        _rootScreenState.value = _rootScreenState.value.copy(menu = WooPosRootScreenState.Menu.Hidden)
    }

    private fun handleConnectToReaderButtonClicked() {
        if (_rootScreenState.value.cardReaderStatus != WooPosRootScreenState.WooPosCardReaderStatus.Connected) {
            cardReaderFacade.connectToReader()
        }
    }

    private fun mapCardReaderStatusToUiState(status: CardReaderStatus): WooPosRootScreenState.WooPosCardReaderStatus {
        return when (status) {
            is Connected -> WooPosRootScreenState.WooPosCardReaderStatus.Connected
            is NotConnected, Connecting -> WooPosRootScreenState.WooPosCardReaderStatus.NotConnected
        }
    }

    private companion object {
        val toolbarMenuItems = listOf(
            MenuItem(
                title = R.string.woopos_get_support_title,
                icon = R.drawable.woopos_ic_get_support,
            ),
            MenuItem(
                title = R.string.woopos_exit_confirmation_title,
                icon = R.drawable.woopos_ic_exit_pos,
            ),
        )
    }
}
