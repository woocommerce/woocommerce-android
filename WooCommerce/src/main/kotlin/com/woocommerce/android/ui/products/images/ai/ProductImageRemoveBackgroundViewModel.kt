package com.woocommerce.android.ui.products.images.ai

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductImageRemoveBackgroundViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val performBackgroundRemoval: PerformImageBackgroundRemoval,
    private val saveProcessedImage: SaveProcessedImageToTheProduct
) : ScopedViewModel(savedState) {
    private val navArgs: ProductImageRemoveBackgroundFragmentArgs by savedState.navArgs()
    private val remoteProductId: Long = navArgs.remoteProductId

    private val _state: MutableStateFlow<ViewState> =
        MutableStateFlow(ViewState.BackgroundProcessingInProgress(navArgs.image.source.toUri()))
    val state: StateFlow<ViewState> = _state

    init {
        processBackgroundRemoval()
    }

    private fun processBackgroundRemoval() {
        launch {
            val result = performBackgroundRemoval(navArgs.image.source)
            result.fold(
                onSuccess = { bitmap ->
                    _state.value = ViewState.Success(bitmap)
                },
                onFailure = { error ->
                    val errorMessage = when {
                        error.message?.contains("No subject detected") == true -> {
                            R.string.remove_background_no_subject_detected
                        }
                        else -> R.string.remove_background_image_load_error_with_reason
                    }

                    triggerEvent(
                        MultiLiveEvent.Event.ShowSnackbar(
                            errorMessage,
                            if (errorMessage == R.string.remove_background_image_load_error_with_reason) {
                                arrayOf(error.message ?: "Unknown error")
                            } else {
                                emptyArray()
                            }
                        )
                    )
                }
            )
        }
    }

    fun onSaveImageTapped() {
        launch {
            val bitmap = when (val currentState = _state.value) {
                is ViewState.Success -> currentState.bitmap
                else -> return@launch
            }

            try {
                saveProcessedImage(bitmap, remoteProductId)
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.save_processed_image_success))
                triggerEvent(MultiLiveEvent.Event.Exit)
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") error: Throwable) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.save_processed_image_error))
                _state.value = ViewState.Success(bitmap)
            }
        }
    }
}
