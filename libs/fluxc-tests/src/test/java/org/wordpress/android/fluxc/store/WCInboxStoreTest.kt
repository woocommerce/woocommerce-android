package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxNoteActionDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxNoteDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxRestClient
import org.wordpress.android.fluxc.persistence.dao.InboxNotesDao
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity.LocalInboxNoteStatus.Unactioned
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions
import org.wordpress.android.fluxc.store.WCInboxStore.Companion.DEFAULT_PAGE
import org.wordpress.android.fluxc.store.WCInboxStore.Companion.DEFAULT_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCInboxStore.Companion.INBOX_NOTE_TYPES_FOR_APPS
import org.wordpress.android.fluxc.store.WCInboxStore.Companion.MAX_PAGE_SIZE_FOR_DELETING_NOTES
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

class WCInboxStoreTest {
    private val restClient: InboxRestClient = mock()
    private val inboxNotesDao: InboxNotesDao = mock()

    private val sut = WCInboxStore(
        restClient,
        initCoroutineEngine(),
        inboxNotesDao
    )

    @Test
    fun `Given notes are fetched successfully, when notes fetched, then save them into DB`() =
        test {
            givenNotesAreFetchedSuccesfully()

            sut.fetchInboxNotes(ANY_SITE_MODEL)

            verify(inboxNotesDao).deleteAllAndInsertInboxNotes(
                ANY_SITE_MODEL.localId(), *INBOX_NOTES_WITH_ACTIONS_ENTITY.toTypedArray()
            )
        }

    @Test
    fun `Given notes are fetched successfully, when notes fetched, then return WooResult`() =
        test {
            givenNotesAreFetchedSuccesfully()

            val result = sut.fetchInboxNotes(ANY_SITE_MODEL)

            Assertions.assertThat(result).isEqualTo(WooResult(Unit))
        }

    @Test
    fun `Given notes fetching error, when notes fetched, then notes are not saved`() =
        test {
            givenErrorFetchingNotes()

            sut.fetchInboxNotes(ANY_SITE_MODEL)

            verify(inboxNotesDao, never()).insertOrUpdateInboxNote(any())
        }

    @Test
    fun `Given notes fetching error, when notes fetched, then returns WooResult error`() =
        test {
            givenErrorFetchingNotes()

            val result = sut.fetchInboxNotes(ANY_SITE_MODEL)

            Assertions.assertThat(result).isEqualTo(ANY_WOO_RESULT_WITH_ERROR)
        }

    @Test
    fun `Given notes loaded, when delete all notes, then clears cached notes from db`() =
        test {
            givenNotesSavedInDbForSite(A_SITE_ID)
            givenNotesDeletedSuccessfully()

            sut.deleteNotesForSite(ANY_SITE_MODEL)

            verify(inboxNotesDao).deleteInboxNotesForSite(LocalId(A_SITE_ID))
        }

    @Test
    fun `Given notes loaded, when delete all notes, requests notes deletion for site to the API`() =
        test {
            givenNotesSavedInDbForSite(A_SITE_ID)
            givenNotesDeletedSuccessfully()

            sut.deleteNotesForSite(ANY_SITE_MODEL)

            verify(restClient).deleteAllNotesForSite(
                ANY_SITE_MODEL,
                DEFAULT_PAGE,
                MAX_PAGE_SIZE_FOR_DELETING_NOTES,
                INBOX_NOTE_TYPES_FOR_APPS
            )
        }

    private suspend fun givenNotesAreFetchedSuccesfully() {
        whenever(
            restClient.fetchInboxNotes(
                ANY_SITE_MODEL,
                DEFAULT_PAGE,
                DEFAULT_PAGE_SIZE,
                INBOX_NOTE_TYPES_FOR_APPS
            )
        ).thenReturn(WooPayload(arrayOf(ANY_INBOX_NOTE_DTO)))
    }

    private suspend fun givenNotesDeletedSuccessfully() {
        whenever(
            restClient.deleteAllNotesForSite(
                ANY_SITE_MODEL,
                DEFAULT_PAGE,
                MAX_PAGE_SIZE_FOR_DELETING_NOTES,
                INBOX_NOTE_TYPES_FOR_APPS
            )
        ).thenReturn(WooPayload(Unit))
    }

    private suspend fun givenErrorFetchingNotes() {
        whenever(
            restClient.fetchInboxNotes(
                ANY_SITE_MODEL,
                DEFAULT_PAGE,
                DEFAULT_PAGE_SIZE,
                INBOX_NOTE_TYPES_FOR_APPS
            )
        ).thenReturn(WooPayload(ANY_WOO_API_ERROR))
    }

    private fun givenNotesSavedInDbForSite(localSiteId: Int) {
        whenever(
            inboxNotesDao.getInboxNotesForSite(LocalId(localSiteId))
        ).thenReturn(listOf(ANY_INBOX_NOTE_ENTITY))
    }

    private companion object {
        const val AN_INBOX_NOTE_REMOTE_ID: Long = 1
        const val A_SITE_ID: Int = 12324
        val ANY_SITE_MODEL = SiteModel().apply { id = A_SITE_ID }
        val ANY_WOO_API_ERROR = WooError(GENERIC_ERROR, UNKNOWN)
        val ANY_WOO_RESULT_WITH_ERROR: WooResult<Unit> = WooResult(ANY_WOO_API_ERROR)
        val ANY_INBOX_NOTE_ACTION_DTO = InboxNoteActionDto(
            id = 2,
            name = "action",
            label = "action",
            query = "",
            status = null,
            primary = true,
            actionedText = "",
            nonceAction = "",
            nonceName = "",
            url = "www.automattic.com"
        )
        val ANY_INBOX_NOTE_DTO = InboxNoteDto(
            id = AN_INBOX_NOTE_REMOTE_ID,
            name = "",
            type = "",
            status = "",
            source = "",
            actions = listOf(ANY_INBOX_NOTE_ACTION_DTO),
            locale = "",
            title = "",
            content = "",
            layout = "",
            dateCreated = "",
            dateReminder = ""
        )
        val ANY_INBOX_NOTE_ENTITY = InboxNoteEntity(
            localId = 1,
            remoteId = AN_INBOX_NOTE_REMOTE_ID,
            localSiteId = LocalId(A_SITE_ID),
            name = "",
            type = "",
            status = Unactioned,
            source = "",
            title = "",
            content = "",
            dateCreated = "",
            dateReminder = ""
        )
        val INBOX_NOTES_WITH_ACTIONS_ENTITY = listOf(
            InboxNoteWithActions(
                inboxNote = ANY_INBOX_NOTE_DTO.toInboxNoteEntity(ANY_SITE_MODEL.localId()),
                noteActions = listOf(
                    ANY_INBOX_NOTE_ACTION_DTO.toDataModel(
                        ANY_SITE_MODEL.localId()
                    )
                )
            )
        )
    }
}
