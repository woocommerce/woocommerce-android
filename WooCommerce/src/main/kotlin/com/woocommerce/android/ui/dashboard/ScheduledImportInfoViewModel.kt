package com.woocommerce.android.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppUrls
import com.woocommerce.android.ui.dashboard.data.AnalyticsScheduledImportRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduledImportInfoViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val scheduledImportRepository: AnalyticsScheduledImportRepository,
) : ScopedViewModel(savedState) {
    private val navArgs = ScheduledImportInfoBottomSheetFragmentArgs.fromSavedStateHandle(savedState)

    private val _viewState = MutableStateFlow(
        ViewState(
            isEnabled = navArgs.isEnabled,
            isUpdating = false,
            hasError = false
        )
    )
    val viewState = _viewState.asLiveData()

    fun onOptionSelected(scheduledEnabled: Boolean) {
        if (_viewState.value.isUpdating) return
        if (scheduledEnabled == _viewState.value.isEnabled) {
            // Tapping the already-selected option keeps the value and simply closes the sheet.
            triggerEvent(SettingUpdated)
            return
        }
        val previous = _viewState.value.isEnabled
        _viewState.update { it.copy(isEnabled = scheduledEnabled, isUpdating = true, hasError = false) }
        launch {
            val result = scheduledImportRepository.setEnabled(scheduledEnabled)
            val updatedValue = result.model
            if (result.isError || updatedValue == null) {
                _viewState.update { it.copy(isEnabled = previous, isUpdating = false, hasError = true) }
            } else {
                _viewState.update { it.copy(isEnabled = updatedValue, isUpdating = false, hasError = false) }
                triggerEvent(SettingUpdated)
            }
        }
    }

    fun onLearnMoreClicked() {
        triggerEvent(Event.LaunchUrlInChromeTab(AppUrls.ANALYTICS_SCHEDULED_IMPORT_DOCS))
    }

    data class ViewState(
        val isEnabled: Boolean,
        val isUpdating: Boolean,
        val hasError: Boolean,
    )

    data object SettingUpdated : Event()
}
