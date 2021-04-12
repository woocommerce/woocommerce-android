package com.woocommerce.android.ui.wpmediapicker

import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.WooLog
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched
import org.wordpress.android.fluxc.utils.MimeType
import javax.inject.Inject

class WPMediaPickerRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val mediaStore: MediaStore
) {
    companion object {
        private const val MEDIA_PAGE_SIZE = WPMediaGalleryView.NUM_COLUMNS * 10
    }

    private val loadContinuation = ContinuationWrapper<Boolean>(WooLog.T.PRODUCTS)

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
    suspend fun fetchSiteMediaList(loadMore: Boolean = false): List<Image> {
        loadContinuation.callAndWaitUntilTimeout {
            val payload = MediaStore.FetchMediaListPayload(
                selectedSite.get(),
                MEDIA_PAGE_SIZE,
                loadMore,
                MimeType.Type.IMAGE
            )
            dispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload))
        }

        return getSiteMediaList()
    }

    /**
     * Returns all media for the current site that are in the database
     */
    fun getSiteMediaList(): List<Image> {
        val mediaList = mediaStore.getSiteImages(selectedSite.get())
        val imageList = ArrayList<Image>()

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
        if (event.isError) {
            loadContinuation.continueWith(false)
        } else if (loadContinuation.isWaiting) {
            canLoadMoreMedia = event.canLoadMore
            loadContinuation.continueWith(true)
        }
    }
}
