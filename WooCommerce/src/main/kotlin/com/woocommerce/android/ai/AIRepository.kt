package com.woocommerce.android.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsResponse
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val jetpackAIStore: JetpackAIStore
) {
    companion object {
        const val PRODUCT_SHARING_FEATURE = "woo_android_share_product"
        const val PRODUCT_DESCRIPTION_FEATURE = "woo_android_product_description"
        const val PRODUCT_CREATION_FEATURE = "woo_android_product_creation"
    }

    suspend fun generateProductSharingText(
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
        return fetchJetpackAICompletionsForSite(prompt, PRODUCT_SHARING_FEATURE)
    }

    suspend fun generateProductDescription(
        productName: String,
        features: String,
        languageISOCode: String = "en"
    ): Result<String> {
        val prompt = AIPrompts.generateProductDescriptionPrompt(
            productName,
            features,
            languageISOCode
        )
        return fetchJetpackAICompletionsForSite(prompt, PRODUCT_DESCRIPTION_FEATURE)
    }

    @Suppress("LongParameterList")
    suspend fun generateProduct(
        productName: String,
        productKeyWords: String,
        tone: String,
        weightUnit: String,
        dimensionUnit: String,
        currency: String,
        existingCategories: List<ProductCategory>,
        existingTags: List<ProductTag>,
        languageISOCode: String = "en"
    ): Result<Product> {
        data class Shipping(
            val weight: Float,
            val height: Float,
            val length: Float,
            val width: Float
        )

        data class JsonResponse(
            val name: String,
            val description: String,
            @SerializedName("short_description") val shortDescription: String,
            @SerializedName("virtual") val isVirtual: Boolean,
            val price: BigDecimal,
            val shipping: Shipping,
            val categories: List<String>? = null,
            val tags: List<String>? = null
        )

        return fetchJetpackAICompletionsForSite(
            prompt = AIPrompts.generateProductCreationPrompt(
                name = productName,
                keywords = productKeyWords,
                tone = tone,
                weightUnit = weightUnit,
                dimensionUnit = dimensionUnit,
                currency = currency,
                existingCategories = existingCategories.map { it.name },
                existingTags = existingTags.map { it.name },
                languageISOCode = languageISOCode
            ),
            feature = PRODUCT_CREATION_FEATURE
        ).mapCatching { json ->
            val parsedResponse = Gson().fromJson(json, JsonResponse::class.java)
            ProductHelper.getDefaultNewProduct(
                SIMPLE,
                parsedResponse.isVirtual
            ).copy(
                name = parsedResponse.name,
                description = parsedResponse.description,
                shortDescription = parsedResponse.shortDescription,
                regularPrice = parsedResponse.price,
                categories = existingCategories.filter { parsedResponse.categories.orEmpty().contains(it.name) },
                tags = existingTags.filter { parsedResponse.tags.orEmpty().contains(it.name) },
                weight = parsedResponse.shipping.weight,
                height = parsedResponse.shipping.height,
                length = parsedResponse.shipping.length,
                width = parsedResponse.shipping.width
            )
        }
    }

    suspend fun identifyISOLanguageCode(text: String, feature: String): Result<String> {
        val prompt = AIPrompts.generateLanguageIdentificationPrompt(text)
        return fetchJetpackAICompletionsForSite(prompt, feature)
    }

    private suspend fun fetchJetpackAICompletionsForSite(
        prompt: String,
        feature: String
    ): Result<String> = withContext(Dispatchers.IO) {
        jetpackAIStore.fetchJetpackAICompletions(selectedSite.get(), prompt, feature).run {
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
