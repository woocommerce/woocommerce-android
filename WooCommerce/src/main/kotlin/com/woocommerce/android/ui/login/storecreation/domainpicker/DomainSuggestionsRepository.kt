package com.woocommerce.android.ui.login.storecreation.domainpicker

import com.woocommerce.android.WooException
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.dispatchAndAwait
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class DomainSuggestionsRepository @Inject constructor(
    private val dispatcher: Dispatcher,
) {
    companion object {
        private const val SUGGESTIONS_REQUEST_COUNT = 20
    }

    val domainSuggestions: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())

    suspend fun fetchDomainSuggestions(domainQuery: String): Result<Unit> {
        val domainSuggestionsPayload = SiteStore.SuggestDomainsPayload(
            domainQuery,
            onlyWordpressCom = true,
            includeWordpressCom = false,
            includeDotBlogSubdomain = false,
            SUGGESTIONS_REQUEST_COUNT,
            includeVendorDot = false
        )
        dispatcher.dispatchAndAwait<SiteStore.SuggestDomainsPayload, SiteStore.OnSuggestedDomains>(
            SiteActionBuilder.newSuggestDomainsAction(domainSuggestionsPayload)
        )
            .let { domainSuggestionsEvent ->
                return if (domainSuggestionsEvent.isError) {
                    WooLog.w(
                        WooLog.T.LOGIN,
                        "Error fetching domain suggestions: ${domainSuggestionsEvent.error.message}"
                    )
                    Result.failure(
                        WooException(
                            WooError(
                                WooErrorType.API_ERROR,
                                GenericErrorType.UNKNOWN,
                                domainSuggestionsEvent.error.message
                            )
                        )
                    )
                } else {
                    WooLog.w(WooLog.T.LOGIN, "Domain suggestions loaded successfully")
                    domainSuggestions.update {
                        domainSuggestionsEvent.suggestions
                            .filter { it.is_free }
                            .map { it.domain_name }
                    }
                    Result.success(Unit)
                }
            }
    }
}
