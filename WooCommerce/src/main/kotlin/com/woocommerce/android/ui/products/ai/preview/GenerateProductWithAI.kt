package com.woocommerce.android.ui.products.ai.preview

import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.ai.AIRepository.JetpackAICompletionsException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ai.AIProductModel
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.AI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GenerateProductWithAI @Inject constructor(
    private val aiRepository: AIRepository,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val categoriesRepository: ProductCategoriesRepository,
    private val tagsRepository: ProductTagsRepository,
    private val parametersRepository: ParameterRepository
) {
    private lateinit var languageISOCode: String
    private var isProductCategoriesFetched = false
    private var isProductTagsFetched = false

    suspend operator fun invoke(productFeatures: String): Result<AIProductModel> {
        val existingCategories = getCategories().getOrElse {
            WooLog.e(AI, "Failed to fetch product categories", it)
            return Result.failure(it)
        }
        val existingTags = getTags().getOrElse {
            WooLog.e(AI, "Failed to fetch product tags", it)
            return Result.failure(it)
        }
        val siteParameters = getSiteParameters().getOrElse {
            WooLog.e(AI, "Failed to fetch site parameters", it)
            return Result.failure(it)
        }

        if (!::languageISOCode.isInitialized) {
            languageISOCode = identifyLanguage(productFeatures).getOrElse {
                return Result.failure(it)
            }
        }

        delay(1000)

        // TODO
        return Result.success(
            AIProductModel.buildDefault(
                name = "Name",
                description = productFeatures
            )
        )
    }

    private suspend fun getCategories(): Result<List<ProductCategory>> {
        return if (isProductCategoriesFetched) {
            withContext(Dispatchers.IO) {
                Result.success(categoriesRepository.getProductCategoriesList())
            }
        } else {
            categoriesRepository.fetchProductCategories().onSuccess {
                isProductCategoriesFetched = true
            }
        }
    }

    private suspend fun getTags(): Result<List<ProductTag>> {
        return if (isProductCategoriesFetched) {
            withContext(Dispatchers.IO) {
                Result.success(tagsRepository.getProductTags())
            }
        } else {
            tagsRepository.fetchProductTags().onSuccess {
                isProductTagsFetched = true
            }
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

    private suspend fun identifyLanguage(productFeatures: String): Result<String> {
        return aiRepository.identifyISOLanguageCode(
            productFeatures,
            AIRepository.PRODUCT_CREATION_FEATURE
        )
            .onSuccess {
                analyticsTracker.track(
                    AnalyticsEvent.AI_IDENTIFY_LANGUAGE_SUCCESS,
                    mapOf(
                        AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_CREATION
                    )
                )
            }
            .onFailure { error ->
                analyticsTracker.track(
                    AnalyticsEvent.AI_IDENTIFY_LANGUAGE_FAILED,
                    mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to (error as? JetpackAICompletionsException)?.errorType,
                        AnalyticsTracker.KEY_ERROR_DESC to (error as? JetpackAICompletionsException)?.errorMessage,
                        AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_CREATION
                    )
                )
            }
    }
}
