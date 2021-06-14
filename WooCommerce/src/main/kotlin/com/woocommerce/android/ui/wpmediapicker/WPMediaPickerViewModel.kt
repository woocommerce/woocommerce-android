package com.woocommerce.android.ui.wpmediapicker

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class WPMediaPickerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val mediaPickerRepository: WPMediaPickerRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState) {
    private val navArgs: WPMediaPickerFragmentArgs by savedState.navArgs()

    private val _mediaList = MutableLiveData<List<Product.Image>>()
    val mediaList: LiveData<List<Product.Image>> = _mediaList

    val viewStateLiveData = LiveDataDelegate(
        savedState,
        ViewState(isMultiSelectionAllowed = navArgs.allowMultiple)
    )
    private var viewState by viewStateLiveData

    fun start() {
        loadMedia()
    }

    fun onLoadMoreRequested() {
        loadMedia(loadMore = true)
    }

    override fun onCleared() {
        super.onCleared()
        mediaPickerRepository.onCleanup()
    }

    private fun isLoading() = viewState.isLoading == true

    private fun isLoadingMore() = viewState.isLoadingMore == true

    private fun loadMedia(loadMore: Boolean = false) {
        if (isLoading()) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading media")
            return
        }

        if (loadMore && !mediaPickerRepository.canLoadMoreMedia) {
            WooLog.d(WooLog.T.PRODUCTS, "can't load more media")
            return
        }

        if (loadMore && isLoadingMore()) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading more media")
            return
        }

        launch {
            viewState = viewState.copy(isLoadingMore = loadMore)
            if (!loadMore) {
                val mediaInDb = mediaPickerRepository.getSiteMediaList()
                if (mediaInDb.isNullOrEmpty()) {
                    viewState = viewState.copy(isLoading = true)
                } else {
                    _mediaList.value = mediaInDb
                }
            }

            fetchMedia(loadMore = loadMore)
        }
    }

    private suspend fun fetchMedia(loadMore: Boolean = false) {
        if (networkStatus.isConnected()) {
            val fetchedMedia = mediaPickerRepository.fetchSiteMediaList(loadMore)
            if (fetchedMedia.isNullOrEmpty()) {
                viewState = viewState.copy(isEmptyViewVisible = true)
            } else {
                _mediaList.value = fetchedMedia
                viewState = viewState.copy(isEmptyViewVisible = false)
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
        viewState = viewState.copy(isLoading = false, isLoadingMore = false)
    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null,
        val isMultiSelectionAllowed: Boolean? = null
    ) : Parcelable
}
