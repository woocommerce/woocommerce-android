package com.woocommerce.android.ui.woopos.settings.categories

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsCategoriesViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(WooPosSettingsCategoriesState())
    val state: StateFlow<WooPosSettingsCategoriesState> = _state.asStateFlow()
}
