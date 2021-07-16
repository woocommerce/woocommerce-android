package com.woocommerce.android.ui.prefs.cardreader.connect.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderEligibilityViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val viewState = MutableLiveData<ViewState>(ViewState.CountryNotSupportedState("US"))
    val viewStateData: LiveData<ViewState> = viewState

    sealed class ViewState(
        val headerLabel: UiString? = null,
        @DrawableRes val illustration: Int? = null,
        val hintLabel: UiString? = null,
        val contactSupportLabel: UiString? = null,
        val learnMoreLabel: UiString? = null
    ) {
        data class CountryNotSupportedState(
            val countryCode: String,
        ) : ViewState(
            headerLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_eligibility_country_not_supported_header,
                params = listOf(UiString.UiStringText(" HC: United States"))
            ),
            illustration = R.drawable.img_products_error,
            hintLabel = UiString.UiStringRes(
               stringRes = R.string.card_reader_eligibility_country_not_supported_hint
            ),
            contactSupportLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_eligibility_country_not_supported_contact_support,
                containsHtml = true
            ),
            learnMoreLabel = UiString.UiStringRes(
                stringRes = R.string.card_reader_eligibility_country_not_supported_learn_more,
                containsHtml = true
            )
        )
    }
}
