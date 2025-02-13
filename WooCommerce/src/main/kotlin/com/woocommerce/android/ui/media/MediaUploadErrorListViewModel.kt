package com.woocommerce.android.ui.media

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.media.MediaFileUploadHandler.ProductImageUploadData
import com.woocommerce.android.ui.media.MediaFileUploadHandler.UploadStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class MediaUploadErrorListViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val mediaFileUploadHandler: MediaFileUploadHandler,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: MediaUploadErrorListFragmentArgs by savedStateHandle.navArgs()

    private var _viewState = savedStateHandle.getStateFlow(
        scope = this,
        initialValue = ViewState(
            uploadErrorList = navArgs.errorList
                ?.map<ProductImageUploadData, ErrorUiModel> {
                    ErrorUiModel(it.uploadStatus as UploadStatus.Failed, it.localUri)
                } ?: emptyList(),
        ),
        key = "uploadErrorsListState"
    )
    val viewState = _viewState.asLiveData()

    init {
        if (navArgs.errorList?.isNotEmpty() == true) {
            // Clear errors to avoid duplicated error items and notifications
            mediaFileUploadHandler.clearImageErrors(navArgs.remoteProductId)
        }
        mediaFileUploadHandler.observeCurrentUploadErrors(navArgs.remoteProductId)
            .filter { it.isNotEmpty() }
            .onEach { newErrors ->
                val currentErrors = _viewState.value.uploadErrorList +
                    newErrors
                        .map { ErrorUiModel(it.uploadStatus as UploadStatus.Failed, it.localUri) }
                        .filter { _viewState.value.uploadErrorList.contains(it).not() } // Filter duplicates
                _viewState.update {
                    _viewState.value.copy(
                        uploadErrorList = currentErrors,
                        toolBarTitle = resourceProvider.getString(
                            R.string.product_images_error_detail_title,
                            currentErrors.size
                        )
                    )
                }
            }
            .launchIn(this)
    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    fun onRetryUploadClicked(error: ErrorUiModel) {
        mediaFileUploadHandler.enqueueUpload(navArgs.remoteProductId, listOf(error.localUri))
        mediaFileUploadHandler.clearImageErrors(navArgs.remoteProductId, listOf(error.localUri))
        _viewState.update {
            _viewState.value.copy(
                uploadErrorList = _viewState.value.uploadErrorList - error
            )
        }
        if (viewState.value?.uploadErrorList?.isEmpty() == true) {
            mediaFileUploadHandler.clearImageErrors(navArgs.remoteProductId)
            triggerEvent(Exit)
        }
    }

    @Parcelize
    data class ViewState(
        val uploadErrorList: List<ErrorUiModel> = emptyList(),
        val toolBarTitle: String = ""
    ) : Parcelable

    @Parcelize
    data class ErrorUiModel(
        val fileName: String,
        val errorMessage: String,
        val localUri: String
    ) : Parcelable {
        constructor(state: UploadStatus.Failed, localUri: String) : this(
            fileName = state.media?.fileName.orEmpty(),
            errorMessage = state.mediaErrorMessage,
            localUri = localUri
        )
    }
}
