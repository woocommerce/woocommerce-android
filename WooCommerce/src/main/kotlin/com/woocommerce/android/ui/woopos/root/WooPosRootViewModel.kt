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
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.MenuItemSelected
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.OnBackFromHomeClicked
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.OnSuccessfulPayment
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
        hideMenu()

        when (event) {
            ConnectToAReaderClicked -> handleConnectToReaderButtonClicked()
            ExitConfirmationDialogDismissed -> {
                _rootScreenState.value = _rootScreenState.value.copy(exitConfirmationDialog = null)
            }

            OnBackFromHomeClicked -> {
                _rootScreenState.value = _rootScreenState.value.copy(
                    exitConfirmationDialog = WooPosRootScreenState.WooPosExitConfirmationDialog
                )
            }

            is OnSuccessfulPayment -> TODO()

            is MenuItemSelected -> {
            }

            is OnToolbarMenuClicked -> {
                _rootScreenState.value = _rootScreenState.value.copy(
                    menu = WooPosRootScreenState.Menu.Visible(items = toolbarMenuItems)
                )
            }

            is WooPosRootUIEvent.OnOutsideOfToolbarMenuClicked -> {
                // Do nothing as the menu is already hidden by any UI event
            }
        }
    }

    private fun hideMenu() {
        if (_rootScreenState.value.menu !is WooPosRootScreenState.Menu.Visible) return

        _rootScreenState.value = _rootScreenState.value.copy(
            menu = WooPosRootScreenState.Menu.Hidden
        )
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
                id = 0,
                title = R.string.woopos_get_support_title,
                icon = R.drawable.woopos_ic_get_support,
            ),
            MenuItem(
                id = 1,
                title = R.string.woopos_exit_confirmation_title,
                icon = R.drawable.woopos_ic_exit_pos,
            ),
        )
    }
}
