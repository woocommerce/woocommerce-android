package com.woocommerce.android.ui.products.ai

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.getStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.parcelize.Parcelize

class AboutProductSubViewModel(
    savedStateHandle: SavedStateHandle,
    override val onDone: (Pair<String, AiTone>) -> Unit,
    private val appsPrefsWrapper: AppPrefsWrapper,
    private val tracker: AnalyticsTrackerWrapper
) : AddProductWithAISubViewModel<Pair<String, AiTone>> {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val productFeatures = savedStateHandle.getStateFlow(
        viewModelScope,
        UiState(
            productName = "",
            productFeatures = "",
            selectedAiTone = appsPrefsWrapper.aiContentGenerationTone
        )
    )

    val state = productFeatures.asLiveData()

    fun onDoneClick() {
        productFeatures.value.let { (_, productFeatures, selectedAiTone) ->
            onDone(Pair(productFeatures, selectedAiTone))

            tracker.track(
                AnalyticsEvent.PRODUCT_CREATION_AI_GENERATE_DETAILS_TAPPED,
                mapOf(
                    AnalyticsTracker.KEY_IS_FIRST_ATTEMPT to appsPrefsWrapper.aiProductCreationIsFirstAttempt
                )
            )
        }
    }

    fun onProductFeaturesUpdated(features: String) {
        productFeatures.value = productFeatures.value.copy(productFeatures = features)
    }

    fun onNewToneSelected(tone: AiTone) {
        tracker.track(
            AnalyticsEvent.PRODUCT_CREATION_AI_TONE_SELECTED,
            mapOf(
                AnalyticsTracker.KEY_TONE to tone.slug
            )
        )
        productFeatures.value = productFeatures.value.copy(selectedAiTone = tone)
        appsPrefsWrapper.aiContentGenerationTone = productFeatures.value.selectedAiTone
    }

    fun updateProductName(name: String) {
        productFeatures.value = productFeatures.value.copy(productName = name)
    }

    override fun close() {
        viewModelScope.cancel()
    }

    @Parcelize
    data class UiState(
        val productName: String,
        val productFeatures: String,
        val selectedAiTone: AiTone
    ) : Parcelable

}
