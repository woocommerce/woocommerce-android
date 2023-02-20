package com.woocommerce.android.ui.login.storecreation.domainpicker

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.common.domain.DomainSuggestionsRepository
import com.woocommerce.android.ui.common.domain.DomainSuggestionsViewModel
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DomainPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    domainSuggestionsRepository: DomainSuggestionsRepository,
    analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    currencyFormatter: CurrencyFormatter,
    private val newStore: NewStore
) : DomainSuggestionsViewModel(savedStateHandle, domainSuggestionsRepository, currencyFormatter) {
    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_DOMAIN_PICKER
            )
        )
    }

    override val helpOrigin = HelpOrigin.STORE_CREATION

    override fun onDomainSuggestionSelected(clickedDomain: String) {
        super.onDomainSuggestionSelected(clickedDomain)
        newStore.update(domain = clickedDomain)
    }
}
