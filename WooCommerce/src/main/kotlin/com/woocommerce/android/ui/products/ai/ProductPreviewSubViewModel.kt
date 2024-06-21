package com.woocommerce.android.ui.products.ai

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.asLiveData
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.WooException
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.ai.AIRepository.JetpackAICompletionsException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ai.AboutProductSubViewModel.AiTone
import com.woocommerce.android.ui.products.ai.AddProductWithAIViewModel.EditPrice
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import java.math.BigDecimal

@Suppress("LongParameterList")
class ProductPreviewSubViewModel(
    private val aiRepository: AIRepository,
    private val buildProductPreviewProperties: BuildProductPreviewProperties,
    private val generateProductWithAI: GenerateProductWithAI,
    private val tracker: AnalyticsTrackerWrapper,
    override val onDone: (Product) -> Unit,
) : AddProductWithAISubViewModel<Product> {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asLiveData()

    private val _events = MutableSharedFlow<MultiLiveEvent.Event>(extraBufferCapacity = 1)
    override val events: Flow<MultiLiveEvent.Event> = _events.asSharedFlow()

    private lateinit var isoLanguageCode: String
    private lateinit var productName: String
    private var productDescription: String? = null
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

    fun updateProductDescription(description: String) {
        this.productDescription = description
    }

    fun updateTone(tone: AiTone) {
        this.tone = tone
    }

    fun onFeedbackReceived(positive: Boolean) {
        tracker.track(
            stat = AnalyticsEvent.PRODUCT_AI_FEEDBACK,
            properties = mapOf(
                AnalyticsTracker.KEY_SOURCE to "product_creation",
                AnalyticsTracker.KEY_IS_USEFUL to positive
            )
        )

        _state.update { (it as State.Success).copy(shouldShowFeedbackView = false) }
    }

    private fun onEditPrice(suggestedPrice: BigDecimal) {
        _events.tryEmit(EditPrice(suggestedPrice))
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

            val generatedProduct = if (productDescription == null) {
                generateProductWithAI(
                    productName = productName,
                    productKeyWords = productKeywords,
                    tone = tone,
                    languageISOCode = isoLanguageCode
                )
            } else {
                generateProductWithAI(
                    productName = productName,
                    productDescription = productDescription!!,
                    productKeyWords = productKeywords,
                    languageISOCode = isoLanguageCode
                )
            }

            generatedProduct.fold(
                onSuccess = { product ->
                    AnalyticsTracker.track(AnalyticsEvent.PRODUCT_CREATION_AI_GENERATE_PRODUCT_DETAILS_SUCCESS)
                    _state.value = State.Success(
                        product = product,
                        propertyGroups = buildProductPreviewProperties(product, onEditPrice = ::onEditPrice)
                    )
                    onDone(product)
                },
                onFailure = {
                    val errorType = when (it) {
                        is JetpackAICompletionsException -> it.errorType
                        is OnChangedException -> (it.error as? ProductError)?.type?.name
                        is WooException -> it.error.type.name
                        else -> null
                    }
                    tracker.track(
                        AnalyticsEvent.PRODUCT_CREATION_AI_GENERATE_PRODUCT_DETAILS_FAILED,
                        mapOf(
                            AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                            AnalyticsTracker.KEY_ERROR_TYPE to errorType,
                            AnalyticsTracker.KEY_ERROR_DESC to it.message
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
                    tracker.track(
                        AnalyticsEvent.AI_IDENTIFY_LANGUAGE_SUCCESS,
                        mapOf(
                            AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_CREATION
                        )
                    )
                    it
                },
                onFailure = { error ->
                    tracker.track(
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

    fun updatePrice(regularPrice: BigDecimal) {
        val currentProduct = (state.value as? State.Success)?.product ?: return
        val updatedProduct = currentProduct.copy(regularPrice = regularPrice)
        val updatedPropertyGroups = buildProductPreviewProperties(updatedProduct, onEditPrice = ::onEditPrice)
        _state.update {
            (it as State.Success).copy(
                product = updatedProduct,
                propertyGroups = updatedPropertyGroups
            )
        }
    }

    sealed interface State {
        object Loading : State
        data class Success(
            val product: Product,
            val propertyGroups: List<List<ProductPropertyCard>>,
            val shouldShowFeedbackView: Boolean = true
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
        val content: String,
        val onClick: () -> Unit = {}
    )
}
