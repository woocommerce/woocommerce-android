package com.woocommerce.android.ui.woopos.settings.categories

import androidx.lifecycle.ViewModel
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsCategoriesViewModel @Inject constructor(
    productsDataSource: WooPosProductsDataSource,
) : ViewModel() {
    private val _state = MutableStateFlow(WooPosSettingsCategoriesState(emptyList()))
    val state: StateFlow<WooPosSettingsCategoriesState> = _state.asStateFlow()

    init {
        val categories = WooPosSettingsCategory.entries.filter {
            when (productsDataSource.getCurrentSyncStrategy()) {
                WooPosProductsDataSource.SyncStrategy.REMOTE -> it != WooPosSettingsCategory.LOCAL_CATALOG
                WooPosProductsDataSource.SyncStrategy.LOCAL_CATALOG -> true
            }
        }
        _state.value = WooPosSettingsCategoriesState(categories)
    }
}
