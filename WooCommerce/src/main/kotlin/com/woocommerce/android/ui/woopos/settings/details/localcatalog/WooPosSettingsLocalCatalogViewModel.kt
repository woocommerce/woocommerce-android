package com.woocommerce.android.ui.woopos.settings.details.localcatalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncRepository
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncScheduler
import com.woocommerce.android.ui.woopos.util.WooPosCellularCapabilityDetector
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.ui.woopos.util.format.WooPosDateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsLocalCatalogViewModel @Inject constructor(
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val localCatalogSyncRepository: WooPosLocalCatalogSyncRepository,
    private val selectedSite: SelectedSite,
    private val dateFormatter: WooPosDateFormatter,
    private val preferencesRepository: WooPosPreferencesRepository,
    private val syncScheduler: WooPosLocalCatalogSyncScheduler,
    private val childToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildEventReceiver: WooPosParentToChildrenEventReceiver,
    private val cellularCapabilityDetector: WooPosCellularCapabilityDetector,
) : ViewModel() {
    private val _state = MutableStateFlow(WooPosSettingsLocalCatalogState())
    val state: StateFlow<WooPosSettingsLocalCatalogState> = _state.asStateFlow()

    init {
        checkCellularCapability()

        loadCatalogStatus()

        listenToCellularDataUpdateValue()

        listenToParentEvents()
    }

    private fun checkCellularCapability() {
        if (!cellularCapabilityDetector.hasCellularCapability()) {
            _state.update { it.copy(cellularCapability = WooPosSettingsLocalCatalogState.CellularCapability.None) }
            viewModelScope.launch {
                preferencesRepository.setAllowCellularDataUpdate(false)
            }
        }
    }

    private fun listenToParentEvents() {
        viewModelScope.launch {
            parentToChildEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.SettingsEvent.RetrySyncRequested -> {
                        runFullCatalogSync()
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun loadCatalogStatus() {
        viewModelScope.launch {
            _state.update { it.copy(catalogStatus = WooPosSettingsLocalCatalogState.CatalogStatus.Loading) }

            val productsLastSyncTimestamp = syncTimestampManager.getProductsLastSyncTimestamp()
            val variationsLastSyncTimestamp = syncTimestampManager.getVariationsLastSyncTimestamp()
            val fullSyncTimestamp = syncTimestampManager.getFullSyncLastCompletedTimestamp()

            val formattedLastSyncTimestamp = dateFormatter.formatCatalogLastUpdate(
                productsLastSyncTimestamp,
                variationsLastSyncTimestamp
            )

            val formattedLastFullSyncTimestamp = dateFormatter.formatCatalogLastFullSync(fullSyncTimestamp)

            val productCount = localCatalogSyncRepository.getProductCount(selectedSite.get())
            val variationCount = localCatalogSyncRepository.getVariationCount(selectedSite.get())

            val catalogStatus = WooPosSettingsLocalCatalogState.CatalogStatus.Available(
                productCount = productCount,
                variationCount = variationCount,
                lastUpdate = formattedLastSyncTimestamp,
                lastFullUpdate = formattedLastFullSyncTimestamp
            )

            _state.update {
                it.copy(catalogStatus = catalogStatus)
            }
        }
    }

    private fun listenToCellularDataUpdateValue() {
        viewModelScope.launch {
            preferencesRepository.allowCellularDataUpdate.collect { allowCellularDataUpdate ->
                _state.update { currentState ->
                    if (cellularCapabilityDetector.hasCellularCapability()) {
                        currentState.copy(
                            cellularCapability = WooPosSettingsLocalCatalogState.CellularCapability.Available(
                                allowCellularDataUpdate = allowCellularDataUpdate
                            )
                        )
                    } else {
                        currentState.copy(
                            cellularCapability = WooPosSettingsLocalCatalogState.CellularCapability.None
                        )
                    }
                }
            }
        }
    }

    fun toggleCellularDataUpdate(newValue: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAllowCellularDataUpdate(newValue)
            syncScheduler.updateWorkConstraints()
        }
    }

    fun runFullCatalogSync() {
        viewModelScope.launch {
            val backupCatalogData =
                (_state.value.catalogStatus as? WooPosSettingsLocalCatalogState.CatalogStatus.Available)

            _state.update { it.copy(catalogStatus = WooPosSettingsLocalCatalogState.CatalogStatus.RefreshingCatalog) }

            val result = localCatalogSyncRepository.syncLocalCatalogFull(selectedSite.get())

            when (result) {
                is PosLocalCatalogSyncResult.Success -> {
                    localCatalogSyncRepository.syncLocalCatalogIncremental(selectedSite.get())
                    loadCatalogStatus()
                    childToParentEventSender.sendToParent(ChildToParentEvent.RefreshProductList)
                }
                is PosLocalCatalogSyncResult.Failure -> {
                    backupCatalogData?.let { _state.update { it.copy(catalogStatus = backupCatalogData) } }
                    childToParentEventSender.sendToParent(
                        ChildToParentEvent.SettingsEvent.ShowSyncErrorDialog(result.error)
                    )
                }
            }
        }
    }
}
