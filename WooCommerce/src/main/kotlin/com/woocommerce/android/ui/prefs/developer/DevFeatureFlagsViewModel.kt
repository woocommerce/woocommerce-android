package com.woocommerce.android.ui.prefs.developer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.FeatureFlagRepository.FeatureFlagState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevFeatureFlagsViewModel @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var initialValues: Map<FeatureFlag, Boolean> = emptyMap()

    init {
        loadFlagStates()
    }

    private fun loadFlagStates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val states = FeatureFlag.entries.associateWith { flag -> featureFlagRepository.getFlagState(flag) }
            if (initialValues.isEmpty()) initialValues = states.mapValues { it.value.effectiveValue }
            _uiState.update { it.copy(flagStates = states, isLoading = false) }
        }
    }

    fun setOverride(flag: FeatureFlag, state: OverrideState) {
        viewModelScope.launch {
            when (state) {
                OverrideState.DEFAULT -> featureFlagRepository.removeFlagOverride(flag)
                OverrideState.ENABLED -> featureFlagRepository.setFlagOverride(flag, true)
                OverrideState.DISABLED -> featureFlagRepository.setFlagOverride(flag, false)
            }
            val newState = featureFlagRepository.getFlagState(flag)
            val updatedFlagStates = _uiState.value.flagStates + (flag to newState)
            val hasChanges = updatedFlagStates.any { (flag, state) -> state.effectiveValue != initialValues[flag] }
            _uiState.update { currentState ->
                currentState.copy(
                    flagStates = updatedFlagStates,
                    hasChanges = hasChanges
                )
            }
        }
    }

    data class UiState(
        val flagStates: Map<FeatureFlag, FeatureFlagState> = emptyMap(),
        val isLoading: Boolean = true,
        val hasChanges: Boolean = false
    )
}
