package com.woocommerce.android.ui.inbox.domain

data class InboxNote(
    val id: Long,
    val title: String,
    val description: String,
    val dateCreated: String,
    val status: Status,
    val type: NoteType,
    val actions: List<InboxNoteAction>
) {
    enum class Status {
        UNACTIONED,
        ACTIONED,
        SNOOZED,
        UNKNOWN
    }

    enum class NoteType {
        INFO,
        MARKETING,
        SURVEY,
        WARNING
    }
}

data class InboxNoteAction(
    val id: Long,
    val label: String,
    val isPrimary: Boolean,
    val url: String,
)
