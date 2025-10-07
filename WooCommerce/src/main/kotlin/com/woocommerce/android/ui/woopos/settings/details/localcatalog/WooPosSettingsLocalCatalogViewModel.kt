package com.woocommerce.android.ui.woopos.settings.details.localcatalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsLocalCatalogViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(WooPosSettingsLocalCatalogState())
    val state: StateFlow<WooPosSettingsLocalCatalogState> = _state.asStateFlow()

    init {
        loadCatalogStatus()
    }

    private fun loadCatalogStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // TODO: Replace with actual repository call to fetch catalog status
            delay(1000) // Simulate network call
            
            val mockStatus = CatalogStatus(
                catalogSize = "8.3 MB",
                lastUpdate = "5 minutes ago",
                lastFullUpdate = "Today at 9:15 AM"
            )
            
            _state.update {
                it.copy(
                    catalogStatus = mockStatus,
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

    fun refreshCatalog() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            
            // TODO: Trigger actual catalog refresh through repository
            delay(3000) // Simulate refresh operation
            
            // Update with new status after refresh
            val updatedStatus = CatalogStatus(
                catalogSize = "8.5 MB",
                lastUpdate = "Just now",
                lastFullUpdate = "Just now"
            )
            
            _state.update {
                it.copy(
                    catalogStatus = updatedStatus,
                    isRefreshing = false
                )
            }
        }
    }
}