package com.woocommerce.android.ui.prefs.domain

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.getTitle
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.domain.DomainSuggestionsRepository.DomainSuggestion
import com.woocommerce.android.ui.prefs.domain.DomainSuggestionsRepository.DomainSuggestion.Paid
import com.woocommerce.android.ui.prefs.domain.DomainSuggestionsRepository.DomainSuggestion.Premium
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DomainSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    domainSuggestionsRepository: DomainSuggestionsRepository,
    currencyFormatter: CurrencyFormatter,
    selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val domainChangeRepository: DomainChangeRepository
) : DomainSuggestionsViewModel(
    savedStateHandle = savedStateHandle,
    domainSuggestionsRepository = domainSuggestionsRepository,
    currencyFormatter = currencyFormatter,
    initialQuery = selectedSite.get().getTitle(""),
    searchOnlyFreeDomains = false,
    isFreeCreditAvailable = savedStateHandle[KEY_IS_FREE_CREDIT_AVAILABLE]!!,
    freeUrl = savedStateHandle[KEY_FREE_DOMAIN_URL]
) {
    companion object {
        const val KEY_FREE_DOMAIN_URL = "freeDomainUrl"
    }
    override val helpOrigin = HelpOrigin.DOMAIN_CHANGE

    private val navArgs: DomainSearchFragmentArgs by savedStateHandle.navArgs()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.CUSTOM_DOMAINS_STEP,
            mapOf(
                AnalyticsTracker.KEY_SOURCE to appPrefsWrapper.getCustomDomainsSourceAsString(),
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_PICKER
            )
        )
    }

    override fun navigateToNextStep(selectedDomain: DomainSuggestion) {
        when (selectedDomain) {
            is Premium -> {
                createShoppingCart(selectedDomain.name, selectedDomain.productId, selectedDomain.supportsPrivacy)
            }
            is Paid -> {
                if (navArgs.isFreeCreditAvailable) {
                    triggerEvent(NavigateToDomainRegistration(selectedDomain.name, selectedDomain.productId))
                } else {
                    createShoppingCart(selectedDomain.name, selectedDomain.productId, selectedDomain.supportsPrivacy)
                }
            }
            else -> throw UnsupportedOperationException("This domain search is only for paid domains")
        }
    }

    private fun createShoppingCart(domain: String, productId: Int, supportsPrivacy: Boolean) {
        launch {
            val result = domainChangeRepository.addDomainToCart(productId, domain, supportsPrivacy)

            if (!result.isError) {
                triggerEvent(ShowCheckoutWebView(domain))
            } else {
                triggerEvent(
                    ShowUiStringSnackbar(UiStringText(result.error.message ?: "Unable to create a shopping cart"))
                )
                triggerEvent(Exit)

                analyticsTrackerWrapper.track(
                    AnalyticsEvent.CUSTOM_DOMAIN_PURCHASE_FAILED,
                    mapOf(
                        AnalyticsTracker.KEY_SOURCE to appPrefsWrapper.getCustomDomainsSourceAsString(),
                        AnalyticsTracker.KEY_USE_DOMAIN_CREDIT to false,
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to result.error.type.name,
                        AnalyticsTracker.KEY_ERROR_DESC to result.error.message
                    )
                )
            }
        }
    }

    data class NavigateToDomainRegistration(val domain: String, val productId: Int) : MultiLiveEvent.Event()
    data class ShowCheckoutWebView(val domain: String) : MultiLiveEvent.Event()
}
