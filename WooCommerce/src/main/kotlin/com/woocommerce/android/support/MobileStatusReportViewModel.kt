package com.woocommerce.android.support

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.zendesk.MobileStatusProvider
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shows the same Mobile Status Report that is attached to support tickets, so it can be read without filing one.
 */
@HiltViewModel
class MobileStatusReportViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val mobileStatusProvider: MobileStatusProvider,
    private val selectedSite: SelectedSite,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState) {
    private val _viewState = MutableStateFlow(ViewState(isLoading = true))
    val viewState = _viewState.asStateFlow()

    init {
        launch {
            val report = mobileStatusProvider(selectedSite.getOrNull())
            _viewState.update { it.copy(report = report, isLoading = false) }
        }
    }

    fun onShareButtonClicked() {
        _viewState.value.report.takeIf { it.isNotEmpty() }?.let { triggerEvent(ShareStatusReport(it)) }
    }

    fun onCopyButtonClicked() {
        _viewState.value.report.takeIf { it.isNotEmpty() }?.let {
            analyticsTrackerWrapper.track(AnalyticsEvent.SUPPORT_MOBILE_STATUS_REPORT_COPY_BUTTON_TAPPED)
            triggerEvent(CopyStatusReport(it))
        }
    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    data class ViewState(
        val report: String = "",
        val isLoading: Boolean = false
    )
}

data class CopyStatusReport(val text: String) : MultiLiveEvent.Event()
data class ShareStatusReport(val text: String) : MultiLiveEvent.Event()
