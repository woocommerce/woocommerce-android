package com.woocommerce.android.ui.products.images.ai

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ProductImageRemoveBackgroundViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val performBackgroundRemoval: PerformImageBackgroundRemoval,
    private val saveProcessedImage: SaveProcessedImageToTheProduct,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
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
                        else -> R.string.remove_background_image_load_error
                    }

                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(errorMessage))
                    triggerEvent(MultiLiveEvent.Event.Exit)
                    trackBackgroundRemovalError(error)
                }
            )
        }
    }

    private fun trackBackgroundRemovalError(error: Throwable) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.PRODUCT_IMAGE_REMOVE_BACKGROUND_ERROR,
            errorContext = this.javaClass.simpleName,
            errorType = error.message,
            errorDescription = error.message
        )
    }

    fun onSaveImageTapped() {
        analyticsTrackerWrapper.track(AnalyticsEvent.PRODUCT_IMAGE_REMOVE_BACKGROUND_SAVE_BUTTON_TAPPED)
        launch {
            val bitmap = when (val currentState = _state.value) {
                is ViewState.Success -> currentState.bitmap
                else -> return@launch
            }

            try {
                saveProcessedImage(bitmap, remoteProductId)
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.save_processed_image_success))
                triggerEvent(MultiLiveEvent.Event.Exit)
            } catch (_: IllegalArgumentException) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.save_processed_image_error))
            } catch (_: IOException) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.save_processed_image_error))
            }
        }
    }

    fun onBackPressed() {
        val currentState = _state.value
        _state.value = when (currentState) {
            is ViewState.BackgroundProcessingInProgress -> currentState.copy(showBackDialog = true)
            is ViewState.Success -> currentState.copy(showBackDialog = true)
        }
    }

    fun onBackDialogDismissed() {
        val currentState = _state.value
        _state.value = when (currentState) {
            is ViewState.BackgroundProcessingInProgress -> currentState.copy(showBackDialog = false)
            is ViewState.Success -> currentState.copy(showBackDialog = false)
        }
    }

    fun onBackDialogConfirmed() {
        analyticsTrackerWrapper.track(AnalyticsEvent.PRODUCT_IMAGE_REMOVE_BACKGROUND_DISCARDED)
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onCancelClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.PRODUCT_IMAGE_REMOVE_BACKGROUND_DISCARDED)
        triggerEvent(MultiLiveEvent.Event.Exit)
    }
}
