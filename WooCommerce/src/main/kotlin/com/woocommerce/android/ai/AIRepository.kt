package com.woocommerce.android.ai

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsResponse
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class AIRepository @Inject constructor(
    private val selectedSite: SelectedSite,
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

    @Suppress("UNUSED_PARAMETER")
    suspend fun generateProduct(
        productName: String,
        productKeyWords: String,
        languageISOCode: String = "en"
    ): Result<Product> {
        // TODO implement the AI prompt and parsing
        delay(5.seconds)
        return Result.success(
            ProductHelper.getDefaultNewProduct(
                SIMPLE,
                false
            ).copy(
                name = "Soft Black Tee: Elevate Your Everyday Style",
                description = "Introducing our USA-Made Classic Organic Cotton Tee—a staple piece designed for " +
                    "everyday comfort and sustainability. Crafted with care from organic cotton, this tee is not" +
                    " just soft on your skin but gentle on the environment.",
                regularPrice = BigDecimal.TEN,
                categories = listOf(ProductCategory(name = "Category 1"), ProductCategory(name = "Category 2")),
                tags = listOf(ProductTag(name = "tag 1")),
                weight = 10f,
                height = 10f,
                length = 10f,
                width = 10f
            )
        )
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
