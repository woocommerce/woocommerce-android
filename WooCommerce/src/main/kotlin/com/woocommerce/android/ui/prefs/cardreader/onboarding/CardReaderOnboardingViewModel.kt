package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderOnboardingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val tracker: AnalyticsTrackerWrapper,
    private val appPrefs: AppPrefs,
) : ScopedViewModel(savedState) {
    override val _event = SingleLiveEvent<Event>()
    override val event: LiveData<Event> = _event

    private lateinit var cardReaderManager: CardReaderManager
    private lateinit var cardReaderChecker: CardReaderOnboardingChecker

    private val viewState = MutableLiveData<ViewState>()
    val viewStateData: LiveData<ViewState> = viewState

    init {
        startFlow()
    }

    private fun startFlow() {
        // TODO
    }

    private fun onCancelClicked() {
        WooLog.e(WooLog.T.CARD_READER, "Onboarding flow interrupted by the user.")
        exitFlow()
    }

    private fun exitFlow() {
        triggerEvent(Event.Exit)
    }

    sealed class ViewState(
        val onboardingState: CardReaderOnboardingState
    ) {
        open val onPrimaryActionClicked: (() -> Unit)? = null
        open val onSecondaryActionClicked: (() -> Unit)? = null
    }
}
