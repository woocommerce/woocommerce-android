package com.woocommerce.android.ui.pushnotifications.connection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPushNotificationsConnectionStepsViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    private val _viewState = MutableLiveData(
        ViewState(siteAddress = getSiteAddress())
    )
    val viewState: LiveData<ViewState> = _viewState

    init {
        startConnectStoreStep()
    }

    private fun startConnectStoreStep() {
        val currentState = _viewState.value ?: return
        _viewState.value = currentState.copy(
            steps = currentState.steps.mapIndexed { index, step ->
                if (index == 0) {
                    step.copy(status = ConnectionStepStatus.IN_PROGRESS)
                } else {
                    step
                }
            }
        )
    }

    private fun getSiteAddress(): String {
        val site = selectedSite.getOrNull() ?: return ""
        return StringUtils.getSiteDomainAndPath(site).ifBlank { site.name.orEmpty() }
    }

    data class ViewState(
        val siteAddress: String,
        val steps: List<ConnectionStepUiModel> = listOf(
            ConnectionStepUiModel(
                title = R.string.woo_push_notifications_connection_steps_step_connect_store,
                status = ConnectionStepStatus.NOT_STARTED
            ),
            ConnectionStepUiModel(
                title = R.string.woo_push_notifications_connection_steps_step_check_plugin_compatibility,
                status = ConnectionStepStatus.NOT_STARTED,
            ),
            ConnectionStepUiModel(
                title = R.string.woo_push_notifications_connection_steps_step_enable_push_notifications,
                status = ConnectionStepStatus.NOT_STARTED
            )
        )
    )
}
