package com.woocommerce.android.ui.analytics.informationcard

import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationSectionContract.SectionViewState

sealed class AnalyticsCardInformationContract {
    sealed class AnalyticsCardInformationViewState {
        object HiddenCardViewState : AnalyticsCardInformationViewState()
        data class CardDataViewState(val title: String,
                                     val totalValues: SectionViewState,
                                     val netValues: SectionViewState)
            : AnalyticsCardInformationViewState()
    }
}
