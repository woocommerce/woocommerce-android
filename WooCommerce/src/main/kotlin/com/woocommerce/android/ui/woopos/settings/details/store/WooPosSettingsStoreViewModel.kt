package com.woocommerce.android.ui.woopos.settings.details.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WOO_POS_CIAB_SETTINGS_PATH
import com.woocommerce.android.ui.woopos.common.data.WOO_POS_SETTINGS_PATH
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsStoreViewModel @Inject constructor(
    private val storeRepository: WooPosSettingsStoreRepository,
    private val receiptRepository: WooPosSettingsReceiptRepository,
    private val selectedSite: SelectedSite,
    private val ciabSiteGateKeeper: CIABSiteGateKeeper,
    private val analyticsTracker: WooPosAnalyticsTracker,
) : ViewModel() {
    private val _state = MutableStateFlow(WooPosSettingsStoreState())
    val state: StateFlow<WooPosSettingsStoreState> = _state.asStateFlow()

    private val _openEditReceiptEvent = MutableSharedFlow<EditReceiptTarget>()
    val openEditReceiptEvent: SharedFlow<EditReceiptTarget> = _openEditReceiptEvent.asSharedFlow()

    init {
        loadStoreData()
    }

    fun onEditReceiptClicked() {
        viewModelScope.launch {
            analyticsTracker.track(WooPosAnalyticsEvent.Event.EditReceiptTapped)
            val url = buildReceiptSettingsUrl()
            _openEditReceiptEvent.emit(EditReceiptTarget(url))
        }
    }

    fun onScreenResumed() {
        loadReceiptData()
    }

    private fun buildReceiptSettingsUrl(): String {
        val siteUrl = selectedSite.get().url.trimEnd('/')
        return if (ciabSiteGateKeeper.isCurrentSiteCIAB()) {
            "$siteUrl$WOO_POS_CIAB_SETTINGS_PATH"
        } else {
            "$siteUrl$WOO_POS_SETTINGS_PATH"
        }
    }

    private fun loadStoreData() {
        viewModelScope.launch {
            val storeInfo = storeRepository.getStoreInfo()
            _state.value = _state.value.copy(
                storeInfoState = WooPosSettingsStoreState.StoreState.Loaded(storeInfo)
            )
        }
    }

    private fun loadReceiptData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(receiptState = WooPosSettingsStoreState.ReceiptState.Loading)
            val receiptResult = receiptRepository.getReceiptInfo()
            val newReceiptState = when (receiptResult) {
                is WooPosReceiptDataResult.Success -> {
                    WooPosSettingsStoreState.ReceiptState.Success(receiptResult.receiptInfo)
                }

                is WooPosReceiptDataResult.NotAvailable -> {
                    WooPosSettingsStoreState.ReceiptState.NotSupported
                }

                is WooPosReceiptDataResult.Error -> {
                    WooPosSettingsStoreState.ReceiptState.Error
                }
            }

            _state.value = _state.value.copy(receiptState = newReceiptState)
        }
    }

    data class EditReceiptTarget(val url: String)
}
