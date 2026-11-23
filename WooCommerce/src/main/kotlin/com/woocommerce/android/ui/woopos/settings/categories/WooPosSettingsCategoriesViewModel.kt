package com.woocommerce.android.ui.woopos.settings.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsCategoriesViewModel @Inject constructor(
    productsDataSource: WooPosProductsDataSource,
    featureFlagRepository: FeatureFlagRepository,
    accessSession: WooPosAccessSession,
) : ViewModel() {
    private val _state = MutableStateFlow(WooPosSettingsCategoriesState(emptyList()))
    val state: StateFlow<WooPosSettingsCategoriesState> = _state.asStateFlow()

    init {
        val categories = WooPosSettingsCategory.entries.filter {
            when (it) {
                WooPosSettingsCategory.LOCAL_CATALOG ->
                    productsDataSource.getCurrentSyncStrategy() != WooPosProductsDataSource.SyncStrategy.REMOTE
                WooPosSettingsCategory.SECURITY -> featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ROLES)
                else -> true
            }
        }
        viewModelScope.launch {
            accessSession.state.collect { session ->
                _state.value = WooPosSettingsCategoriesState(
                    categories.filter {
                        it != WooPosSettingsCategory.SECURITY || session.allows(WooPosCapability.MANAGE_STORE_SETTINGS)
                    }
                )
            }
        }
    }
}
