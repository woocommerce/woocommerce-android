package com.woocommerce.android.ui.inbox.domain

data class InboxNote(
    val id: Long,
    val title: String,
    val description: String,
    val dateCreated: String,
    val status: Status,
    val actions: List<InboxNoteAction>
) {
    enum class Status {
        Unactioned,
        Actioned,
        Snoozed,
        Unknown
    }
}

data class InboxNoteAction(
    val id: Long,
    val label: String,
    val isPrimary: Boolean,
    val url: String
)
