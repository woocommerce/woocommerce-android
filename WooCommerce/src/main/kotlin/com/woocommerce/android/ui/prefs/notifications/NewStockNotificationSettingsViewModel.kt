package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.extensions.adminUrlOrDefault
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

@HiltViewModel
class NewStockNotificationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asLiveData()

    init {
        refreshDefaultLowStockThreshold()
    }

    fun onNotificationsEnabledChanged(isEnabled: Boolean) {
        _viewState.update { it.copy(notificationsEnabled = isEnabled) }
    }

    fun onStockNotificationEnabledChanged(type: StockNotificationType, isEnabled: Boolean) {
        _viewState.update {
            when (type) {
                StockNotificationType.LowStock -> it.copy(lowStockNotificationsEnabled = isEnabled)
                StockNotificationType.OutOfStock -> it.copy(outOfStockNotificationsEnabled = isEnabled)
                StockNotificationType.Backorder -> it.copy(backorderNotificationsEnabled = isEnabled)
            }
        }
    }

    fun onStoreSettingsWebViewClosed() {
        refreshDefaultLowStockThreshold()
    }

    fun onEditStoreSettingsClicked() {
        triggerEvent(
            MultiLiveEvent.Event.LaunchUrlInAuthenticatedWebView(
                url = selectedSite.get().adminUrlOrDefault.slashJoin(STOCK_SETTINGS_PATH),
                screenTitle = UiString.UiStringRes(R.string.more_menu_button_wс_admin)
            )
        )
    }

    private fun refreshDefaultLowStockThreshold() {
        launch {
            val productSettings = wooCommerceStore.fetchSiteProductSettings(selectedSite.get()).model

            productSettings?.defaultLowStockThreshold?.let { threshold ->
                updateDefaultLowStockThreshold(threshold)
            }
        }
    }

    private fun updateDefaultLowStockThreshold(threshold: Int) {
        if (_viewState.value.defaultLowStockThreshold != threshold) {
            _viewState.update { it.copy(defaultLowStockThreshold = threshold) }
        }
    }

    data class ViewState(
        val notificationsEnabled: Boolean = true,
        val lowStockNotificationsEnabled: Boolean = true,
        val outOfStockNotificationsEnabled: Boolean = true,
        val backorderNotificationsEnabled: Boolean = true,
        val defaultLowStockThreshold: Int = 5
    )

    enum class StockNotificationType {
        LowStock,
        OutOfStock,
        Backorder
    }

    companion object {
        private const val STOCK_SETTINGS_PATH = "admin.php?page=wc-settings&tab=products&section=inventory"
    }
}
