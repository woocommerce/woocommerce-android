package com.woocommerce.android.ui.products.ai.preview

import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.ai.AIRepository.JetpackAICompletionsException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.ai.AIProductModel
import kotlinx.coroutines.delay
import javax.inject.Inject

class GenerateProductWithAI @Inject constructor(
    private val aiRepository: AIRepository,
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    private lateinit var languageISOCode: String
    // private var isProductDependenciesFetched = false

    suspend operator fun invoke(productFeatures: String): Result<AIProductModel> {
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
