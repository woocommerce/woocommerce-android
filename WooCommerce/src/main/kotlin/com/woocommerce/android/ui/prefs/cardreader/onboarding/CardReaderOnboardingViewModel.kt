package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.annotation.LayoutRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderOnboardingViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    override val _event = SingleLiveEvent<Event>()
    override val event: LiveData<Event> = _event

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
        @LayoutRes
        fun getLayoutRes(): Int {
            return when (onboardingState) {
                else -> R.layout.fragment_card_reader_onboarding_loading
            }
        }
    }
}
