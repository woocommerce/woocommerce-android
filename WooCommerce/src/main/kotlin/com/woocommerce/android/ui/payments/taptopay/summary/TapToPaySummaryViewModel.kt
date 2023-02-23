package com.woocommerce.android.ui.payments.taptopay.summary

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class TapToPaySummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _uiState = MutableStateFlow(UiState)
    val uiState: LiveData<UiState> = _uiState.asLiveData()

    fun onTryPaymentClicked() {
    }

    fun onBackClicked() {
    }

    fun onLearnMoreClicked() {
    }

    object UiState {
        @StringRes
        const val screenTitleText = R.string.card_reader_tap_to_pay_explanation_screen_title
        @StringRes
        const val titleText = R.string.card_reader_tap_to_pay_explanation_title
        @DrawableRes
        const val illustration = R.drawable.img_card_reader
        @StringRes
        const val explanationOneText = R.string.card_reader_tap_to_pay_explanation_one
        @StringRes
        const val explanationTwoText = R.string.card_reader_tap_to_pay_explanation_two
        @StringRes
        const val explanationThreeText = R.string.card_reader_tap_to_pay_explanation_three
        @StringRes
        const val buttonText = R.string.card_reader_tap_to_pay_explanation_try_payment
        @StringRes
        const val learnMoreText = R.string.card_reader_tap_to_pay_explanation_learn_more
    }
}
