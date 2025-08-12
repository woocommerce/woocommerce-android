package com.woocommerce.android.ui.woopos.settings.container

import androidx.lifecycle.ViewModel
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsState
import com.woocommerce.android.ui.woopos.settings.SettingsCategory
import com.woocommerce.android.ui.woopos.settings.SettingsDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsContainerViewModel @Inject constructor() : ViewModel() {
    
    private val _navigationState = MutableStateFlow(WooPosSettingsState())
    val navigationState: StateFlow<WooPosSettingsState> = _navigationState.asStateFlow()
    
    fun onCategorySelected(category: SettingsCategory) {
        _navigationState.update { currentState ->
            val newDetailDestination = when (category) {
                SettingsCategory.HARDWARE -> SettingsDetailDestination.HardwareOverview
            }
            currentState.copy(
                selectedCategory = category,
                detailBackStack = listOf(newDetailDestination)
            )
        }
    }
    
    fun navigateToDetail(destination: SettingsDetailDestination) {
        _navigationState.update { currentState ->
            currentState.copy(
                detailBackStack = currentState.detailBackStack + destination
            )
        }
    }
    
    fun popDetailBackStack() {
        _navigationState.update { currentState ->
            if (currentState.detailBackStack.size > 1) {
                currentState.copy(
                    detailBackStack = currentState.detailBackStack.dropLast(1)
                )
            } else {
                currentState
            }
        }
    }
}