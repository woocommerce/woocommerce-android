package com.woocommerce.android.ui.products.ai.preview

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.media.MediaFilesRepository
import com.woocommerce.android.model.Image
import com.woocommerce.android.ui.products.ai.AIProductModel
import com.woocommerce.android.ui.products.ai.BuildProductPreviewProperties
import com.woocommerce.android.ui.products.ai.ProductPropertyCard
import com.woocommerce.android.ui.products.ai.components.ImageAction
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.MediaModel
import javax.inject.Inject

@HiltViewModel
class AiProductPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val buildProductPreviewProperties: BuildProductPreviewProperties,
    private val generateProductWithAI: GenerateProductWithAI,
    private val mediaFilesRepository: MediaFilesRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs by savedStateHandle.navArgs<AiProductPreviewFragmentArgs>()

    private val imageState = savedStateHandle.getStateFlow(viewModelScope, ImageState(navArgs.image))
    private val selectedVariant = savedStateHandle.getStateFlow(viewModelScope, 0)
    private val generatedProduct = MutableStateFlow<Result<AIProductModel>?>(
        Result.success(AIProductModel.buildDefault("Name", navArgs.productFeatures))
    )
    private val saveProductState = savedStateHandle.getStateFlow<SaveProductDraftState>(
        viewModelScope, SaveProductDraftState.Idle
    )

    val state: LiveData<State> = combine(
        generatedProduct,
        saveProductState,
        imageState,
        selectedVariant
    ) { generatedProduct, saveProductState, imageState, selectedVariant ->
        when {
            generatedProduct == null -> State.Loading
            generatedProduct.getOrNull() == null || generatedProduct.isFailure ->
                State.Error(
                    onRetryClick = { generateProduct() },
                    onDismissClick = { triggerEvent(MultiLiveEvent.Event.Exit) },
                    messageRes = R.string.product_creation_ai_generation_failure_message
                )

            saveProductState is SaveProductDraftState.Error -> State.Error(
                onRetryClick = { generateProduct() },
                onDismissClick = { triggerEvent(MultiLiveEvent.Event.Exit) },
                messageRes = saveProductState.messageRes
            )

            else -> {
                val propertyGroups = buildProductPreviewProperties(
                    product = generatedProduct.getOrThrow(),
                    variant = selectedVariant,
                )

                State.Success(
                    selectedVariant = selectedVariant,
                    product = generatedProduct.getOrThrow(),
                    propertyGroups = propertyGroups.map { group ->
                        group.map { property ->
                            ProductPropertyCard(
                                icon = property.icon,
                                title = property.title,
                                content = property.content
                            )
                        }
                    },
                    imageState = imageState,
                    saveProductState = saveProductState
                )
            }
        }
    }.asLiveData()

    init {
        generateProduct()
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

    fun onSaveProductAsDraft() {
        launch {
            saveProductState.value = SaveProductDraftState.Loading

            val uploadedMediaModel = imageState.value.image
                ?.let { uploadImage(it) }
                ?.getOrElse {
                    val uploadErrorMessageRes = when (it) {
                        is MediaFilesRepository.MediaUploadException ->
                            R.string.ai_product_creation_error_media_upload

                        is OnChangedException ->
                            R.string.ai_product_creation_error_media_fetch

                        else -> R.string.ai_product_creation_error_media_upload
                    }
                    saveProductState.value = SaveProductDraftState.Error(messageRes = uploadErrorMessageRes)
                    return@launch
                }

            // Create product
            createProductDraft(uploadedMediaModel)
            saveProductState.value = SaveProductDraftState.Success
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun createProductDraft(uploadedMediaModel: MediaModel?) {
        //TODO()
    }

    private suspend fun uploadImage(selectedImage: Image): Result<MediaModel> =
        when (selectedImage) {
            is Image.LocalImage -> mediaFilesRepository.uploadFile(selectedImage.uri)
                .transform {
                    when (it) {
                        is MediaFilesRepository.UploadResult.UploadSuccess -> emit(Result.success(it.media))
                        is MediaFilesRepository.UploadResult.UploadFailure -> throw it.error
                        else -> {
                            /* Do nothing */
                        }
                    }
                }
                .retry(1)
                .catch { emit(Result.failure(it)) }
                .first()

            is Image.WPMediaLibraryImage -> mediaFilesRepository.fetchWordPressMedia(selectedImage.content.id)
        }

    sealed interface State {
        data object Loading : State
        data class Success(
            val selectedVariant: Int,
            private val product: AIProductModel,
            val propertyGroups: List<List<ProductPropertyCard>>,
            val imageState: ImageState,
            val shouldShowFeedbackView: Boolean = true,
            val saveProductState: SaveProductDraftState,
        ) : State {
            val variantsCount = minOf(product.names.size, product.descriptions.size, product.shortDescriptions.size)
            val shouldShowVariantSelector = variantsCount > 1

            val title: String
                get() = product.names[selectedVariant]
            val description: String
                get() = product.descriptions[selectedVariant]
            val shortDescription: String
                get() = product.shortDescriptions[selectedVariant]
        }

        data class Error(
            val onRetryClick: () -> Unit,
            val onDismissClick: () -> Unit,
            @StringRes val messageRes: Int
        ) : State
    }

    @Parcelize
    data class ImageState(
        val image: Image?,
        val showImageFullScreen: Boolean = false,
    ) : Parcelable

    sealed interface SaveProductDraftState : Parcelable {
        @Parcelize
        data object Loading : SaveProductDraftState

        @Parcelize
        data class Error(@StringRes val messageRes: Int) : SaveProductDraftState

        @Parcelize
        data object Success : SaveProductDraftState

        @Parcelize
        data object Idle : SaveProductDraftState
    }
}
