package com.woocommerce.android.ui.inbox

data class InboxNoteUi(
    val id: Long,
    val title: String,
    val description: String,
    val dateCreated: String,
    val isSurvey: Boolean,
    val isActioned: Boolean,
    val actions: List<InboxNoteActionUi>
)
