package com.woocommerce.android.ui.media

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.media.MediaFileUploadHandler.ProductImageUploadData
import com.woocommerce.android.ui.media.MediaFileUploadHandler.UploadStatus
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class MediaUploadErrorListViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    mediaFileUploadHandler: MediaFileUploadHandler,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: MediaUploadErrorListFragmentArgs by savedState.navArgs()

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        val errorList = navArgs.errorList
        if (errorList != null) {
            val currentErrors = errorList.map<ProductImageUploadData, ErrorUiModel> {
                ErrorUiModel(it.uploadStatus as UploadStatus.Failed)
            }
            viewState = viewState.copy(
                uploadErrorList = currentErrors,
                toolBarTitle = resourceProvider.getString(
                    R.string.product_images_error_detail_title,
                    currentErrors.size
                )
            )
            mediaFileUploadHandler.clearImageErrors(navArgs.remoteId)
        } else {
            mediaFileUploadHandler.observeCurrentUploadErrors(navArgs.remoteId)
                .filter { it.isNotEmpty() }
                .onEach { errors ->
                    val currentErrors =
                        viewState.uploadErrorList + errors.map { ErrorUiModel(it.uploadStatus as UploadStatus.Failed) }
                    viewState = viewState.copy(
                        uploadErrorList = currentErrors,
                        toolBarTitle = resourceProvider.getString(
                            R.string.product_images_error_detail_title,
                            currentErrors.size
                        )
                    )
                    // Remove errors from mediaFileUploadHandler to avoid duplicate notifications
                    mediaFileUploadHandler.clearImageErrors(navArgs.remoteId)
                }
                .launchIn(this)
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
        val filePath: String
    ) : Parcelable {
        constructor(state: UploadStatus.Failed) : this(
            fileName = state.media?.fileName.orEmpty(),
            errorMessage = state.mediaErrorMessage,
            filePath = state.media?.filePath.orEmpty()
        )
    }
}
