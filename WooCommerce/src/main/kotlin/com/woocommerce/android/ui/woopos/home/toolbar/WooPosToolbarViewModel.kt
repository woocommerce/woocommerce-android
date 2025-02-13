package com.woocommerce.android.ui.woopos.home.toolbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppUrls.WOO_POS_DOCUMENTATION_URL
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connecting
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosToolbarUIEvent.MenuItemClicked
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosToolbarUIEvent.OnCardReaderStatusClicked
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosToolbarUIEvent.OnOutsideOfToolbarMenuClicked
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosToolbarUIEvent.OnToolbarMenuClicked
import com.woocommerce.android.ui.woopos.support.WooPosGetSupportFacade
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosToolbarViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val getSupportFacade: WooPosGetSupportFacade,
    private val networkStatus: WooPosNetworkStatus,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(
        WooPosToolbarState(
            cardReaderStatus = WooPosToolbarState.WooPosCardReaderStatus.NotConnected,
            menu = WooPosToolbarState.Menu.Hidden,
        )
    )
    val state: StateFlow<WooPosToolbarState> = _state

    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

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
            R.string.woopos_get_support_title -> getSupportFacade.openSupportForm()
            R.string.woopos_exit_confirmation_title ->
                viewModelScope.launch {
                    childrenToParentEventSender.sendToParent(ChildToParentEvent.ExitPosClicked)
                }
            R.string.woopos_documentation_title -> {
                viewModelScope.launch {
                    _openUrlEvent.emit(WOO_POS_DOCUMENTATION_URL)
                }
            }
        }
    }

    private fun hideMenu() {
        _state.value = _state.value.copy(menu = WooPosToolbarState.Menu.Hidden)
    }

    private fun handleOnCardReaderStatusClicked() {
        when (_state.value.cardReaderStatus) {
            WooPosToolbarState.WooPosCardReaderStatus.Connected -> {
                viewModelScope.launch {
                    cardReaderFacade.disconnectFromReader()
                }
            }
            WooPosToolbarState.WooPosCardReaderStatus.NotConnected -> {
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

    private fun mapCardReaderStatusToUiState(status: CardReaderStatus): WooPosToolbarState.WooPosCardReaderStatus {
        return when (status) {
            is Connected -> WooPosToolbarState.WooPosCardReaderStatus.Connected
            is NotConnected, Connecting -> WooPosToolbarState.WooPosCardReaderStatus.NotConnected
        }
    }

    private companion object {
        val toolbarMenuItems = listOf(
            WooPosToolbarState.Menu.MenuItem(
                title = R.string.woopos_documentation_title,
                icon = R.drawable.woo_pos_info_ic,
            ),
            WooPosToolbarState.Menu.MenuItem(
                title = R.string.woopos_get_support_title,
                icon = R.drawable.woopos_ic_get_support,
            ),
            WooPosToolbarState.Menu.MenuItem(
                title = R.string.woopos_exit_confirmation_title,
                icon = R.drawable.ic_woo_pos_exit,
            ),
        )
    }
}
