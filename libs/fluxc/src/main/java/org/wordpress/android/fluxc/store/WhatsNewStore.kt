package org.wordpress.android.fluxc.store

import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel
import org.wordpress.android.fluxc.network.rest.wpcom.whatsnew.WhatsNewRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.whatsnew.WhatsNewRestClient.WhatsNewFetchedPayload
import org.wordpress.android.fluxc.persistence.WhatsNewMapper
import org.wordpress.android.fluxc.persistence.dao.WhatsNewDao
import org.wordpress.android.fluxc.store.WhatsNewStore.WhatsNewErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsNewStore @Inject internal constructor(
    private val whatsNewRestClient: WhatsNewRestClient,
    private val whatsNewDao: WhatsNewDao,
    private val coroutineEngine: CoroutineEngine
) {

    suspend fun fetchCachedAnnouncements() =
            coroutineEngine.withDefaultContext(T.API, this, "fetchWhatsNew") {
                return@withDefaultContext OnWhatsNewFetched(getAnnouncements(), true)
            }

    suspend fun fetchRemoteAnnouncements(versionName: String) =
            coroutineEngine.withDefaultContext(T.API, this, "fetchWhatsNew") {
                val fetchedWhatsNewPayload = whatsNewRestClient.fetchWhatsNew(versionName)

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
        return announcementsWithFeatures.map { WhatsNewMapper.toDomainModel(it) }
    }

    private fun updateAnnouncementCache(announcements: List<WhatsNewAnnouncementModel>?) {
        runBlocking {
            // Delete all existing data (features are cascade deleted)
            whatsNewDao.deleteAllAnnouncements()

            if (announcements.isNullOrEmpty()) {
                return@runBlocking
            }

            // Convert domain models to entities and insert
            val announcementsWithFeatures = announcements.map { WhatsNewMapper.fromDomainModel(it) }
            whatsNewDao.insertAnnouncementsWithFeatures(announcementsWithFeatures)
        }
    }

    data class OnWhatsNewFetched(
        val whatsNewItems: List<WhatsNewAnnouncementModel>? = null,
        val isFromCache: Boolean = false,
        val fetchError: WhatsNewFetchError? = null
    )

    data class WhatsNewFetchError(
        val type: WhatsNewErrorType,
        val message: String = ""
    )

    enum class WhatsNewErrorType {
        GENERIC_ERROR
    }
}
