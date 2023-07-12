package com.woocommerce.android.ai

import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsResponse
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepository @Inject constructor(
    private val jetpackAIStore: JetpackAIStore
) {
    companion object {
        const val PRODUCT_SHARING_FEATURE = "woo_android_share_product"
        const val PRODUCT_DESCRIPTION_FEATURE = "woo_android_product_description"
    }

    suspend fun generateProductSharingText(
        site: SiteModel,
        productName: String,
        permalink: String,
        productDescription: String?
    ): Result<String> {
        val prompt = AIPrompts.generateProductSharingPrompt(
            productName,
            permalink,
            productDescription.orEmpty()
        )
        return fetchJetpackAICompletionsForSite(site, prompt, PRODUCT_SHARING_FEATURE)
    }

    suspend fun generateProductDescription(site: SiteModel, productName: String, features: String): Result<String> {
        val prompt = AIPrompts.generateProductDescriptionPrompt(
            productName,
            features
        )
        return fetchJetpackAICompletionsForSite(site, prompt, PRODUCT_DESCRIPTION_FEATURE, skipCache = true)
    }

    suspend fun identifyISOLanguageCode(site: SiteModel, text: String): Result<String> {
        val prompt = AIPrompts.generateLanguageIdentificationPrompt(text)
        return fetchJetpackAICompletionsForSite(site, prompt, PRODUCT_DESCRIPTION_FEATURE, skipCache = true)
    }

    private suspend fun fetchJetpackAICompletionsForSite(
        site: SiteModel,
        prompt: String,
        feature: String,
        skipCache: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        jetpackAIStore.fetchJetpackAICompletionsForSite(site, prompt, feature, skipCache).run {
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
