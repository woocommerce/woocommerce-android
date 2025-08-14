package com.woocommerce.android.ui.woopos.settings

import androidx.lifecycle.ViewModel
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsViewModel @Inject constructor() : ViewModel() {
    private val _navigationState = MutableStateFlow(WooPosSettingsState())
    val navigationState: StateFlow<WooPosSettingsState> = _navigationState.asStateFlow()

    fun onCategorySelected(category: WooPosSettingsCategory) {
        _navigationState.update { currentState ->
            currentState.copy(
                selectedCategory = category,
                currentDestination = category.rootDestination
            )
        }
    }

    fun navigateToDetail(destination: WooPosSettingsDetailDestination) {
        _navigationState.update { currentState ->
            currentState.copy(currentDestination = destination)
        }
    }

    fun navigateBack() {
        _navigationState.update { currentState ->
            val parentDestination = currentState.currentDestination.parentDestination
            if (parentDestination != null) {
                currentState.copy(currentDestination = parentDestination)
            } else {
                currentState
            }
        }
    }
}
