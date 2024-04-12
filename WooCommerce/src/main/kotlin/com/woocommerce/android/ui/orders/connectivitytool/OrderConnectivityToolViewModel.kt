package com.woocommerce.android.ui.orders.connectivitytool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_INTERNET
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_JETPACK_TUNNEL
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_SITE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_WP_COM
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.InternetConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.StoreConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.StoreOrdersConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.WordPressConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.NotStarted
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStep.Finished
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStep.InternetCheck
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStep.StoreCheck
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStep.StoreOrdersCheck
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStep.WordPressCheck
import com.woocommerce.android.ui.orders.connectivitytool.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.WordPressConnectionCheckUseCase
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderConnectivityToolViewModel @Inject constructor(
    private val internetConnectionCheck: InternetConnectionCheckUseCase,
    private val wordPressConnectionCheck: WordPressConnectionCheckUseCase,
    private val storeConnectionCheck: StoreConnectionCheckUseCase,
    private val storeOrdersCheck: StoreOrdersCheckUseCase,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
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

    private val wordpressCheckFlow = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = WordPressConnectivityCheckData(
            retryConnectionAction = { handleRetryConnectionClick(WordPressCheck) }
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

    val viewState = combine(
        internetCheckFlow,
        wordpressCheckFlow,
        storeCheckFlow,
        ordersCheckFlow
    ) { internet, wordpress, store, orders ->
        ViewState(internet, wordpress, store, orders)
    }.distinctUntilChanged().asLiveData()

    val isCheckFinished = stateMachine.map { it == Finished }.asLiveData()

    private val nextStep
        get() = when (stateMachine.value) {
            InternetCheck -> WordPressCheck
            WordPressCheck -> StoreCheck
            StoreCheck -> StoreOrdersCheck
            StoreOrdersCheck -> Finished
            Finished -> error("Cannot move to next state from Finished")
        }

    fun startConnectionChecks() {
        launch {
            stateMachine.collect {
                when (it) {
                    InternetCheck -> startInternetCheck()
                    WordPressCheck -> startWordPressCheck()
                    StoreCheck -> startStoreCheck()
                    StoreOrdersCheck -> startStoreOrdersCheck()
                    Finished -> { /* No-op */ }
                }
            }
        }
    }

    fun onContactSupportClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.CONNECTIVITY_TOOL_CONTACT_SUPPORT_TAPPED)
        triggerEvent(OpenSupportRequest)
    }

    fun onReturnClicked() {
        triggerEvent(Exit)
    }

    private fun handleRetryConnectionClick(step: ConnectivityCheckStep) {
        when (step) {
            InternetCheck -> internetCheckFlow.update { it.copy(connectivityCheckStatus = NotStarted) }
            WordPressCheck -> wordpressCheckFlow.update { it.copy(connectivityCheckStatus = NotStarted) }
            StoreCheck -> storeCheckFlow.update { it.copy(connectivityCheckStatus = NotStarted) }
            StoreOrdersCheck -> ordersCheckFlow.update { it.copy(connectivityCheckStatus = NotStarted) }
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
        val startTime = System.currentTimeMillis()
        internetConnectionCheck().onEach { status ->
            trackChanges(status, VALUE_INTERNET, startTime)
            status.startNextCheck()
            internetCheckFlow.update { it.copy(connectivityCheckStatus = status) }
        }.launchIn(viewModelScope)
    }

    private fun startWordPressCheck() {
        val startTime = System.currentTimeMillis()
        wordPressConnectionCheck().onEach { status ->
            trackChanges(status, VALUE_WP_COM, startTime)
            status.startNextCheck()
            wordpressCheckFlow.update { it.copy(connectivityCheckStatus = status) }
        }.launchIn(viewModelScope)
    }

    private fun startStoreCheck() {
        val startTime = System.currentTimeMillis()
        storeConnectionCheck().onEach { status ->
            trackChanges(status, VALUE_SITE, startTime)
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
        val startTime = System.currentTimeMillis()
        storeOrdersCheck().onEach { status ->
            trackChanges(status, VALUE_JETPACK_TUNNEL, startTime)
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

    private fun trackChanges(
        status: ConnectivityCheckStatus,
        type: String,
        startTime: Long
    ) {
        if (status is InProgress || status is NotStarted) return

        analyticsTrackerWrapper.track(
            AnalyticsEvent.CONNECTIVITY_TOOL_REQUEST_RESPONSE,
            mapOf(
                AnalyticsTracker.KEY_SUCCESS to (status is Success),
                AnalyticsTracker.KEY_TYPE to type,
                AnalyticsTracker.KEY_TIME_TAKEN to (System.currentTimeMillis() - startTime)
            )
        )
    }

    object OpenSupportRequest : MultiLiveEvent.Event()
    data class OpenWebView(val url: String) : MultiLiveEvent.Event()

    data class ViewState(
        val internetCheckData: InternetConnectivityCheckData,
        val wordPressCheckData: WordPressConnectivityCheckData,
        val storeCheckData: StoreConnectivityCheckData,
        val ordersCheckData: StoreOrdersConnectivityCheckData
    ) {
        val shouldDisplaySummary: Boolean
            get() = internetCheckData.connectivityCheckStatus is Success &&
                wordPressCheckData.connectivityCheckStatus is Success &&
                storeCheckData.connectivityCheckStatus is Success &&
                ordersCheckData.connectivityCheckStatus is Success
    }

    enum class ConnectivityCheckStep {
        InternetCheck,
        WordPressCheck,
        StoreCheck,
        StoreOrdersCheck,
        Finished
    }

    companion object {
        const val jetpackTroubleshootingUrl =
            "https://jetpack.com/support/reconnecting-reinstalling-jetpack/"
        const val genericTroubleshootingUrl =
            "https://woocommerce.com/document/android-ios-apps-troubleshooting-error-fetching-orders/"
    }
}
