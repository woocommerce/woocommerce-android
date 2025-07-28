package com.woocommerce.android.ui.products.images.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductImageRemoveBackgroundViewModel @Inject constructor(
    savedState: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ScopedViewModel(savedState) {

    private val navArgs: ProductImageRemoveBackgroundFragmentArgs by savedState.navArgs()

    val productImage: Product.Image = navArgs.image

    private val _state: MutableStateFlow<ViewState> =
        MutableStateFlow(ViewState.BackgroundProcessingInProgress(navArgs.image.source.toUri()))
    val state: StateFlow<ViewState> = _state

    init {
        loadImageAndCreateInputImage()
    }

    private fun loadImageAndCreateInputImage() {
        launch {
            val result = createInputImageFromUrl(navArgs.image.source)
            result.fold(
                onSuccess = { removeBackground(it) },
                onFailure = { error ->
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(
                        R.string.remove_background_image_load_error_with_reason,
                        arrayOf(error.message ?: "Unknown error")
                    ))
                }
            )
        }
    }

    private fun removeBackground(image: InputImage) {
        val options = SubjectSegmenterOptions.Builder()
            .build()
        val segmenter = SubjectSegmentation.getClient(options)
        segmenter.process(image).addOnSuccessListener { result ->
            val foregroundBitmap = result.foregroundBitmap
            if (foregroundBitmap != null) {
                _state.value = ViewState.Success(foregroundBitmap)
            } else {
                _state.value = ViewState.Failure
            }
        }
        .addOnFailureListener {
            _state.value = ViewState.Failure
        }
    }

    private suspend fun createInputImageFromUrl(imageUrl: String): Result<InputImage> {
        return try {
            val imageLoader = ImageLoader(context)
            val imageRequest = ImageRequest.Builder(context)
                .data(imageUrl)
                .build()

            val result = imageLoader.execute(imageRequest)
            when (result) {
                is SuccessResult -> {
                    val bitmap = result.drawable.toBitmap()
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    Result.success(inputImage)
                }
                else -> Result.failure(Exception("Image loading failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    object ExitScreen : MultiLiveEvent.Event()
}

sealed class ViewState {
    data class BackgroundProcessingInProgress(val imageUri: Uri) : ViewState()
    data class Success(val bitmap: Bitmap) : ViewState()
    data object Failure : ViewState()
    data object ImageUploadInProgress : ViewState()
}
