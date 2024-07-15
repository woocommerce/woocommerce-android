package com.woocommerce.android.ui.products.ai.preview

import android.os.Parcelable
import androidx.annotation.DrawableRes
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
import com.woocommerce.android.ui.products.ai.components.ImageAction
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.MediaModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AiProductPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val buildProductPreviewProperties: BuildProductPreviewProperties,
    private val mediaFilesRepository: MediaFilesRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs by savedStateHandle.navArgs<AiProductPreviewFragmentArgs>()

    private val imageState = savedStateHandle.getStateFlow(
        viewModelScope, ImageState(
            navArgs.image,
            uploadError = null
        )
    )
    private val selectedVariant = savedStateHandle.getStateFlow(viewModelScope, 0)
    private val generatedProduct = MutableStateFlow<Result<AIProductModel>?>(
        Result.success(AIProductModel.buildDefault("Name", navArgs.productFeatures))
    )

    val state: LiveData<State> = generatedProduct.transformLatest {
        if (it == null) {
            emit(State.Loading)
            return@transformLatest
        }

        when (val product = it.getOrNull()) {
            null -> emit(
                State.Error(
                    onRetryClick = { TODO() },
                    onDismissClick = { triggerEvent(MultiLiveEvent.Event.Exit) },
                )
            )

            else -> emitAll(product.prepareState())
        }
    }.asLiveData()

    private fun AIProductModel.prepareState() = flow {
        val propertyGroups = buildProductPreviewProperties(
            toProduct(
                variant = 0,
                existingCategories = emptyList(),
                existingTags = emptyList()
            )
        )

        emitAll(
            combine(
                imageState,
                selectedVariant
            ) { imageState, selectedVariant ->
                State.Success(
                    selectedVariant = selectedVariant,
                    product = this@prepareState,
                    propertyGroups = propertyGroups.map { group ->
                        group.map { property ->
                            ProductPropertyCard(
                                icon = property.icon,
                                title = property.title,
                                content = property.content
                            )
                        }
                    },
                    imageState = imageState
                )
            }
        )
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

    fun onSaveProductAsDraft() {
        // Upload image if any selected image
        launch {
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
                    imageState.value = imageState.value.copy(uploadError = uploadErrorMessageRes)
                    return@launch
                }
            // Save product as draft
            saveProductAsDraft(uploadedMediaModel)
        }
    }

    private fun saveProductAsDraft(uploadedMediaModel: MediaModel?) {
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
            private val selectedVariant: Int,
            private val product: AIProductModel,
            val propertyGroups: List<List<ProductPropertyCard>>,
            val imageState: ImageState,
            val shouldShowFeedbackView: Boolean = true
        ) : State {
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
        ) : State
    }

    @Parcelize
    data class ImageState(
        val image: Image?,
        val showImageFullScreen: Boolean = false,
        @StringRes val uploadError: Int?
    ) : Parcelable

    data class ProductPropertyCard(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        val content: String
    )
}
