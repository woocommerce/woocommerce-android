package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxNoteDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxRestClient
import org.wordpress.android.fluxc.persistence.dao.InboxNotesDao
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCInboxStore @Inject constructor(
    private val restClient: InboxRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val inboxNotesDao: InboxNotesDao
) {
    companion object {
        // API has a restriction were no more than 25 notes can be deleted at once so when deleting all notes
        // for site we have to calculate the number of "pages" to be deleted. This only applies for deletion.
        const val MAX_PAGE_SIZE_FOR_DELETING_NOTES = 25
        const val DEFAULT_PAGE_SIZE = 100
        const val DEFAULT_PAGE = 1
        val INBOX_NOTE_TYPES_FOR_APPS = arrayOf(
            "info",
            "survey",
            "marketing",
            "warning"
        )
    }

    suspend fun fetchInboxNotes(
        site: SiteModel,
        page: Int = DEFAULT_PAGE,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        inboxNoteTypes: Array<String> = INBOX_NOTE_TYPES_FOR_APPS
    ): WooResult<Unit> =
        coroutineEngine.withDefaultContext(API, this, "fetchInboxNotes") {
            val response = restClient.fetchInboxNotes(site, page, pageSize, inboxNoteTypes)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    saveInboxNotes(response.result, site)
                    WooResult(Unit)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }

    fun observeInboxNotes(site: SiteModel): Flow<List<InboxNoteWithActions>> =
        inboxNotesDao.observeInboxNotes(site.localId())
            .distinctUntilChanged()

    suspend fun markInboxNoteAsActioned(
        site: SiteModel,
        noteId: Long,
        actionId: Long
    ): WooResult<Unit> =
        coroutineEngine.withDefaultContext(API, this, "fetchInboxNotes") {
            val response = restClient.markInboxNoteAsActioned(site, noteId, actionId)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    markNoteAsActionedLocally(site, response.result)
                    WooResult(Unit)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }

    suspend fun deleteNote(
        site: SiteModel,
        noteId: Long
    ): WooResult<Unit> =
        coroutineEngine.withDefaultContext(API, this, "fetchInboxNotes") {
            val response = restClient.deleteNote(site, noteId)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    inboxNotesDao.deleteInboxNote(noteId, site.localId())
                    WooResult(Unit)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }

    suspend fun deleteNotesForSite(
        site: SiteModel,
        pageSize: Int = MAX_PAGE_SIZE_FOR_DELETING_NOTES,
        inboxNoteTypes: Array<String> = INBOX_NOTE_TYPES_FOR_APPS
    ): WooResult<Unit> =
        coroutineEngine.withDefaultContext(API, this, "fetchInboxNotes") {
            var latestResult = WooResult(Unit)
            for (page in 1..getNumberOfPagesToDelete(site)) {
                latestResult = restClient
                    .deleteAllNotesForSite(site, page, pageSize, inboxNoteTypes)
                    .asWooResult()
                if (latestResult.isError) break
            }
            if (!latestResult.isError) {
                inboxNotesDao.deleteInboxNotesForSite(site.localId())
            }
            latestResult
        }

    private fun getNumberOfPagesToDelete(site: SiteModel): Int {
        val sizeOfCachedNotesForSite = inboxNotesDao.getInboxNotesForSite(site.localId()).size
        var numberOfPagesToDelete = sizeOfCachedNotesForSite / MAX_PAGE_SIZE_FOR_DELETING_NOTES
        if (sizeOfCachedNotesForSite % MAX_PAGE_SIZE_FOR_DELETING_NOTES > 0) {
            numberOfPagesToDelete++
        }
        return numberOfPagesToDelete
    }

    @Suppress("SpreadOperator")
    private suspend fun saveInboxNotes(result: Array<InboxNoteDto>, site: SiteModel) {
        val notesWithActions = result.map { it.toInboxNoteWithActionsEntity(site.localId()) }
        inboxNotesDao.deleteAllAndInsertInboxNotes(site.localId(), *notesWithActions.toTypedArray())
    }

    private suspend fun markNoteAsActionedLocally(site: SiteModel, updatedNote: InboxNoteDto) {
        val noteWithActionsEntity = updatedNote.toInboxNoteWithActionsEntity(site.localId())
        inboxNotesDao.updateNote(site.localId(), noteWithActionsEntity)
    }
}
