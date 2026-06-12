package com.woocommerce.android.ui.woopos.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventReceiver
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventSender
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategory
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.HardwareTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.HelpTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.LocalCatalogTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SettingsClosed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SettingsOpened
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.StoreDetailsTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsViewModel @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val childToParentEventReceiver: WooPosChildrenToParentEventReceiver,
    private val parentToChildEventSender: WooPosParentToChildrenEventSender,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val _state: MutableStateFlow<WooPosSettingsState> = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = WooPosSettingsState(),
        key = KEY_STATE,
    )
    val state: StateFlow<WooPosSettingsState> = _state

    init {
        listenToChildEvents()
    }

    private fun listenToChildEvents() {
        viewModelScope.launch {
            childToParentEventReceiver.events.collect { event ->
                when (event) {
                    is ChildToParentEvent.SettingsEvent.ShowSyncErrorDialog -> {
                        showSyncErrorDialog(event.errorMessage, event.isServerPermissionsError)
                    }
                    is ChildToParentEvent.SettingsEvent.ShowCardReaderConnectionDialog -> {
                        showCardReaderConnectionDialog()
                    }
                    is ChildToParentEvent.SettingsEvent.ShowCardReaderUpdateDialog -> {
                        showCardReaderUpdateDialog()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onRetrySyncFromDialogClicked() {
        hideDialog()
        viewModelScope.launch {
            parentToChildEventSender.sendToChildren(ParentToChildrenEvent.SettingsEvent.RetrySyncRequested)
        }
    }

    fun onCategorySelected(category: WooPosSettingsCategory) {
        trackCategorySelection(category)
        _state.update { currentState ->
            currentState.copy(
                selectedCategory = category,
                currentDestination = category.rootDestination,
                showingDetail = true,
            )
        }
    }

    fun dismissDetail() {
        _state.update { currentState ->
            currentState.copy(showingDetail = false)
        }
    }

    private fun trackCategorySelection(category: WooPosSettingsCategory) {
        val event = when (category) {
            WooPosSettingsCategory.STORE -> StoreDetailsTapped
            WooPosSettingsCategory.LOCAL_CATALOG -> LocalCatalogTapped
            WooPosSettingsCategory.HARDWARE -> HardwareTapped
            WooPosSettingsCategory.HELP -> HelpTapped
        }
        viewModelScope.launch {
            analyticsTracker.track(event)
        }
    }

    fun navigateToDetail(destination: WooPosSettingsDetailDestination) {
        _state.update { currentState ->
            currentState.copy(currentDestination = destination)
        }
    }

    fun navigateBack() {
        _state.update { currentState ->
            val parentDestination = currentState.currentDestination.parentDestination
            if (parentDestination != null) {
                currentState.copy(currentDestination = parentDestination)
            } else {
                currentState
            }
        }
    }

    fun showProductInfoDialog() {
        _state.update { currentState ->
            currentState.copy(dialogState = WooPosSettingsDialogState.ProductsInfoDialog)
        }
        viewModelScope.launch {
            analyticsTracker.track(WooPosAnalyticsEvent.Event.SimpleProductExplanationDialogShown)
        }
    }

    fun showScanningSetupDialog() {
        _state.update { currentState ->
            currentState.copy(dialogState = WooPosSettingsDialogState.ScanningSetupDialog)
        }
    }

    fun showSyncErrorDialog(errorMessage: String, isServerPermissionsError: Boolean = false) {
        _state.update { currentState ->
            currentState.copy(
                dialogState = WooPosSettingsDialogState.SyncErrorDialog(errorMessage, isServerPermissionsError)
            )
        }
    }

    fun showCardReaderConnectionDialog() {
        _state.update { currentState ->
            currentState.copy(dialogState = WooPosSettingsDialogState.CardReaderConnectionDialog)
        }
    }

    fun showCardReaderUpdateDialog() {
        _state.update { currentState ->
            currentState.copy(dialogState = WooPosSettingsDialogState.CardReaderUpdateDialog)
        }
    }

    fun hideDialog() {
        _state.update { currentState ->
            currentState.copy(dialogState = WooPosSettingsDialogState.Hidden)
        }
    }

    fun onSettingsOpened() {
        viewModelScope.launch {
            analyticsTracker.track(SettingsOpened)
        }
    }

    fun onSettingsClosed() {
        viewModelScope.launch {
            analyticsTracker.track(SettingsClosed)
        }
    }

    private companion object {
        const val KEY_STATE = "woo_pos_settings_view_state"
    }
}
