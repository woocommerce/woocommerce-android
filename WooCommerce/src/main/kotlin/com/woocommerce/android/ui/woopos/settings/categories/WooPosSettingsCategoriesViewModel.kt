package com.woocommerce.android.ui.woopos.settings.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIsLocalCatalogSupported
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsCategoriesViewModel @Inject constructor(
    selectedSite: SelectedSite,
    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported,
) : ViewModel() {
    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<WooPosSettingsCategoriesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = WooPosSettingsCategory.entries.filter {
                if (!isLocalCatalogSupported(selectedSite.get().localId())) {
                    it != WooPosSettingsCategory.LOCAL_CATALOG
                } else {
                    true
                }
            }
            _state.value = WooPosSettingsCategoriesState(categories)
        }
    }

    private fun createInitialState(): WooPosSettingsCategoriesState {
        return WooPosSettingsCategoriesState(
            categories = WooPosSettingsCategory.entries.filter { it != WooPosSettingsCategory.LOCAL_CATALOG }
        )
    }
}
