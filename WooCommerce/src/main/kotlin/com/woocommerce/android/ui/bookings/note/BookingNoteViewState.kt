package com.woocommerce.android.ui.bookings.note

data class BookingNoteViewState(
    val initialNote: String = "",
    val editedNote: String? = null,
    val noteSaveStatus: NoteSaveStatus = NoteSaveStatus.Idle,
    val onNoteChange: (String) -> Unit = {},
    val onSaveClicked: () -> Unit = {},
) {

    val isSaveVisible: Boolean
        get() = initialNote.trim() != editedNote.orEmpty().trim()

    val isSaveEnabled: Boolean
        get() = noteSaveStatus == NoteSaveStatus.Idle

    val noteEditable: Boolean
        get() = noteSaveStatus == NoteSaveStatus.Idle
}

sealed interface NoteSaveStatus {
    data object Idle : NoteSaveStatus
    data object InProgress : NoteSaveStatus
}
