package org.wordpress.android.fluxc.store

import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WhatsNewAction
import org.wordpress.android.fluxc.action.WhatsNewAction.FETCH_CACHED_ANNOUNCEMENT
import org.wordpress.android.fluxc.action.WhatsNewAction.FETCH_REMOTE_ANNOUNCEMENT
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel.WhatsNewAnnouncementFeature
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.whatsnew.WhatsNewRestClient
import org.wordpress.android.fluxc.persistence.dao.WhatsNewDao
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementEntity
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementFeatureEntity
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementWithFeatures
import org.wordpress.android.fluxc.store.WhatsNewStore.WhatsNewErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsNewStore @Inject internal constructor(
    private val whatsNewRestClient: WhatsNewRestClient,
    private val whatsNewDao: WhatsNewDao,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WhatsNewAction ?: return
        when (actionType) {
            FETCH_REMOTE_ANNOUNCEMENT -> {
                val versionName = (action.payload as WhatsNewFetchPayload).versionName
                val appId = (action.payload as WhatsNewFetchPayload).appId
                coroutineEngine.launch(AppLog.T.API, this, "FETCH_REMOTE_ANNOUNCEMENT") {
                    emitChange(fetchRemoteAnnouncements(versionName, appId))
                }
            }
            FETCH_CACHED_ANNOUNCEMENT -> {
                coroutineEngine.launch(AppLog.T.API, this, "FETCH_CACHED_ANNOUNCEMENT") {
                    emitChange(fetchCachedAnnouncements())
                }
            }
        }
    }

    suspend fun fetchCachedAnnouncements() =
            coroutineEngine.withDefaultContext(T.API, this, "fetchWhatsNew") {
                return@withDefaultContext OnWhatsNewFetched(getAnnouncements(), true)
            }

    suspend fun fetchRemoteAnnouncements(versionName: String, appId: WhatsNewAppId) =
            coroutineEngine.withDefaultContext(T.API, this, "fetchWhatsNew") {
                val fetchedWhatsNewPayload = whatsNewRestClient.fetchWhatsNew(versionName, appId)

                return@withDefaultContext if (!fetchedWhatsNewPayload.isError) {
                    val fetchedAnnouncements = fetchedWhatsNewPayload.whatsNewItems
                    updateAnnouncementCache(fetchedAnnouncements)
                    OnWhatsNewFetched(fetchedAnnouncements)
                } else {
                    OnWhatsNewFetched(
                            fetchError = WhatsNewFetchError(GENERIC_ERROR, fetchedWhatsNewPayload.error.message)
                    )
                }
            }

    private fun getAnnouncements(): List<WhatsNewAnnouncementModel> {
        val announcementsWithFeatures = runBlocking { whatsNewDao.getAnnouncementsWithFeatures() }
        return announcementsWithFeatures.map { it.toDomainModel() }
    }

    private fun updateAnnouncementCache(announcements: List<WhatsNewAnnouncementModel>?) {
        runBlocking {
            // Delete all existing data (features are cascade deleted)
            whatsNewDao.deleteAllAnnouncements()

            if (announcements.isNullOrEmpty()) {
                return@runBlocking
            }

            // Convert domain models to entities
            val announcementEntities = announcements.map { it.toEntity() }
            val featureEntities = announcements.flatMap { announcement ->
                announcement.features.map { feature ->
                    feature.toEntity(announcement.announcementVersion)
                }
            }

            // Insert new data
            whatsNewDao.insertAnnouncements(announcementEntities)
            whatsNewDao.insertFeatures(featureEntities)
        }
    }

    private fun WhatsNewAnnouncementWithFeatures.toDomainModel(): WhatsNewAnnouncementModel {
        return WhatsNewAnnouncementModel(
            announcementVersion = announcement.announcementId.value.toInt(),
            minimumAppVersion = announcement.minimumAppVersion,
            maximumAppVersion = announcement.maximumAppVersion,
            appVersionTargets = announcement.appVersionTargets,
            isLocalized = announcement.localized,
            features = features.map { it.toDomainModel() }
        )
    }

    private fun WhatsNewAnnouncementFeatureEntity.toDomainModel(): WhatsNewAnnouncementFeature {
        return WhatsNewAnnouncementFeature(
            title = title,
            subtitle = subtitle,
            iconBase64 = iconBase64,
            iconUrl = iconUrl
        )
    }

    private fun WhatsNewAnnouncementModel.toEntity(): WhatsNewAnnouncementEntity {
        return WhatsNewAnnouncementEntity(
            announcementId = RemoteId(announcementVersion.toLong()),
            minimumAppVersion = minimumAppVersion,
            maximumAppVersion = maximumAppVersion,
            appVersionTargets = appVersionTargets,
            localized = isLocalized
        )
    }

    private fun WhatsNewAnnouncementFeature.toEntity(announcementVersion: Int): WhatsNewAnnouncementFeatureEntity {
        return WhatsNewAnnouncementFeatureEntity(
            announcementId = RemoteId(announcementVersion.toLong()),
            title = title ?: "",
            subtitle = subtitle,
            iconUrl = iconUrl,
            iconBase64 = iconBase64
        )
    }

    override fun onRegister() {
        AppLog.d(API, WhatsNewStore::class.java.simpleName + " onRegister")
    }

    class WhatsNewFetchPayload(
        val versionName: String,
        val appId: WhatsNewAppId
    ) : Payload<BaseNetworkError>()

    class WhatsNewFetchedPayload(
        val whatsNewItems: List<WhatsNewAnnouncementModel>? = null
    ) : Payload<BaseNetworkError>()

    data class OnWhatsNewFetched(
        val whatsNewItems: List<WhatsNewAnnouncementModel>? = null,
        val isFromCache: Boolean = false,
        val fetchError: WhatsNewFetchError? = null
    ) : Store.OnChanged<WhatsNewFetchError>() {
        init {
            // we allow setting error from constructor, so it will be a part of data class
            // and used during comparison, so we can test error events
            this.error = fetchError
        }
    }

    data class WhatsNewFetchError(
        val type: WhatsNewErrorType,
        val message: String = ""
    ) : OnChangedError

    enum class WhatsNewErrorType {
        GENERIC_ERROR
    }

    enum class WhatsNewAppId(val id: Int) {
        WOO_ANDROID(3),
    }
}
