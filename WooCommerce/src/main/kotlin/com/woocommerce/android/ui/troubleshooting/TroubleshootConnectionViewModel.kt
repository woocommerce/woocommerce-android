package com.woocommerce.android.ui.troubleshooting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CONNECTIVITY_TEST
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.NotStarted
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.troubleshooting.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreProductsCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.WPComConnectionCheckUseCase
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TroubleshootConnectionViewModel @Inject constructor(
    private val internetConnectionCheck: InternetConnectionCheckUseCase,
    private val wpComConnectionCheck: WPComConnectionCheckUseCase,
    private val storeConnectionCheck: StoreConnectionCheckUseCase,
    private val storeOrdersCheck: StoreOrdersCheckUseCase,
    private val storeProductsCheck: StoreProductsCheckUseCase,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val selectedSite: SelectedSite,
    private val featureFlagRepository: FeatureFlagRepository,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val isAppPasswordSite: Boolean
        get() = selectedSite.connectionType == SiteConnectionType.ApplicationPasswords

    private val initialChecks = buildList {
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
        val areChecksFinished = checks.isFinished()
        val isAiSupportChatAvailable = isAiSupportChatAvailable()

        ViewState(
            checks = checks,
            shouldDisplaySummary = checks.all { it.status is Success },
            shouldDisplayAiSupportChatButton = areChecksFinished && isAiSupportChatAvailable,
            shouldDisplayContactSupportButton = areChecksFinished && !isAiSupportChatAvailable
        )
    }.distinctUntilChanged().asLiveData()

    val isCheckFinished = checksFlow.map { checks ->
        checks.isFinished()
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

    fun onAiSupportChatClicked() {
        if (!checksFlow.value.isFinished()) return
        triggerEvent(OpenAiSupportChat(checks = checksFlow.value))
    }

    fun onReturnClicked() {
        triggerEvent(Exit)
    }

    fun onRetryClicked(type: ConnectivityCheckType) {
        checksFlow.update { checks ->
            checks.map {
                if (it.type == type) it.copy(status = NotStarted) else it
            }
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
        while (true) {
            val checks = checksFlow.value
            if (checks.any { it.status is Failure }) return
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
            }

            val finalStatus = checksFlow.value.first { it.type == nextCheck.type }.status
            if (finalStatus !is Success) {
                return
            }
        }
    }

    private fun updateCheckStatus(type: ConnectivityCheckType, status: ConnectivityCheckStatus) {
        checksFlow.update { checks ->
            checks.map {
                if (it.type == type) it.copy(status = status) else it
            }
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
                    is Failure -> {
                        val errorName = status.error?.name ?: "Failed"
                        val details = status.technicalDetails?.let { "\n$it" } ?: ""
                        errorName + details
                    }
                    else -> "Unknown"
                }
                appendLine("Result: $resultStr")
                appendLine()
            }
        }.trimEnd()
    }

    private fun isAiSupportChatAvailable(): Boolean =
        featureFlagRepository.isEnabled(FeatureFlag.AI_SUPPORT_CHAT) &&
            selectedSite.getIfExists()?.isJetpackConnected == true

    private fun List<ConnectivityCheckCardData>.isFinished(): Boolean =
        any { it.status is Failure } || all { it.status is Success }

    data class OpenSupportRequest(val diagnosticLog: String?) : MultiLiveEvent.Event()
    data class OpenWebView(val url: String) : MultiLiveEvent.Event()
    data class OpenAiSupportChat(val checks: List<ConnectivityCheckCardData>) : MultiLiveEvent.Event()

    data class ViewState(
        val checks: List<ConnectivityCheckCardData>,
        val shouldDisplaySummary: Boolean,
        val shouldDisplayAiSupportChatButton: Boolean,
        val shouldDisplayContactSupportButton: Boolean
    )

    companion object {
        const val jetpackTroubleshootingUrl =
            "https://jetpack.com/support/reconnecting-reinstalling-jetpack/"
        const val genericTroubleshootingUrl =
            "https://woocommerce.com/document/android-ios-apps-troubleshooting-error-fetching-orders/"
    }
}
