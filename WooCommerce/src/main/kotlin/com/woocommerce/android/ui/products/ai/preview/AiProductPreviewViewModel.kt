package com.woocommerce.android.ui.products.ai.preview

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Image
import com.woocommerce.android.model.Image.WPMediaLibraryImage
import com.woocommerce.android.ui.products.ai.AIProductModel
import com.woocommerce.android.ui.products.ai.BuildProductPreviewProperties
import com.woocommerce.android.ui.products.ai.ProductPropertyCard
import com.woocommerce.android.ui.products.ai.SaveAiGeneratedProduct
import com.woocommerce.android.ui.products.ai.components.ImageAction
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
    private val uploadImage: UploadImage,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val saveAiGeneratedProduct: SaveAiGeneratedProduct
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val DEFAULT_COUNT_OF_VARIANTS = 3
    }

    private val navArgs by savedStateHandle.navArgs<AiProductPreviewFragmentArgs>()

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
                    onRetryClick = { TODO() },
                    onDismissClick = { triggerEvent(MultiLiveEvent.Event.Exit) }
                )
            )

            else -> {
                emitAll(product.prepareState())
            }
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
                savingProductState
            ) { imageState, selectedVariant, propertyGroups, editedFields, savingProductState ->
                State.Success(
                    selectedVariant = selectedVariant,
                    product = this@prepareState,
                    propertyGroups = propertyGroups,
                    imageState = imageState,
                    savingProductState = savingProductState,
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

    @Suppress("UNUSED_PARAMETER")
    fun onFeedbackReceived(positive: Boolean) {
        TODO()
    }

    fun onBackButtonClick() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onImageActionSelected(action: ImageAction) {
        when (action) {
            ImageAction.View -> imageState.value = imageState.value.copy(showImageFullScreen = true)
            ImageAction.Remove -> imageState.value = imageState.value.copy(image = null)
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
        val generatedName = generatedProduct.value?.getOrNull()?.names?.get(selectedVariant.value)
        val actualValue = if (name == generatedName) null else name

        userEditedFields.update {
            it.copy(names = it.names.toMutableList().apply { set(selectedVariant.value, actualValue) })
        }
    }

    fun onDescriptionChanged(description: String?) {
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

    fun onSaveProductAsDraft() {
        analyticsTrackerWrapper.track(AnalyticsEvent.PRODUCT_CREATION_AI_SAVE_AS_DRAFT_BUTTON_TAPPED)
        addAiGeneratedProduct(publishProduct = false)
    }

    fun onPublishProduct() {
        addAiGeneratedProduct(publishProduct = true)
    }

    private suspend fun uploadSelectedImage() {
        imageState.value.image
            ?.let { uploadImage(it) }
            ?.fold(
                onSuccess = {
                    imageState.value = imageState.value.copy(
                        image = WPMediaLibraryImage(content = it)
                    )
                },
                onFailure = {
                    savingProductState.value = SavingProductState.Error(
                        messageRes = R.string.ai_product_creation_error_media_upload,
                        onRetryClick = ::onSaveProductAsDraft,
                        onDismissClick = { savingProductState.value = SavingProductState.Idle }
                    )
                }
            )
    }

    private fun addAiGeneratedProduct(publishProduct: Boolean) {
        savingProductState.value = SavingProductState.Loading
        val product = generatedProduct.value?.getOrNull()?.toProduct(selectedVariant.value) ?: return
        viewModelScope.launch {
            savingProductState.value = SavingProductState.Loading
            uploadSelectedImage()
            val editedFields = userEditedFields.value
            val (success, productId) = saveAiGeneratedProduct(
                product.copy(
                    name = editedFields.names[selectedVariant.value] ?: product.name,
                    description = editedFields.descriptions[selectedVariant.value] ?: product.description,
                    shortDescription = editedFields.shortDescriptions[selectedVariant.value] ?: product.shortDescription
                ),
                publishProduct,
                getSelectedProductImage()
            )
            if (!success) {
                savingProductState.value = SavingProductState.Error(
                    messageRes = R.string.error_generic,
                    onRetryClick = when {
                        publishProduct -> ::onPublishProduct
                        else -> ::onSaveProductAsDraft
                    },
                    onDismissClick = { savingProductState.value = SavingProductState.Idle }
                )
                analyticsTrackerWrapper.track(AnalyticsEvent.PRODUCT_CREATION_AI_SAVE_AS_DRAFT_FAILED)
            } else {
                analyticsTrackerWrapper.track(AnalyticsEvent.PRODUCT_CREATION_AI_SAVE_AS_DRAFT_SUCCESS)
                triggerEvent(NavigateToProductDetailScreen(productId))
                savingProductState.value = SavingProductState.Success
            }
        }
    }

    private fun getSelectedProductImage() = if (imageState.value.image is WPMediaLibraryImage) {
        (imageState.value.image as WPMediaLibraryImage).content
    } else {
        null
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
