package com.woocommerce.android.ui.products.ai

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ai.AboutProductSubViewModel.AiTone
import com.woocommerce.android.viewmodel.getStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.parcelize.Parcelize

class AboutProductSubViewModel(
    savedStateHandle: SavedStateHandle,
    override val onDone: (Pair<String, AiTone>) -> Unit
) : AddProductWithAISubViewModel<Pair<String, AiTone>> {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val productFeatures = savedStateHandle.getStateFlow(
        viewModelScope,
        UiState(
            productName = "",
            productFeatures = "",
            selectedAiTone = AiTone.Casual
        )
    )

    val state = productFeatures.asLiveData()

    fun onDoneClick() {
        productFeatures.value.let { (_, productFeatures, selectedAiTone) ->
            onDone(Pair(productFeatures, selectedAiTone))
        }
    }

    fun onProductFeaturesUpdated(features: String) {
        productFeatures.value = productFeatures.value.copy(productFeatures = features)
    }

    fun onNewToneSelected(tone: AiTone) {
        productFeatures.value = productFeatures.value.copy(selectedAiTone = tone)
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

    enum class AiTone(@StringRes val displayName: Int, val slug: String) {
        Casual(R.string.product_creation_ai_tone_casual, "Casual"),
        Formal(R.string.product_creation_ai_tone_formal, "Formal"),
        Flowery(R.string.product_creation_ai_tone_flowery, "Flowery"),
        Convincing(R.string.product_creation_ai_tone_convincing, "Convincing");
    }
}
