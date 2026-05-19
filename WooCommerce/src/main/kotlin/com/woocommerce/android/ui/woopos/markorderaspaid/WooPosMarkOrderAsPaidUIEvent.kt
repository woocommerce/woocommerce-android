package com.woocommerce.android.ui.woopos.markorderaspaid

sealed class WooPosMarkOrderAsPaidUIEvent {
    data class NoteChanged(val newNote: String) : WooPosMarkOrderAsPaidUIEvent()
    data object ConfirmClicked : WooPosMarkOrderAsPaidUIEvent()
}
