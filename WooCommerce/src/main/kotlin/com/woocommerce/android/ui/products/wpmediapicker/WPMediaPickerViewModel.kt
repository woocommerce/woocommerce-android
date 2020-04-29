package com.woocommerce.android.ui.products.wpmediapicker

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch

class WPMediaPickerViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val mediaPickerRepository: WPMediaPickerRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState, dispatchers) {
    private val _mediaList = MutableLiveData<List<Product.Image>>()
    val mediaList: LiveData<List<Product.Image>> = _mediaList

    val viewStateLiveData = LiveDataDelegate(savedState,
            ViewState()
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

    private fun isLoadingMore() = viewState.isLoadingMore == true

    private fun isRefreshing() = viewState.isRefreshing == true

    private fun loadMedia(loadMore: Boolean = false) {
        if (loadMore && !mediaPickerRepository.canLoadMoreMedia) {
            WooLog.d(WooLog.T.PRODUCTS, "can't load more media")
            return
        }

        if (loadMore && isLoadingMore()) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading more media")
            return
        }

        if (loadMore && isRefreshing()) {
            WooLog.d(WooLog.T.PRODUCTS, "already refreshing media")
            return
        }

        launch {
            viewState = viewState.copy(isLoadingMore = loadMore)
            if (!loadMore) {
                val mediaInDb = mediaPickerRepository.getSiteMediaList()
                if (mediaInDb.isNullOrEmpty()) {
                    viewState = viewState.copy(isSkeletonShown = true)
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
                if (!loadMore) {
                    viewState = viewState.copy(isEmptyViewVisible = true)
                }
            } else {
                _mediaList.value = fetchedMedia
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
        viewState = viewState.copy(
                isSkeletonShown = false,
                isRefreshing = false,
                isLoadingMore = false
        )
    }

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val canLoadMore: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<WPMediaPickerViewModel>
}
