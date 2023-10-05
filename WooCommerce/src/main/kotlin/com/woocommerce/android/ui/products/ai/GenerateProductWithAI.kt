package com.woocommerce.android.ui.products.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.ui.products.ProductStatus.DRAFT
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ai.AboutProductSubViewModel.AiTone
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.AI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

class GenerateProductWithAI @Inject constructor(
    private val aiRepository: AIRepository,
    private val categoriesRepository: ProductCategoriesRepository,
    private val tagsRepository: ProductTagsRepository,
    private val parametersRepository: ParameterRepository
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(
        productName: String,
        productKeyWords: String,
        tone: AiTone,
        languageISOCode: String
    ): Result<Product> {
        val existingCategories = categoriesRepository.fetchProductCategories().getOrElse {
            WooLog.e(AI, "Failed to fetch product categories", it)
            return Result.failure(it)
        }
        val existingTags = tagsRepository.fetchProductTags().getOrElse {
            WooLog.e(AI, "Failed to fetch product tags", it)
            return Result.failure(it)
        }
        val siteParameters = getSiteParameters().getOrElse {
            WooLog.e(AI, "Failed to fetch site parameters", it)
            return Result.failure(it)
        }

        return aiRepository.generateProduct(
            productName = productName,
            productKeyWords = productKeyWords,
            tone = tone.slug,
            weightUnit = siteParameters.weightUnit!!,
            dimensionUnit = siteParameters.dimensionUnit!!,
            currency = siteParameters.currencyCode!!,
            existingCategories = existingCategories,
            existingTags = existingTags,
            languageISOCode = languageISOCode
        ).mapCatching { json ->
            val parsedResponse = Gson().fromJson(json, AIProductJsonResponse::class.java)
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
                width = parsedResponse.shipping.width,
                status = DRAFT
            )
        }
    }

    private suspend fun getSiteParameters(): Result<SiteParameters> = withContext(Dispatchers.IO) {
        fun predicate(parameters: SiteParameters): Boolean {
            return parameters.weightUnit.isNotNullOrEmpty() &&
                parameters.dimensionUnit.isNotNullOrEmpty() &&
                parameters.currencyCode.isNotNullOrEmpty()
        }

        return@withContext parametersRepository.getParameters().takeIf(::predicate)?.let { Result.success(it) }
            ?: parametersRepository.fetchParameters().mapCatching {
                require(predicate(it)) { "Site parameters missing information after a successful fetch" }
                it
            }
    }

    private data class AIProductJsonResponse(
        val name: String,
        val description: String,
        @SerializedName("short_description") val shortDescription: String,
        @SerializedName("virtual") val isVirtual: Boolean,
        val price: BigDecimal,
        val shipping: Shipping,
        val categories: List<String>? = null,
        val tags: List<String>? = null
    )

    private data class Shipping(
        val weight: Float,
        val height: Float,
        val length: Float,
        val width: Float
    )
}
