package com.woocommerce.android.ui.prefs.developer

import androidx.lifecycle.SavedStateHandle
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
    private val featureFlagRepository: FeatureFlagRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val skipRemoteLoad: Boolean = savedStateHandle[SKIP_REMOTE_LOAD_KEY] ?: false

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var initialValues: Map<FeatureFlag, Boolean> = emptyMap()

    init {
        viewModelScope.launch {
            if (!skipRemoteLoad) {
                featureFlagRepository.awaitRemoteFlagsLoaded()
            }
            loadFlagStates()
        }
    }

    companion object {
        const val SKIP_REMOTE_LOAD_KEY = "skip_remote_load"
    }

    private fun loadFlagStates() {
        val states = FeatureFlag.entries.associateWith { flag -> featureFlagRepository.getFlagState(flag) }
        if (initialValues.isEmpty()) initialValues = states.mapValues { it.value.effectiveValue }
        _uiState.update {
            it.copy(
                flagStates = states,
                hasChanges = states.any { (flag, state) ->
                    state.effectiveValue != initialValues[flag]
                }
            )
        }
    }

    fun setOverride(flag: FeatureFlag, state: OverrideState) {
        when (state) {
            OverrideState.DEFAULT -> featureFlagRepository.removeFlagOverride(flag)
            OverrideState.ENABLED -> featureFlagRepository.setFlagOverride(flag, true)
            OverrideState.DISABLED -> featureFlagRepository.setFlagOverride(flag, false)
        }

        loadFlagStates()
    }

    data class UiState(
        val flagStates: Map<FeatureFlag, FeatureFlagState> = emptyMap(),
        val hasChanges: Boolean = false
    )
}
