package org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.InboxNoteActionEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity.LocalInboxNoteStatus
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity.LocalInboxNoteStatus.Actioned
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity.LocalInboxNoteStatus.Snoozed
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity.LocalInboxNoteStatus.Unactioned
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions

data class InboxNoteDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String?,
    @SerializedName("status") val status: String,
    @SerializedName("source") val source: String?,
    @SerializedName("actions") val actions: List<InboxNoteActionDto> = emptyList(),
    @SerializedName("locale") val locale: String?,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("layout") val layout: String?,
    @SerializedName("date_created") val dateCreated: String,
    @SerializedName("date_reminder") val dateReminder: String?
) {
    fun toInboxNoteWithActionsEntity(localSiteId: LocalId) =
        InboxNoteWithActions(
            inboxNote = toInboxNoteEntity(localSiteId),
            noteActions = actions.map { it.toDataModel(localSiteId) }
        )

    fun toInboxNoteEntity(localSiteId: LocalId) =
        InboxNoteEntity(
            remoteId = id,
            localSiteId = localSiteId,
            name = name,
            title = title,
            content = content,
            dateCreated = dateCreated,
            status = status.toInboxNoteStatus(),
            source = source,
            type = type,
            dateReminder = dateReminder
        )

    private fun String.toInboxNoteStatus() =
        when {
            this == STATUS_UNACTIONED -> Unactioned
            this == STATUS_ACTIONED -> Actioned
            this == STATUS_SNOOZED -> Snoozed
            else -> LocalInboxNoteStatus.Unknown
        }

    private companion object {
        const val STATUS_UNACTIONED = "unactioned"
        const val STATUS_ACTIONED = "actioned"
        const val STATUS_SNOOZED = "snoozed"
    }
}

data class InboxNoteActionDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("label") val label: String,
    @SerializedName("query") val query: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("primary") val primary: Boolean = false,
    @SerializedName("actioned_text") val actionedText: String?,
    @SerializedName("nonce_action") val nonceAction: String?,
    @SerializedName("nonce_name") val nonceName: String?,
    @SerializedName("url") val url: String
) {
    fun toDataModel(localSiteId: LocalId) =
        InboxNoteActionEntity(
            remoteId = id,
            inboxNoteLocalId = 0,
            localSiteId = localSiteId,
            name = name,
            label = label,
            url = url,
            query = query,
            status = status,
            primary = primary,
            actionedText = actionedText
        )
}
