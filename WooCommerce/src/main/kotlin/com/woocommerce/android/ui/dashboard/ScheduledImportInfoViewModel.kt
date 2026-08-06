package com.woocommerce.android.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.ui.dashboard.data.AnalyticsScheduledImportRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduledImportInfoViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val scheduledImportRepository: AnalyticsScheduledImportRepository,
) : ScopedViewModel(savedState) {
    private var confirmedIsEnabled: Boolean
        get() = savedState[IS_ENABLED_KEY] ?: false
        set(value) {
            savedState[IS_ENABLED_KEY] = value
        }

    private val _viewState = MutableStateFlow(
        ViewState(
            isVisible = savedState[IS_VISIBLE_KEY] ?: false,
            isEnabled = confirmedIsEnabled,
            isUpdating = false,
            hasError = false
        )
    )
    val viewState = _viewState.asStateFlow()

    private var updateJob: Job? = null

    fun show(isEnabled: Boolean) {
        if (_viewState.value.isUpdating) {
            updateViewState { it.copy(isVisible = true) }
            return
        }

        confirmedIsEnabled = isEnabled
        updateViewState {
            it.copy(
                isVisible = true,
                isEnabled = isEnabled,
                hasError = false,
            )
        }
    }

    fun onDismissed() {
        updateJob?.cancel()
        updateJob = null
        updateViewState {
            it.copy(
                isVisible = false,
                isEnabled = confirmedIsEnabled,
                isUpdating = false,
                hasError = false,
            )
        }
    }

    fun onOptionSelected(scheduledEnabled: Boolean) {
        if (_viewState.value.isUpdating) return
        if (scheduledEnabled == _viewState.value.isEnabled) {
            // Tapping the already-selected option keeps the value and simply closes the sheet.
            triggerEvent(SettingUpdated)
            return
        }
        updateViewState { it.copy(isEnabled = scheduledEnabled, isUpdating = true, hasError = false) }
        updateJob = launch {
            val result = scheduledImportRepository.setEnabled(scheduledEnabled)
            currentCoroutineContext().ensureActive()
            val updatedValue = result.model
            if (result.isError || updatedValue == null) {
                updateViewState {
                    it.copy(isEnabled = confirmedIsEnabled, isUpdating = false, hasError = true)
                }
            } else {
                confirmedIsEnabled = updatedValue
                updateViewState { it.copy(isEnabled = updatedValue, isUpdating = false, hasError = false) }
                triggerEvent(SettingUpdated)
            }
        }
    }

    fun onLearnMoreClicked() {
        triggerEvent(Event.LaunchUrlInChromeTab(AppUrls.ANALYTICS_SCHEDULED_IMPORT_DOCS))
    }

    private fun updateViewState(transform: (ViewState) -> ViewState) {
        _viewState.update(transform)
        savedState[IS_VISIBLE_KEY] = _viewState.value.isVisible
    }

    data class ViewState(
        val isVisible: Boolean,
        val isEnabled: Boolean,
        val isUpdating: Boolean,
        val hasError: Boolean,
    )

    data object SettingUpdated : Event()

    private companion object {
        const val IS_VISIBLE_KEY = "scheduledImportInfoIsVisible"
        const val IS_ENABLED_KEY = "scheduledImportInfoIsEnabled"
    }
}
