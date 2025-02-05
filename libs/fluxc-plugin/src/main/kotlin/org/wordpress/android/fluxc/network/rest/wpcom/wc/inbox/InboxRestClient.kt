package org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class InboxRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchInboxNotes(
        site: SiteModel,
        page: Int,
        pageSize: Int,
        inboxNoteTypes: Array<String>
    ): WooPayload<Array<InboxNoteDto>> {
        val url = WOOCOMMERCE.admin.notes.pathV4Analytics

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = mapOf(
                "page" to page.toString(),
                "per_page" to pageSize.toString(),
                "type" to inboxNoteTypes.joinToString(separator = ",")
            ),
            clazz = Array<InboxNoteDto>::class.java
        )
        return response.toWooPayload()
    }

    suspend fun markInboxNoteAsActioned(
        site: SiteModel,
        inboxNoteId: Long,
        inboxNoteActionId: Long
    ): WooPayload<InboxNoteDto> {
        val url = WOOCOMMERCE.admin.notes.note(inboxNoteId).action.item(inboxNoteActionId).pathV4Analytics

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = InboxNoteDto::class.java
        )
        return response.toWooPayload()
    }

    suspend fun deleteNote(
        site: SiteModel,
        inboxNoteId: Long
    ): WooPayload<Unit> {
        val url = WOOCOMMERCE.admin.notes.delete.note(inboxNoteId).pathV4Analytics

        val response = wooNetwork.executeDeleteGsonRequest(
            site = site,
            path = url,
            clazz = Unit::class.java
        )
        return response.toWooPayload()
    }

    suspend fun deleteAllNotesForSite(
        site: SiteModel,
        page: Int,
        pageSize: Int,
        inboxNoteTypes: Array<String>
    ): WooPayload<Unit> {
        val url = WOOCOMMERCE.admin.notes.delete.all.pathV4Analytics

        val response = wooNetwork.executeDeleteGsonRequest(
            site = site,
            path = url,
            clazz = Array<InboxNoteDto>::class.java,
            params = mapOf(
                "page" to page.toString(),
                "per_page" to pageSize.toString(),
                "type" to inboxNoteTypes.joinToString(separator = ",")
            )
        )
        return response.toWooPayload { }
    }
}
