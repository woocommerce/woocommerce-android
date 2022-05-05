package com.woocommerce.android.ui.cardreader.statuschecker

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.ui.cardreader.CardReaderTracker
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingParams
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardReaderStatusCheckerViewModel
@Inject constructor(
    savedState: SavedStateHandle,
    private val cardReaderManager: CardReaderManager,
    private val cardReaderChecker: CardReaderOnboardingChecker,
    private val cardReaderTracker: CardReaderTracker,
    private val appPrefsWrapper: AppPrefsWrapper,
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderStatusCheckerDialogFragmentArgs by savedState.navArgs()

    override val _event = SingleLiveEvent<MultiLiveEvent.Event>()
    override val event: LiveData<MultiLiveEvent.Event> = _event

    init {
        launch {
            checkStatus()
        }
    }

    private suspend fun checkStatus() {
        when (val param = arguments.cardReaderFlowParam) {
            CardReaderFlowParam.CardReadersHub -> triggerEvent(
                StatusCheckerEvent.NavigateToOnboarding(
                    CardReaderOnboardingParams.Check(param)
                )
            )
            is CardReaderFlowParam.PaymentOrRefund -> {
                if (cardReaderManager.readerStatus.value is CardReaderStatus.Connected) {
                    triggerEvent(StatusCheckerEvent.NavigateToPayment(param))
                } else {
                    handleOnboardingStatus(param)
                }
            }
        }.exhaustive
    }

    private suspend fun handleOnboardingStatus(param: CardReaderFlowParam) {
        when (val state = cardReaderChecker.getOnboardingState()) {
            is CardReaderOnboardingState.OnboardingCompleted -> {
                if (appPrefsWrapper.isCardReaderWelcomeDialogShown()) {
                    cardReaderTracker.trackOnboardingState(state)
                    triggerEvent(StatusCheckerEvent.NavigateToConnection(param))
                } else {
                    triggerEvent(StatusCheckerEvent.NavigateToWelcome(param))
                }
            }
            else -> triggerEvent(
                StatusCheckerEvent.NavigateToOnboarding(
                    CardReaderOnboardingParams.Failed(param, state)
                )
            )
        }
    }

    sealed class StatusCheckerEvent : MultiLiveEvent.Event() {
        data class NavigateToWelcome(val cardReaderFlowParam: CardReaderFlowParam) : MultiLiveEvent.Event()
        data class NavigateToConnection(val cardReaderFlowParam: CardReaderFlowParam) : MultiLiveEvent.Event()
        data class NavigateToPayment(val cardReaderFlowParam: CardReaderFlowParam) : MultiLiveEvent.Event()
        data class NavigateToOnboarding(val cardReaderOnboardingParams: CardReaderOnboardingParams) :
            MultiLiveEvent.Event()
    }
}
