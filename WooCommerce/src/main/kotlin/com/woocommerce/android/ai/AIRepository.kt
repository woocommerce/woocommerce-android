package com.woocommerce.android.ai

import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsResponse
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import java.lang.Exception
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepository @Inject constructor(
    private val jetpackAIStore: JetpackAIStore
) {
    companion object {
        const val PRODUCT_SHARING_FEATURE = "woo_android_share_product"
        const val PRODUCT_DESCRIPTION_FEATURE = "woo_android_product_description"
        const val ORDER_DETAIL_THANK_YOU_NOTE = "woo_android_order_detail_thank_you_note"
        const val PRODUCT_SALES_PRICE_ADVICE = "woo_android_product_sales_price_advice"
    }

    suspend fun generateProductSharingText(
        site: SiteModel,
        productName: String,
        permalink: String,
        productDescription: String?,
        languageISOCode: String = "en"
    ): Result<String> {
        val prompt = AIPrompts.generateProductSharingPrompt(
            productName,
            permalink,
            productDescription.orEmpty(),
            languageISOCode
        )
        return fetchJetpackAICompletionsForSite(site, prompt, PRODUCT_SHARING_FEATURE)
    }

    suspend fun generateProductDescription(
        site: SiteModel,
        productName: String,
        features: String,
        languageISOCode: String = "en"
    ): Result<String> {
        val prompt = AIPrompts.generateProductDescriptionPrompt(
            productName,
            features,
            languageISOCode
        )
        return fetchJetpackAICompletionsForSite(site, prompt, PRODUCT_DESCRIPTION_FEATURE)
    }

    suspend fun generateOrderThankYouNote(
        site: SiteModel,
        customerName: String,
        productName: String,
        productDescription: String?,
        languageISOCode: String = "en"
    ): Result<String> {
        val prompt = AIPrompts.generateThankYouNotePrompt(
            customerName = customerName,
            productName = productName,
            productDescription = productDescription.orEmpty(),
            languageISOCode = languageISOCode
        )
        return fetchJetpackAICompletionsForSite(site, prompt, ORDER_DETAIL_THANK_YOU_NOTE)
    }

    @Suppress("LongParameterList")
    suspend fun generateSalesPriceAdvice(
        site: SiteModel,
        currentPrice: BigDecimal,
        currency: String?,
        productName: String,
        productDescription: String?,
        countryCode: String,
        stateCode: String
    ): Result<String> {
        val prompt = AIPrompts.generateSalesPriceAdvicePrompt(
            currentPrice,
            currency,
            productName,
            productDescription.orEmpty(),
            countryCode,
            stateCode
        )
        return fetchJetpackAICompletionsForSite(site, prompt, PRODUCT_SALES_PRICE_ADVICE)
    }

    suspend fun identifyISOLanguageCode(site: SiteModel, text: String, feature: String): Result<String> {
        val prompt = AIPrompts.generateLanguageIdentificationPrompt(text)
        return fetchJetpackAICompletionsForSite(site, prompt, feature)
    }

    private suspend fun fetchJetpackAICompletionsForSite(
        site: SiteModel,
        prompt: String,
        feature: String
    ): Result<String> = withContext(Dispatchers.IO) {
        jetpackAIStore.fetchJetpackAICompletions(site, prompt, feature).run {
            when (this) {
                is JetpackAICompletionsResponse.Success -> {
                    WooLog.d(WooLog.T.AI, "Fetching Jetpack AI completions succeeded")
                    Result.success(completion)
                }
                is JetpackAICompletionsResponse.Error -> {
                    WooLog.w(WooLog.T.AI, "Fetching Jetpack AI completions failed: $message")
                    Result.failure(this.mapToException())
                }
            }
        }
    }
    data class JetpackAICompletionsException(
        val errorMessage: String,
        val errorType: String
    ) : Exception(errorMessage)

    private fun JetpackAICompletionsResponse.Error.mapToException() =
        JetpackAICompletionsException(
            errorMessage = message ?: "Unable to fetch AI completions",
            errorType = type.name
        )
}
