package com.woocommerce.android.mediapicker.medialibrary

import com.woocommerce.android.R
import com.woocommerce.android.tools.NetworkStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched
import org.wordpress.android.fluxc.utils.MimeType
import org.wordpress.android.mediapicker.api.MediaSource
import org.wordpress.android.mediapicker.api.MediaSource.MediaLoadingResult
import org.wordpress.android.mediapicker.api.MediaSource.MediaLoadingResult.Empty
import org.wordpress.android.mediapicker.api.MediaSource.MediaLoadingResult.Failure
import org.wordpress.android.mediapicker.model.MediaItem
import org.wordpress.android.mediapicker.model.MediaItem.Identifier.RemoteMedia
import org.wordpress.android.mediapicker.model.MediaType
import org.wordpress.android.mediapicker.model.MediaType.AUDIO
import org.wordpress.android.mediapicker.model.MediaType.DOCUMENT
import org.wordpress.android.mediapicker.model.MediaType.IMAGE
import org.wordpress.android.mediapicker.model.MediaType.VIDEO
import org.wordpress.android.mediapicker.model.UiString.UiStringRes
import org.wordpress.android.mediapicker.model.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtils
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.wordpress.android.mediapicker.api.R as MPApiR

class MediaLibrarySource(
    private val mediaStore: MediaStore,
    private val dispatcher: Dispatcher,
    private val bgDispatcher: CoroutineDispatcher,
    private val networkStatus: NetworkStatus,
    private val siteModel: SiteModel,
    private val mediaTypes: Set<MediaType>
) : MediaSource {
    private var loadContinuations = mutableMapOf<MimeType.Type, Continuation<OnMediaListFetched>>()

    init {
        dispatcher.register(this)
    }

    override suspend fun load(
        forced: Boolean,
        loadMore: Boolean,
        filter: String?
    ): MediaLoadingResult {
        if (!networkStatus.isConnected()) {
            return Failure(
                UiStringRes(MPApiR.string.no_network_title),
                htmlSubtitle = UiStringRes(R.string.no_network_message),
                image = MPApiR.drawable.img_illustration_cloud_off_152dp,
                data = if (loadMore) get(mediaTypes, filter) else listOf()
            )
        }
        return withContext(bgDispatcher) {
            val loadingResults = mediaTypes.map { mediaType ->
                async {
                    loadPage(
                        siteModel,
                        loadMore,
                        mediaType.toMimeType()
                    )
                }
            }.awaitAll()

            var error: String? = null
            var hasMore = false
            for (loadingResult in loadingResults) {
                if (loadingResult.isError) {
                    error = loadingResult.error.message
                    break
                } else {
                    hasMore = hasMore || loadingResult.canLoadMore
                }
            }
            if (error != null) {
                Failure(
                    UiStringRes(R.string.media_loading_failed),
                    htmlSubtitle = UiStringText(error),
                    image = MPApiR.drawable.img_illustration_cloud_off_152dp,
                    data = if (loadMore) get(mediaTypes, filter) else listOf()
                )
            } else {
                val data = get(mediaTypes, filter)
                if (filter.isNullOrEmpty() || data.isNotEmpty()) {
                    MediaLoadingResult.Success(data, hasMore)
                } else {
                    Empty(
                        UiStringRes(MPApiR.string.media_empty_search_list),
                        image = MPApiR.drawable.img_illustration_empty_results_216dp
                    )
                }
            }
        }
    }

    private suspend fun get(mediaTypes: Set<MediaType>, filter: String?): List<MediaItem> {
        return withContext(bgDispatcher) {
            mediaTypes.map { mediaType ->
                async {
                    if (filter == null) {
                        getFromDatabase(mediaType)
                    } else {
                        searchInDatabase(mediaType, filter)
                    }
                }
            }.fold(mutableListOf<MediaItem>()) { result, databaseItems ->
                result.addAll(databaseItems.await())
                result
            }.sortedByDescending { it.dataModified }
        }
    }

    private fun List<MediaModel>.toMediaItems(mediaType: MediaType): List<MediaItem> {
        return this.map { mediaModel ->
            MediaItem(
                RemoteMedia(
                    mediaModel.mediaId,
                    mediaModel.title,
                    mediaModel.url,
                    mediaModel.uploadDate
                ),
                mediaModel.url,
                mediaModel.title,
                mediaType,
                mediaModel.mimeType,
                DateTimeUtils.dateFromIso8601(mediaModel.uploadDate).time
            )
        }
    }

    private fun getFromDatabase(mediaType: MediaType): List<MediaItem> {
        return when (mediaType) {
            IMAGE -> mediaStore.getSiteImages(siteModel)
            VIDEO -> mediaStore.getSiteVideos(siteModel)
            AUDIO -> mediaStore.getSiteAudio(siteModel)
            DOCUMENT -> mediaStore.getSiteDocuments(siteModel)
        }.toMediaItems(mediaType)
    }

    private fun searchInDatabase(mediaType: MediaType, filter: String): List<MediaItem> {
        return when (mediaType) {
            IMAGE -> mediaStore.searchSiteImages(siteModel, filter)
            VIDEO -> mediaStore.searchSiteVideos(siteModel, filter)
            AUDIO -> mediaStore.searchSiteAudio(siteModel, filter)
            DOCUMENT -> mediaStore.searchSiteDocuments(siteModel, filter)
        }.toMediaItems(mediaType)
    }

    private suspend fun loadPage(siteModel: SiteModel, loadMore: Boolean, filter: MimeType.Type): OnMediaListFetched =
        suspendCoroutine { cont ->
            loadContinuations[filter] = cont
            val payload = FetchMediaListPayload(
                siteModel,
                NUM_MEDIA_PER_FETCH,
                loadMore,
                filter
            )
            dispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload))
        }

    private fun MediaType.toMimeType(): MimeType.Type {
        return when (this) {
            IMAGE -> MimeType.Type.IMAGE
            VIDEO -> MimeType.Type.VIDEO
            AUDIO -> MimeType.Type.AUDIO
            DOCUMENT -> MimeType.Type.APPLICATION
        }
    }

    @Suppress("Unused")
    @Subscribe(threadMode = MAIN)
    fun onMediaListFetched(event: OnMediaListFetched) {
        loadContinuations[event.mimeType]?.resume(event)
        loadContinuations.remove(event.mimeType)
    }

    companion object {
        const val NUM_MEDIA_PER_FETCH = 24
    }

    class Factory
    @Inject constructor(
        private val mediaStore: MediaStore,
        private val dispatcher: Dispatcher,
        private val bgDispatcher: CoroutineDispatcher,
        private val networkStatus: NetworkStatus,
        private val siteModel: SiteModel
    ) {
        fun build(mediaTypes: Set<MediaType>) =
            MediaLibrarySource(
                mediaStore,
                dispatcher,
                bgDispatcher,
                networkStatus,
                siteModel,
                mediaTypes
            )
    }
}
