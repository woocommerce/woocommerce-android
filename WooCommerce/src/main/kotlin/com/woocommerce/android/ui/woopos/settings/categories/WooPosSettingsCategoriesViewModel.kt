package com.woocommerce.android.ui.woopos.settings.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import com.woocommerce.android.util.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsCategoriesViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<WooPosSettingsCategoriesState> = _state.asStateFlow()
    private fun createInitialState(): WooPosSettingsCategoriesState {
        val allCategories = WooPosSettingsCategory.entries
        val visibleCategories = if (FeatureFlag.WOO_POS_LOCAL_CATALOG_M1.isEnabled(context)) {
            allCategories
        } else {
            allCategories.filter { it != WooPosSettingsCategory.LOCAL_CATALOG }
        }
        return WooPosSettingsCategoriesState(categories = visibleCategories)
    }
}
