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
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

@HiltViewModel
class NewStockNotificationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asLiveData()

    fun onStockNotificationEnabledChanged(type: StockNotificationType, isEnabled: Boolean) {
        _viewState.update {
            when (type) {
                StockNotificationType.LowStock -> it.copy(lowStockNotificationsEnabled = isEnabled)
                StockNotificationType.OutOfStock -> it.copy(outOfStockNotificationsEnabled = isEnabled)
                StockNotificationType.Backorder -> it.copy(backorderNotificationsEnabled = isEnabled)
            }
        }
    }

    fun onEditStoreSettingsClicked() {
        triggerEvent(
            MultiLiveEvent.Event.LaunchUrlInAuthenticatedWebView(
                url = selectedSite.get().adminUrlOrDefault.slashJoin(STOCK_SETTINGS_PATH),
                screenTitle = UiString.UiStringRes(R.string.more_menu_button_wс_admin)
            )
        )
    }

    data class ViewState(
        val lowStockNotificationsEnabled: Boolean = true,
        val outOfStockNotificationsEnabled: Boolean = true,
        val backorderNotificationsEnabled: Boolean = true
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
