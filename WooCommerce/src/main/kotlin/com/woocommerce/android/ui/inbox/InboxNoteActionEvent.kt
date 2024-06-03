package com.woocommerce.android.ui.inbox

import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

sealed class InboxNoteActionEvent : Event() {
    data class OpenUrlEvent(val url: String) : InboxNoteActionEvent()
}
