package com.woocommerce.android.ui.payments.cardreader.statuschecker

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingParams
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.statuschecker.CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToConnection
import com.woocommerce.android.ui.payments.cardreader.statuschecker.CardReaderStatusCheckerViewModel.StatusCheckerEvent.NavigateToIPPReaderTypeSelection
import com.woocommerce.android.ui.payments.taptopay.IsTapToPayAvailable
import com.woocommerce.android.ui.payments.taptopay.IsTapToPayAvailable.Result.Available
import com.woocommerce.android.ui.payments.taptopay.IsTapToPayAvailable.Result.NotAvailable
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
    private val isTapToPayAvailable: IsTapToPayAvailable,
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
                val cardReaderStatus = cardReaderManager.readerStatus.value
                if (cardReaderStatus is CardReaderStatus.Connected) {
                    triggerEvent(
                        StatusCheckerEvent.NavigateToPayment(
                            param,
                            cardReaderStatus.cardReader.toCardReaderType()
                        )
                    )
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

                    when (val result = isTapToPayAvailable(state.countryCode)) {
                        Available -> triggerEvent(NavigateToIPPReaderTypeSelection(param, state.countryCode))
                        is NotAvailable -> {
                            cardReaderTracker.trackTapToPayNotAvailableReason(result)
                            triggerEvent(NavigateToConnection(param))
                        }
                    }
                } else {
                    triggerEvent(StatusCheckerEvent.NavigateToWelcome(param, state.countryCode))
                }
            }
            else -> triggerEvent(
                StatusCheckerEvent.NavigateToOnboarding(
                    CardReaderOnboardingParams.Failed(param, state)
                )
            )
        }
    }

    private fun CardReader.toCardReaderType() =
        if (ReaderType.isExternalReaderType(type)) {
            CardReaderType.EXTERNAL
        } else {
            CardReaderType.BUILT_IN
        }

    sealed class StatusCheckerEvent : MultiLiveEvent.Event() {
        data class NavigateToWelcome(
            val cardReaderFlowParam: CardReaderFlowParam,
            val countryCode: String,
        ) : MultiLiveEvent.Event()

        data class NavigateToIPPReaderTypeSelection(
            val cardReaderFlowParam: CardReaderFlowParam,
            val countryCode: String,
        ) : MultiLiveEvent.Event()

        data class NavigateToConnection(val cardReaderFlowParam: CardReaderFlowParam) : MultiLiveEvent.Event()

        data class NavigateToPayment(
            val cardReaderFlowParam: CardReaderFlowParam,
            val cardReaderType: CardReaderType,
        ) : MultiLiveEvent.Event()

        data class NavigateToOnboarding(val cardReaderOnboardingParams: CardReaderOnboardingParams) :
            MultiLiveEvent.Event()
    }
}
