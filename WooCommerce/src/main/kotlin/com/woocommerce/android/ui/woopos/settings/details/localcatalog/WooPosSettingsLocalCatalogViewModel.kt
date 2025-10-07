package com.woocommerce.android.ui.woopos.settings.details.localcatalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncRepository
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
) : ViewModel() {
    private val _state = MutableStateFlow(WooPosSettingsLocalCatalogState())
    val state: StateFlow<WooPosSettingsLocalCatalogState> = _state.asStateFlow()

    init {
        loadCatalogStatus()
    }

    private fun loadCatalogStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Get timestamps for products and variations
            val productsTimestamp = syncTimestampManager.getProductsLastSyncTimestamp()
            val variationsTimestamp = syncTimestampManager.getVariationsLastSyncTimestamp()

            // Format timestamps for display
            val formattedTimestamp = dateFormatter.formatCatalogLastUpdate(
                productsTimestamp,
                variationsTimestamp
            )

            val catalogStatus = CatalogStatus(
                catalogSize = "8.3 MB", // TODO: Replace with actual catalog size
                lastUpdate = formattedTimestamp,
                lastFullUpdate = formattedTimestamp // TODO: Replace with full sync timestamp
            )

            _state.update {
                it.copy(
                    catalogStatus = catalogStatus,
                    isLoading = false
                )
            }
        }
    }

    fun toggleCellularDataUpdate() {
        viewModelScope.launch {
            _state.update {
                it.copy(allowCellularDataUpdate = !it.allowCellularDataUpdate)
            }

            // TODO: Save preference to shared preferences or data store
        }
    }

    fun runFullCatalogSync() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            val result = localCatalogSyncRepository.syncLocalCatalogFull(selectedSite.get())

            when (result) {
                is PosLocalCatalogSyncResult.Success -> {
                    loadCatalogStatus()
                }
                is PosLocalCatalogSyncResult.Failure -> {
                    // TODO: Handle errors
                }
            }

            _state.update { it.copy(isRefreshing = false) }
        }
    }
}
