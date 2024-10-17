package com.woocommerce.android.ui.products.ai.preview

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.combine
import com.woocommerce.android.model.Image
import com.woocommerce.android.ui.products.ai.AIProductModel
import com.woocommerce.android.ui.products.ai.AiProductSaveResult
import com.woocommerce.android.ui.products.ai.BuildProductPreviewProperties
import com.woocommerce.android.ui.products.ai.ProductPropertyCard
import com.woocommerce.android.ui.products.ai.SaveAiGeneratedProduct
import com.woocommerce.android.ui.products.ai.components.ImageAction
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AiProductPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val buildProductPreviewProperties: BuildProductPreviewProperties,
    private val generateProductWithAI: GenerateProductWithAI,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val saveAiGeneratedProduct: SaveAiGeneratedProduct,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val DEFAULT_COUNT_OF_VARIANTS = 3
    }

    private val navArgs by savedStateHandle.navArgs<AiProductPreviewFragmentArgs>()

    private val shouldShowFeedbackView = savedStateHandle.getStateFlow(viewModelScope, true)
    private val imageState = savedStateHandle.getStateFlow(viewModelScope, ImageState(navArgs.image))
    private val selectedVariant = savedStateHandle.getStateFlow(viewModelScope, 0)
    private val userEditedFields = savedStateHandle.getStateFlow(viewModelScope, UserEditedFields())

    private val generatedProduct = MutableStateFlow<Result<AIProductModel>?>(
        Result.success(AIProductModel.buildDefault("Name", navArgs.productFeatures))
    )
    private val savingProductState = MutableStateFlow<SavingProductState>(SavingProductState.Idle)

    val state: LiveData<State> = generatedProduct.transformLatest {
        if (it == null) {
            emit(State.Loading)
            return@transformLatest
        }

        when (val product = it.getOrNull()) {
            null -> emit(
                State.Error(
                    onRetryClick = { generateProduct() },
                    onDismissClick = { triggerEvent(MultiLiveEvent.Event.Exit) }
                )
            )

            else -> emitAll(product.prepareState())
        }
    }.asLiveData()

    init {
        generateProduct()
    }

    private fun AIProductModel.prepareState() = flow {
        emitAll(
            combine(
                imageState,
                selectedVariant,
                selectedVariant.map {
                    buildProductPreviewProperties(
                        product = this@prepareState,
                        variant = it,
                    )
                },
                userEditedFields,
                savingProductState,
                shouldShowFeedbackView
            ) { imageState, selectedVariant, propertyGroups, editedFields, savingProductState, shouldShowFeedbackView ->
                State.Success(
                    selectedVariant = selectedVariant,
                    product = this@prepareState,
                    propertyGroups = propertyGroups,
                    imageState = imageState,
                    savingProductState = savingProductState,
                    shouldShowFeedbackView = shouldShowFeedbackView,
                    userEditedName = editedFields.names[selectedVariant],
                    userEditedDescription = editedFields.descriptions[selectedVariant],
                    userEditedShortDescription = editedFields.shortDescriptions[selectedVariant]
                )
            }
        )
    }

    private fun generateProduct() = launch {
        generatedProduct.value = null
        generatedProduct.value = generateProductWithAI(navArgs.productFeatures)
    }

    fun onFeedbackReceived(positive: Boolean) {
        analyticsTracker.track(
            stat = AnalyticsEvent.PRODUCT_AI_FEEDBACK,
            properties = mapOf(
                AnalyticsTracker.KEY_SOURCE to "product_creation",
                AnalyticsTracker.KEY_IS_USEFUL to positive
            )
        )

        shouldShowFeedbackView.value = false
    }

    fun onBackButtonClick() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onImageActionSelected(action: ImageAction) {
        when (action) {
            ImageAction.View -> imageState.value = imageState.value.copy(showImageFullScreen = true)
            ImageAction.Remove -> {
                val previousState = imageState.value
                imageState.value = imageState.value.copy(image = null)
                triggerEvent(
                    MultiLiveEvent.Event.ShowUndoSnackbar(
                        message = resourceProvider.getString(R.string.ai_product_creation_photo_removed),
                        undoAction = { imageState.value = previousState }
                    )
                )
            }

            else -> error("Unsupported action: $action")
        }
    }

    fun onFullScreenImageDismissed() {
        imageState.value = imageState.value.copy(showImageFullScreen = false)
    }

    fun onSelectNextVariant() {
        selectedVariant.update { it + 1 }
    }

    fun onSelectPreviousVariant() {
        selectedVariant.update { it - 1 }
    }

    fun onNameChanged(name: String?) {
        if (name == null) {
            trackUndoEditClick("name")
        }
        val generatedName = generatedProduct.value?.getOrNull()?.names?.get(selectedVariant.value)
        val actualValue = if (name == generatedName) null else name

        userEditedFields.update {
            it.copy(names = it.names.toMutableList().apply { set(selectedVariant.value, actualValue) })
        }
    }

    fun onDescriptionChanged(description: String?) {
        if (description == null) {
            trackUndoEditClick("description")
        }
        val generatedDescription = generatedProduct.value?.getOrNull()?.descriptions?.get(selectedVariant.value)
        val actualValue = if (description == generatedDescription) null else description

        userEditedFields.update {
            it.copy(
                descriptions = it.descriptions.toMutableList().apply {
                    set(selectedVariant.value, actualValue)
                }
            )
        }
    }

    fun onShortDescriptionChanged(shortDescription: String?) {
        if (shortDescription == null) {
            trackUndoEditClick("short_description")
        }
        val generatedShortDescription = generatedProduct.value?.getOrNull()?.shortDescriptions
            ?.get(selectedVariant.value)
        val actualValue = if (shortDescription == generatedShortDescription) null else shortDescription

        userEditedFields.update {
            it.copy(
                shortDescriptions = it.shortDescriptions.toMutableList().apply {
                    set(selectedVariant.value, actualValue)
                }
            )
        }
    }

    fun onGenerateAgainClicked() {
        userEditedFields.value = UserEditedFields()
        selectedVariant.value = 0
        analyticsTracker.track(
            AnalyticsEvent.PRODUCT_CREATION_AI_GENERATE_DETAILS_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_IS_FIRST_ATTEMPT to false,
                AnalyticsTracker.KEY_FEATURE_WORD_COUNT to navArgs.productFeatures.split(" ").size,
            )
        )
        generateProduct()
    }

    fun onSaveProductAsDraft() {
        analyticsTracker.track(AnalyticsEvent.PRODUCT_CREATION_AI_SAVE_AS_DRAFT_BUTTON_TAPPED)
        val product = generatedProduct.value?.getOrNull()?.toProduct(selectedVariant.value) ?: return
        savingProductState.value = SavingProductState.Loading
        viewModelScope.launch {
            val image = imageState.value.image
            val editedFields = userEditedFields.value

            val result = saveAiGeneratedProduct(
                product.copy(
                    name = editedFields.names[selectedVariant.value] ?: product.name,
                    description = editedFields.descriptions[selectedVariant.value] ?: product.description,
                    shortDescription = editedFields.shortDescriptions[selectedVariant.value]
                        ?: product.shortDescription
                ),
                image
            )

            when (result) {
                is AiProductSaveResult.Success -> {
                    savingProductState.value = SavingProductState.Success
                    triggerEvent(NavigateToProductDetailScreen(result.productId))
                    analyticsTracker.track(AnalyticsEvent.PRODUCT_CREATION_AI_SAVE_AS_DRAFT_SUCCESS)
                }

                is AiProductSaveResult.Failure -> {
                    // Keep track of the uploaded image to avoid re-uploading it on retry
                    (result as? AiProductSaveResult.Failure.Generic)?.uploadedImage?.let {
                        imageState.value = imageState.value.copy(image = it)
                    }

                    val messageRes = when (result) {
                        is AiProductSaveResult.Failure.UploadImageFailure ->
                            R.string.ai_product_creation_error_media_upload

                        else -> R.string.error_generic
                    }

                    savingProductState.value = SavingProductState.Error(
                        messageRes = messageRes,
                        onRetryClick = ::onSaveProductAsDraft,
                        onDismissClick = { savingProductState.value = SavingProductState.Idle }
                    )
                    analyticsTracker.track(AnalyticsEvent.PRODUCT_CREATION_AI_SAVE_AS_DRAFT_FAILED)
                }
            }
        }
    }

    private fun trackUndoEditClick(field: String) {
        analyticsTracker.track(
            AnalyticsEvent.PRODUCT_CREATION_AI_UNDO_EDIT_TAPPED,
            mapOf(
                "field" to field
            )
        )
    }

    sealed interface State {
        data object Loading : State
        data class Success(
            val selectedVariant: Int,
            private val product: AIProductModel,
            val propertyGroups: List<List<ProductPropertyCard>>,
            val imageState: ImageState,
            val savingProductState: SavingProductState,
            val shouldShowFeedbackView: Boolean = true,
            private val userEditedName: String? = null,
            private val userEditedDescription: String? = null,
            private val userEditedShortDescription: String? = null
        ) : State {
            val variantsCount = minOf(
                DEFAULT_COUNT_OF_VARIANTS,
                product.names.size,
                product.descriptions.size,
                product.shortDescriptions.size
            )
            val shouldShowVariantSelector = variantsCount > 1

            val name: TextFieldState = TextFieldState(
                value = userEditedName ?: product.names[selectedVariant],
                isValueEditedManually = userEditedName != null
            )
            val description: TextFieldState = TextFieldState(
                value = userEditedDescription ?: product.descriptions[selectedVariant],
                isValueEditedManually = userEditedDescription != null
            )
            val shortDescription: TextFieldState = TextFieldState(
                value = userEditedShortDescription ?: product.shortDescriptions[selectedVariant],
                isValueEditedManually = userEditedShortDescription != null
            )
        }

        data class Error(
            val onRetryClick: () -> Unit,
            val onDismissClick: () -> Unit
        ) : State
    }

    @Parcelize
    data class ImageState(
        val image: Image?,
        val showImageFullScreen: Boolean = false,
    ) : Parcelable

    data class TextFieldState(
        val value: String,
        val isValueEditedManually: Boolean
    )

    @Parcelize
    private data class UserEditedFields(
        val names: List<String?> = List(DEFAULT_COUNT_OF_VARIANTS) { null },
        val descriptions: List<String?> = List(DEFAULT_COUNT_OF_VARIANTS) { null },
        val shortDescriptions: List<String?> = List(DEFAULT_COUNT_OF_VARIANTS) { null }
    ) : Parcelable

    sealed interface SavingProductState : Parcelable {
        @Parcelize
        data object Loading : SavingProductState

        @Parcelize
        data class Error(
            val onRetryClick: () -> Unit,
            val onDismissClick: () -> Unit,
            @StringRes val messageRes: Int
        ) : SavingProductState

        @Parcelize
        data object Success : SavingProductState

        @Parcelize
        data object Idle : SavingProductState
    }

    data class NavigateToProductDetailScreen(val productId: Long) : MultiLiveEvent.Event()
}
