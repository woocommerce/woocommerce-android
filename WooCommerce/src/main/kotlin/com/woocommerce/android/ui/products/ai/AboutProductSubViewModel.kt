package com.woocommerce.android.ui.products.ai

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.getStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.parcelize.Parcelize

class AboutProductSubViewModel(
    savedStateHandle: SavedStateHandle,
    override val onDone: (String) -> Unit
) : AddProductWithAISubViewModel<String> {
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    override val events: Flow<Event> get() = _events

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val productFeatures = savedStateHandle.getStateFlow(
        viewModelScope,
        UiState(
            productFeatures = "",
            selectedAiTone = AiTone.Casual
        )
    )

    val state = productFeatures.asLiveData()

    fun onDoneClick() {
        onDone(productFeatures.value.productFeatures)
    }

    fun onProductFeaturesUpdated(features: String) {
        productFeatures.value = productFeatures.value.copy(productFeatures = features)
    }

    fun onNewToneSelected(tone: AiTone) {
        productFeatures.value = productFeatures.value.copy(selectedAiTone = tone)
    }

    override fun close() {
        viewModelScope.cancel()
    }

    @Parcelize
    data class UiState(
        val productFeatures: String,
        val selectedAiTone: AiTone
    ) : Parcelable

    enum class AiTone(@StringRes val displayName: Int) {
        Casual(R.string.product_creation_ai_tone_casual),
        Formal(R.string.product_creation_ai_tone_formal),
        Flowery(R.string.product_creation_ai_tone_flowery),
        Convincing(R.string.product_creation_ai_tone_convincing);
    }
}
