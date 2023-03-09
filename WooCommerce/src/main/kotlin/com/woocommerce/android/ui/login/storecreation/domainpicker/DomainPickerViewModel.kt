package com.woocommerce.android.ui.login.storecreation.domainpicker

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.common.domain.DomainSuggestionsRepository
import com.woocommerce.android.ui.common.domain.DomainSuggestionsRepository.DomainSuggestion
import com.woocommerce.android.ui.common.domain.DomainSuggestionsViewModel
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DomainPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    domainSuggestionsRepository: DomainSuggestionsRepository,
    analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    currencyFormatter: CurrencyFormatter,
    private val newStore: NewStore
) : DomainSuggestionsViewModel(
    savedStateHandle = savedStateHandle,
    domainSuggestionsRepository = domainSuggestionsRepository,
    currencyFormatter = currencyFormatter,
    initialQuery = savedStateHandle[KEY_INITIAL_QUERY]!!,
    searchOnlyFreeDomains = true,
    isFreeCreditAvailable = false
) {
    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_DOMAIN_PICKER
            )
        )
    }

    override val helpOrigin = HelpOrigin.STORE_CREATION

    override fun navigateToNextStep(selectedDomain: DomainSuggestion) {
        newStore.update(domain = selectedDomain.name)
        triggerEvent(NavigateToNextStep(selectedDomain.name))
    }

    data class NavigateToNextStep(val domain: String) : MultiLiveEvent.Event()
}
