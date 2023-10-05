package com.woocommerce.android.ui.products.ai

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.ai.AIRepository.JetpackAICompletionsException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ai.AboutProductSubViewModel.AiTone
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ProductPreviewSubViewModel(
    private val aiRepository: AIRepository,
    private val buildProductPreviewProperties: BuildProductPreviewProperties,
    private val generateProductWithAI: GenerateProductWithAI,
    override val onDone: (Product) -> Unit,
) : AddProductWithAISubViewModel<Product> {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asLiveData()

    private val _events = MutableSharedFlow<MultiLiveEvent.Event>(extraBufferCapacity = 1)
    override val events: Flow<MultiLiveEvent.Event> = _events.asSharedFlow()

    private lateinit var isoLanguageCode: String
    private lateinit var productName: String
    private lateinit var productKeywords: String
    private lateinit var tone: AiTone

    private var generationJob: Job? = null

    override fun onStart() {
        startProductGeneration()
    }

    override fun onStop() {
        generationJob?.cancel()
    }

    fun updateName(name: String) {
        this.productName = name
    }

    fun updateKeywords(keywords: String) {
        this.productKeywords = keywords
    }

    fun updateTone(tone: AiTone) {
        this.tone = tone
    }

    override fun close() {
        viewModelScope.cancel()
    }

    private fun startProductGeneration() {
        fun createErrorState() = State.Error(
            onRetryClick = ::startProductGeneration,
            onDismissClick = { _events.tryEmit(Exit) }
        )

        generationJob = viewModelScope.launch {
            _state.value = State.Loading

            if (!::isoLanguageCode.isInitialized) {
                isoLanguageCode = identifyLanguage() ?: run {
                    WooLog.e(WooLog.T.AI, "Identifying language for the AI prompt failed")
                    _state.value = createErrorState()
                    return@launch
                }
            }

            generateProductWithAI(
                productName = productName,
                productKeyWords = productKeywords,
                tone = tone,
                languageISOCode = isoLanguageCode
            ).fold(
                onSuccess = { product ->
                    AnalyticsTracker.track(AnalyticsEvent.PRODUCT_CREATION_AI_GENERATE_PRODUCT_DETAILS_SUCCESS)
                    _state.value = State.Success(
                        product = product,
                        propertyGroups = buildProductPreviewProperties(product)
                    )
                    onDone(product)
                },
                onFailure = {
                    AnalyticsTracker.track(
                        AnalyticsEvent.PRODUCT_CREATION_AI_GENERATE_PRODUCT_DETAILS_FAILED,
                        mapOf(
                            AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                            AnalyticsTracker.KEY_ERROR_TYPE to (it as? JetpackAICompletionsException)?.errorType,
                            AnalyticsTracker.KEY_ERROR_DESC to (it as? JetpackAICompletionsException)?.errorMessage
                        )
                    )
                    WooLog.e(WooLog.T.AI, "Failed to generate product with AI", it)
                    _state.value = createErrorState()
                }
            )
        }
    }

    private suspend fun identifyLanguage(): String? {
        return aiRepository.identifyISOLanguageCode(
            "$productName\n$productKeywords",
            AIRepository.PRODUCT_CREATION_FEATURE
        )
            .fold(
                onSuccess = {
                    AnalyticsTracker.track(
                        AnalyticsEvent.AI_IDENTIFY_LANGUAGE_SUCCESS,
                        mapOf(
                            AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_CREATION
                        )
                    )
                    it
                },
                onFailure = { error ->
                    AnalyticsTracker.track(
                        AnalyticsEvent.AI_IDENTIFY_LANGUAGE_FAILED,
                        mapOf(
                            AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                            AnalyticsTracker.KEY_ERROR_TYPE to (error as? JetpackAICompletionsException)?.errorType,
                            AnalyticsTracker.KEY_ERROR_DESC to (error as? JetpackAICompletionsException)?.errorMessage,
                            AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_CREATION
                        )
                    )
                    null
                }
            )
    }

    sealed interface State {
        object Loading : State
        data class Success(
            private val product: Product,
            val propertyGroups: List<List<ProductPropertyCard>>
        ) : State {
            val title: String
                get() = product.name
            val description: String
                get() = product.description
            val shortDescription: String
                get() = product.shortDescription
        }

        data class Error(
            val onRetryClick: () -> Unit,
            val onDismissClick: () -> Unit
        ) : State
    }

    data class ProductPropertyCard(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        val content: String
    )
}
