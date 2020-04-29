package com.woocommerce.android.ui.wpmediapicker

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.suspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched
import javax.inject.Inject
import kotlin.coroutines.resume

class WPMediaPickerRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val mediaStore: MediaStore
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
        private const val MEDIA_PAGE_SIZE = WPMediaGalleryView.NUM_COLUMNS * 10
    }

    private var loadContinuation: CancellableContinuation<Boolean>? = null

    var canLoadMoreMedia = true
        private set

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    /**
     * Submits a fetch request to get a list of media for the current site
     */
    suspend fun fetchSiteMediaList(loadMore: Boolean = false): List<Product.Image> {
        try {
            loadContinuation?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                loadContinuation = it
                val payload = MediaStore.FetchMediaListPayload(
                        selectedSite.get(),
                        MEDIA_PAGE_SIZE,
                        loadMore
                )
                dispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.e(WooLog.T.PRODUCTS, "CancellationException while fetching site media", e)
        }

        loadContinuation = null
        return getSiteMediaList()
    }

    /**
     * Returns all media for the current site that are in the database
     */
    fun getSiteMediaList(): List<Product.Image> = mediaStore.getSiteImages(selectedSite.get()).map { it.toAppModel() }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaListFetched(event: OnMediaListFetched) {
        if (event.isError) {
            loadContinuation?.resume(false)
        } else {
            canLoadMoreMedia = event.canLoadMore
            loadContinuation?.resume(true)
        }
    }
}
