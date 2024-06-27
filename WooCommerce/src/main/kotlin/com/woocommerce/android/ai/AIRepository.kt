package com.woocommerce.android.ai

import com.google.gson.Gson
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIQueryResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.ResponseFormat
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
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
        const val PRODUCT_NAME_FEATURE = "woo_android_product_name"
        const val PRODUCT_DETAILS_FROM_SCANNED_TEXT_FEATURE = "woo_android_product_details_from_scanned_texts"
        const val ORDER_DETAIL_THANK_YOU_NOTE = "woo_android_order_detail_thank_you_note"
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
        return fetchJetpackAIQuery(prompt, PRODUCT_SHARING_FEATURE)
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
        return fetchJetpackAIQuery(prompt, PRODUCT_DESCRIPTION_FEATURE)
    }

    suspend fun generateProductName(
        keywords: String,
        languageISOCode: String = "en"
    ): Result<String> {
        val prompt = AIPrompts.generateProductNamePrompt(
            keywords,
            languageISOCode
        )

        return fetchJetpackAIQuery(prompt, PRODUCT_NAME_FEATURE)
    }

    suspend fun generateProductNameAndDescription(
        keywords: String,
        languageISOCode: String = "en"
    ): Result<AIProductDetailsResult> {
        val prompt = AIPrompts.generateProductNameAndDescriptionPrompt(
            keywords,
            languageISOCode
        )

        return fetchJetpackAIQuery(prompt, PRODUCT_DETAILS_FROM_SCANNED_TEXT_FEATURE, ResponseFormat.JSON)
            .mapCatching { json ->
                Gson().fromJson(json, AIProductDetailsResult::class.java)
            }
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
        languageISOCode: String
    ): Result<String> {
        return fetchJetpackAIQuery(
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
            feature = PRODUCT_CREATION_FEATURE,
            format = ResponseFormat.JSON
        )
    }

    suspend fun generateOrderThankYouNote(
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
        return fetchJetpackAIQuery(prompt, ORDER_DETAIL_THANK_YOU_NOTE)
    }

    suspend fun identifyISOLanguageCode(text: String, feature: String): Result<String> {
        val prompt = AIPrompts.generateLanguageIdentificationPrompt(text)
        return fetchJetpackAIQuery(prompt, feature)
    }

    private suspend fun fetchJetpackAIQuery(
        prompt: String,
        feature: String,
        format: ResponseFormat = ResponseFormat.TEXT,
    ): Result<String> = withContext(Dispatchers.IO) {
        jetpackAIStore.fetchJetpackAIQuery(
            site = selectedSite.get(),
            question = prompt,
            feature = feature,
            format = format,
            stream = false
        ).run {
            when (this) {
                is JetpackAIQueryResponse.Success -> {
                    WooLog.d(WooLog.T.AI, "Fetching Jetpack AI query succeeded")
                    Result.success(choices[0].message?.content ?: "")
                }

                is JetpackAIQueryResponse.Error -> {
                    WooLog.w(WooLog.T.AI, "Fetching Jetpack AI query failed: $message")
                    Result.failure(mapToException())
                }
            }
        }
    }

    data class JetpackAICompletionsException(
        val errorMessage: String,
        val errorType: String
    ) : Exception(errorMessage)

    data class AIProductDetailsResult(
        val name: String,
        val description: String
    )

    private fun JetpackAIQueryResponse.Error.mapToException() =
        JetpackAICompletionsException(
            errorMessage = message ?: "Unable to fetch AI completions",
            errorType = type.name
        )
}
