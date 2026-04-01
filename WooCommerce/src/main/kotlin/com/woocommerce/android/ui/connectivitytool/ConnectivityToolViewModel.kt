package com.woocommerce.android.ui.connectivitytool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CONNECTIVITY_TEST
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CONNECTIVITY_INTERNET
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CONNECTIVITY_ORDERS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CONNECTIVITY_PRODUCTS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CONNECTIVITY_SITE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CONNECTIVITY_WP_COM
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckCardData.InternetConnectivityCheckData
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckCardData.StoreConnectivityCheckData
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckCardData.StoreOrdersConnectivityCheckData
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckCardData.StoreProductsConnectivityCheckData
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckCardData.WPComConnectivityCheckData
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.NotStarted
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.connectivitytool.ConnectivityToolViewModel.ConnectivityCheckStep.Finished
import com.woocommerce.android.ui.connectivitytool.ConnectivityToolViewModel.ConnectivityCheckStep.InternetCheck
import com.woocommerce.android.ui.connectivitytool.ConnectivityToolViewModel.ConnectivityCheckStep.StoreCheck
import com.woocommerce.android.ui.connectivitytool.ConnectivityToolViewModel.ConnectivityCheckStep.StoreOrdersCheck
import com.woocommerce.android.ui.connectivitytool.ConnectivityToolViewModel.ConnectivityCheckStep.StoreProductsCheck
import com.woocommerce.android.ui.connectivitytool.ConnectivityToolViewModel.ConnectivityCheckStep.WPComCheck
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private val stateMachine = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = InternetCheck
    )

    private val internetCheckFlow = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = InternetConnectivityCheckData(
            retryConnectionAction = { handleRetryConnectionClick(InternetCheck) }
        )
    )

    private val wpComCheckFlow = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = WPComConnectivityCheckData(
            retryConnectionAction = { handleRetryConnectionClick(WPComCheck) }
        )
    )

    private val storeCheckFlow = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = StoreConnectivityCheckData(
            retryConnectionAction = { handleRetryConnectionClick(StoreCheck) }
        )
    )

    private val ordersCheckFlow = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = StoreOrdersConnectivityCheckData(
            retryConnectionAction = { handleRetryConnectionClick(StoreOrdersCheck) }
        )
    )

    private val productsCheckFlow = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = StoreProductsConnectivityCheckData(
            retryConnectionAction = { handleRetryConnectionClick(StoreProductsCheck) }
        )
    )

    val viewState = combine(
        internetCheckFlow,
        wpComCheckFlow,
        storeCheckFlow,
        ordersCheckFlow,
        productsCheckFlow
    ) { internet, wpCom, store, orders, products ->
        ViewState(
            internetCheckData = internet,
            wpComCheckData = wpCom,
            storeCheckData = store,
            ordersCheckData = orders,
            productsCheckData = products,
            isWPComCheckVisible = !isAppPasswordSite
        )
    }.distinctUntilChanged().asLiveData()

    val isCheckFinished = stateMachine.map { it == Finished }.asLiveData()

    private val _technicalDetailsToShow = MutableStateFlow<String?>(null)
    val technicalDetailsToShow = _technicalDetailsToShow.asLiveData()

    fun onViewTechnicalDetailsClicked(details: String) {
        analyticsTrackerWrapper.track(AnalyticsEvent.CONNECTIVITY_TOOL_TECHNICAL_DETAILS_TAPPED)
        _technicalDetailsToShow.value = details
    }

    fun onTechnicalDetailsDismissed() {
        _technicalDetailsToShow.value = null
    }

    private val nextStep
        get() = when (stateMachine.value) {
            InternetCheck -> if (isAppPasswordSite) StoreCheck else WPComCheck
            WPComCheck -> StoreCheck
            StoreCheck -> StoreOrdersCheck
            StoreOrdersCheck -> StoreProductsCheck
            StoreProductsCheck -> Finished
            Finished -> error("Cannot move to next state from Finished")
        }

    fun startConnectionChecks() {
        launch {
            stateMachine.collect {
                when (it) {
                    InternetCheck -> startInternetCheck()
                    WPComCheck -> startWPComCheck()
                    StoreCheck -> startStoreCheck()
                    StoreOrdersCheck -> startStoreOrdersCheck()
                    StoreProductsCheck -> startProductsCheck()
                    Finished -> { /* No-op */ }
                }
            }
        }
    }

    fun onContactSupportClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.CONNECTIVITY_TOOL_CONTACT_SUPPORT_TAPPED)
        triggerEvent(OpenSupportRequest(diagnosticLog = generateDiagnosticLog()))
    }

    fun onReturnClicked() {
        triggerEvent(Exit)
    }

    private fun handleRetryConnectionClick(step: ConnectivityCheckStep) {
        when (step) {
            InternetCheck -> internetCheckFlow.update { it.copy(connectivityCheckStatus = NotStarted) }
            WPComCheck -> wpComCheckFlow.update { it.copy(connectivityCheckStatus = NotStarted) }
            StoreCheck -> storeCheckFlow.update { it.copy(connectivityCheckStatus = NotStarted) }
            StoreOrdersCheck -> ordersCheckFlow.update { it.copy(connectivityCheckStatus = NotStarted) }
            StoreProductsCheck -> productsCheckFlow.update { it.copy(connectivityCheckStatus = NotStarted) }
            Finished -> { /* No-op */ }
        }
        stateMachine.update { step }
    }

    private fun handleReadMoreClick(failureType: FailureType) {
        analyticsTrackerWrapper.track(AnalyticsEvent.CONNECTIVITY_TOOL_READ_MORE_TAPPED)
        when (failureType) {
            FailureType.JETPACK -> triggerEvent(OpenWebView(jetpackTroubleshootingUrl))
            else -> triggerEvent(OpenWebView(genericTroubleshootingUrl))
        }
    }

    private fun startInternetCheck() {
        internetConnectionCheck().onEach { status ->
            trackChanges(status, VALUE_CONNECTIVITY_INTERNET)
            status.startNextCheck()
            internetCheckFlow.update { it.copy(connectivityCheckStatus = status) }
        }.launchIn(viewModelScope)
    }

    private fun startWPComCheck() {
        wpComConnectionCheck().onEach { status ->
            trackChanges(status, VALUE_CONNECTIVITY_WP_COM)
            status.startNextCheck()
            wpComCheckFlow.update { it.copy(connectivityCheckStatus = status) }
        }.launchIn(viewModelScope)
    }

    private fun startStoreCheck() {
        storeConnectionCheck().onEach { status ->
            trackChanges(status, VALUE_CONNECTIVITY_SITE)
            status.startNextCheck()
            storeCheckFlow.update {
                if (status is Failure) {
                    it.copy(
                        connectivityCheckStatus = status,
                        readMoreAction = { handleReadMoreClick(status.error ?: FailureType.GENERIC) }
                    )
                } else {
                    it.copy(connectivityCheckStatus = status)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun startStoreOrdersCheck() {
        storeOrdersCheck().onEach { status ->
            trackChanges(status, VALUE_CONNECTIVITY_ORDERS)
            status.startNextCheck()
            ordersCheckFlow.update {
                if (status is Failure) {
                    it.copy(
                        connectivityCheckStatus = status,
                        readMoreAction = { handleReadMoreClick(status.error ?: FailureType.GENERIC) }
                    )
                } else {
                    it.copy(connectivityCheckStatus = status)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun startProductsCheck() {
        storeProductsCheck().onEach { status ->
            trackChanges(status, VALUE_CONNECTIVITY_PRODUCTS)
            status.startNextCheck()
            productsCheckFlow.update {
                if (status is Failure) {
                    it.copy(
                        connectivityCheckStatus = status,
                        readMoreAction = { handleReadMoreClick(status.error ?: FailureType.GENERIC) }
                    )
                } else {
                    it.copy(connectivityCheckStatus = status)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun ConnectivityCheckStatus.startNextCheck() {
        if (stateMachine.value == Finished) return

        stateMachine.update {
            when (this) {
                is Success -> nextStep
                is Failure -> Finished
                else -> it
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
        val currentState = viewState.value ?: return null
        val checks = buildList {
            add(InternetConnectionCheckUseCase.OPERATION_NAME to currentState.internetCheckData.connectivityCheckStatus)
            if (currentState.isWPComCheckVisible) {
                add(WPComConnectionCheckUseCase.OPERATION_NAME to currentState.wpComCheckData.connectivityCheckStatus)
            }
            add(StoreConnectionCheckUseCase.OPERATION_NAME to currentState.storeCheckData.connectivityCheckStatus)
            add(StoreOrdersCheckUseCase.OPERATION_NAME to currentState.ordersCheckData.connectivityCheckStatus)
            add(StoreProductsCheckUseCase.OPERATION_NAME to currentState.productsCheckData.connectivityCheckStatus)
        }.filter { it.second is Success || it.second is Failure }

        return buildString {
            appendLine("Connectivity Test Log")
            appendLine("====================")
            appendLine()
            checks.forEachIndexed { index, (name, status) ->
                val statusStr = when (status) {
                    is Success -> "SUCCESS"
                    is Failure -> "FAILED"
                    else -> "UNKNOWN"
                }
                appendLine("${index + 1}. $name: $statusStr (${status.durationMs}ms)")
                if (status is Failure) {
                    status.error?.let { appendLine("   Error: ${it.name}") }
                    status.technicalDetails?.let { details ->
                        details.lines().forEach { line -> appendLine("   $line") }
                    }
                }
            }
        }.trimEnd()
    }

    data class OpenSupportRequest(val diagnosticLog: String?) : MultiLiveEvent.Event()
    data class OpenWebView(val url: String) : MultiLiveEvent.Event()

    data class ViewState(
        val internetCheckData: InternetConnectivityCheckData,
        val wpComCheckData: WPComConnectivityCheckData,
        val storeCheckData: StoreConnectivityCheckData,
        val ordersCheckData: StoreOrdersConnectivityCheckData,
        val productsCheckData: StoreProductsConnectivityCheckData,
        val isWPComCheckVisible: Boolean
    ) {
        val shouldDisplaySummary: Boolean
            get() = internetCheckData.connectivityCheckStatus is Success &&
                (wpComCheckData.connectivityCheckStatus is Success || !isWPComCheckVisible) &&
                storeCheckData.connectivityCheckStatus is Success &&
                ordersCheckData.connectivityCheckStatus is Success &&
                productsCheckData.connectivityCheckStatus is Success
    }

    enum class ConnectivityCheckStep {
        InternetCheck,
        WPComCheck,
        StoreCheck,
        StoreOrdersCheck,
        StoreProductsCheck,
        Finished
    }

    companion object {
        const val jetpackTroubleshootingUrl =
            "https://jetpack.com/support/reconnecting-reinstalling-jetpack/"
        const val genericTroubleshootingUrl =
            "https://woocommerce.com/document/android-ios-apps-troubleshooting-error-fetching-orders/"
    }
}
