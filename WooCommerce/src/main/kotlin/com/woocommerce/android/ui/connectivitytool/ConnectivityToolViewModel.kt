package com.woocommerce.android.ui.connectivitytool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CONNECTIVITY_TEST
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.NotStarted
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.connectivitytool.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.connectivitytool.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.connectivitytool.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.connectivitytool.useCases.StoreProductsCheckUseCase
import com.woocommerce.android.ui.connectivitytool.useCases.WPComConnectionCheckUseCase
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ConnectivityToolViewModel @Inject constructor(
    private val internetConnectionCheck: InternetConnectionCheckUseCase,
    private val wpComConnectionCheck: WPComConnectionCheckUseCase,
    private val storeConnectionCheck: StoreConnectionCheckUseCase,
    private val storeOrdersCheck: StoreOrdersCheckUseCase,
    private val storeProductsCheck: StoreProductsCheckUseCase,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val selectedSite: SelectedSite,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val isAppPasswordSite: Boolean
        get() = selectedSite.connectionType == SiteConnectionType.ApplicationPasswords

    private val initialChecks = ArrayList<ConnectivityCheckCardData>().apply {
        add(ConnectivityCheckCardData(ConnectivityCheckType.INTERNET))
        if (!isAppPasswordSite) {
            add(ConnectivityCheckCardData(ConnectivityCheckType.WP_COM))
        }
        add(ConnectivityCheckCardData(ConnectivityCheckType.STORE))
        add(ConnectivityCheckCardData(ConnectivityCheckType.ORDERS))
        add(ConnectivityCheckCardData(ConnectivityCheckType.PRODUCTS))
    }

    private val checksFlow = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = initialChecks,
        key = "checksFlow"
    )

    val viewState = checksFlow.map { checks ->
        ViewState(
            checks = checks,
            shouldDisplaySummary = checks.all { it.status is Success }
        )
    }.distinctUntilChanged().asLiveData()

    val isCheckFinished = checksFlow.map { checks ->
        checks.all { it.status is Success || it.status is Failure }
    }.distinctUntilChanged().asLiveData()

    private val _technicalDetailsToShow = MutableStateFlow<String?>(null)
    val technicalDetailsToShow = _technicalDetailsToShow.asLiveData()

    fun onViewTechnicalDetailsClicked(details: String) {
        analyticsTrackerWrapper.track(AnalyticsEvent.CONNECTIVITY_TOOL_TECHNICAL_DETAILS_TAPPED)
        _technicalDetailsToShow.value = details
    }

    fun onTechnicalDetailsDismissed() {
        _technicalDetailsToShow.value = null
    }

    fun startConnectionChecks() {
        launch {
            executeNextCheck()
        }
    }

    fun onContactSupportClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.CONNECTIVITY_TOOL_CONTACT_SUPPORT_TAPPED)
        triggerEvent(OpenSupportRequest(diagnosticLog = generateDiagnosticLog()))
    }

    fun onReturnClicked() {
        triggerEvent(Exit)
    }

    fun onRetryClicked(type: ConnectivityCheckType) {
        checksFlow.update { checks ->
            checks.map {
                if (it.type == type) it.copy(status = NotStarted) else it
            }.toCollection(ArrayList())
        }
        launch {
            executeNextCheck()
        }
    }

    fun onReadMoreClicked(failureType: FailureType) {
        analyticsTrackerWrapper.track(AnalyticsEvent.CONNECTIVITY_TOOL_READ_MORE_TAPPED)
        when (failureType) {
            FailureType.JETPACK -> triggerEvent(OpenWebView(jetpackTroubleshootingUrl))
            else -> triggerEvent(OpenWebView(genericTroubleshootingUrl))
        }
    }

    private suspend fun executeNextCheck() {
        val checks = checksFlow.value
        val nextCheck = checks.firstOrNull { it.status is NotStarted || it.status is InProgress } ?: return

        val flow = when (nextCheck.type) {
            ConnectivityCheckType.INTERNET -> internetConnectionCheck()
            ConnectivityCheckType.WP_COM -> wpComConnectionCheck()
            ConnectivityCheckType.STORE -> storeConnectionCheck()
            ConnectivityCheckType.ORDERS -> storeOrdersCheck()
            ConnectivityCheckType.PRODUCTS -> storeProductsCheck()
        }

        flow.collect { status ->
            trackChanges(status, nextCheck.type.analyticsValue)
            updateCheckStatus(nextCheck.type, status)
            if (status is Success) {
                executeNextCheck()
            }
        }
    }

    private fun updateCheckStatus(type: ConnectivityCheckType, status: ConnectivityCheckStatus) {
        checksFlow.update { checks ->
            checks.map {
                if (it.type == type) it.copy(status = status) else it
            }.toCollection(ArrayList())
        }
    }

    private fun trackChanges(status: ConnectivityCheckStatus, type: String) {
        if (status is InProgress || status is NotStarted) return

        analyticsTrackerWrapper.track(
            AnalyticsEvent.CONNECTIVITY_TOOL_REQUEST_RESPONSE,
            mapOf(
                AnalyticsTracker.KEY_SUCCESS to (status is Success),
                KEY_CONNECTIVITY_TEST to type,
                AnalyticsTracker.KEY_TIME_TAKEN to status.durationMs
            )
        )
    }

    private fun generateDiagnosticLog(): String? {
        val completedChecks = checksFlow.value.filter {
            it.status is Success || it.status is Failure
        }

        if (completedChecks.isEmpty()) return null

        return buildString {
            completedChecks.forEachIndexed { index, check ->
                appendLine("## ${index + 1}. ${check.type.operationName}")
                appendLine("Took: ${check.status.durationMs}ms")
                val resultStr = when (val status = check.status) {
                    is Success -> "Success"
                    is Failure -> buildString {
                        append(status.error?.name ?: "Failed")
                        status.technicalDetails?.let { details ->
                            append("\n$details")
                        }
                    }
                    else -> "Unknown"
                }
                appendLine("Result: $resultStr")
                appendLine()
            }
        }.trimEnd()
    }

    data class OpenSupportRequest(val diagnosticLog: String?) : MultiLiveEvent.Event()
    data class OpenWebView(val url: String) : MultiLiveEvent.Event()

    data class ViewState(
        val checks: List<ConnectivityCheckCardData>,
        val shouldDisplaySummary: Boolean
    )

    companion object {
        const val jetpackTroubleshootingUrl =
            "https://jetpack.com/support/reconnecting-reinstalling-jetpack/"
        const val genericTroubleshootingUrl =
            "https://woocommerce.com/document/android-ios-apps-troubleshooting-error-fetching-orders/"
    }
}
