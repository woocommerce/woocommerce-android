package com.woocommerce.android.ui.woopos.markorderascomplete

sealed class WooPosMarkOrderAsCompleteUIEvent {
    data class NoteChanged(val newNote: String) : WooPosMarkOrderAsCompleteUIEvent()
    data object ConfirmClicked : WooPosMarkOrderAsCompleteUIEvent()
}
