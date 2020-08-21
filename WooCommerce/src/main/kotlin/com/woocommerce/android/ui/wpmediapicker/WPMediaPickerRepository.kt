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
import org.wordpress.android.fluxc.utils.MimeType
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

        // according to the docs, use this to return only images (passing "image/*" doesn't work)
        private const val MEDIA_MIME_TYPE = "image"
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
                        loadMore,
                        MimeType.Type.IMAGE
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
    fun getSiteMediaList(): List<Product.Image> {
        val mediaList = mediaStore.getSiteImages(selectedSite.get())
        val imageList = ArrayList<Product.Image>()

        for (media in mediaList) {
            // skip media with empty URLs - these are media that are still being uploaded
            if (media.url.isNullOrEmpty()) {
                WooLog.w(WooLog.T.MEDIA, "Empty media url")
            } else {
                imageList.add(media.toAppModel())
            }
        }

        return imageList
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaListFetched(event: OnMediaListFetched) {
        loadContinuation?.let {
            if (event.isError) {
                it.resume(false)
            } else {
                canLoadMoreMedia = event.canLoadMore
                it.resume(true)
            }
        }
    }
}
