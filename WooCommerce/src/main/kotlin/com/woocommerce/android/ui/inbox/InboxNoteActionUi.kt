package com.woocommerce.android.ui.inbox

import androidx.annotation.ColorRes

data class InboxNoteActionUi(
    val id: Long,
    val parentNoteId: Long,
    val label: String,
    @ColorRes val textColor: Int,
    val url: String,
    val isDismissing: Boolean = false,
    val onClick: (Long, Long) -> Unit
)
