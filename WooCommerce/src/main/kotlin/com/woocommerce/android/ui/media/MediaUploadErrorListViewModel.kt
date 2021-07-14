package com.woocommerce.android.ui.media

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.media.MediaFileUploadHandler.ProductImageUploadUiModel
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploaded
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

@HiltViewModel
class MediaUploadErrorListViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val mediaFileUploadHandler: MediaFileUploadHandler,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: MediaUploadErrorListFragmentArgs by savedState.navArgs()

    private val _mediaUploadErrorList = MutableLiveData<List<ProductImageUploadUiModel>>()
    val mediaUploadErrorList: LiveData<List<ProductImageUploadUiModel>> = _mediaUploadErrorList

    val viewStateData = LiveDataDelegate(
        savedState, ViewState(toolBarTitle = getToolbarTitle())
    )
    private var viewState by viewStateData

    init {
        EventBus.getDefault().register(this)
        start()
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    private fun getToolbarTitle() = resourceProvider.getString(
        R.string.product_images_error_detail_title,
        mediaFileUploadHandler.getMediaUploadErrorCount(navArgs.remoteId)
    )

    private fun start() {
        _mediaUploadErrorList.value = mediaFileUploadHandler.getMediaUploadErrors(navArgs.remoteId) ?: emptyList()
        viewState = viewState.copy(toolBarTitle = getToolbarTitle())
    }

    @Parcelize
    data class ViewState(
        val toolBarTitle: String
    ) : Parcelable

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImageUploaded) {
        if (event.isError) {
            start()
        }
    }
}
