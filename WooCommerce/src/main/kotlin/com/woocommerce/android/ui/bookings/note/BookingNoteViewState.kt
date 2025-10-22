package com.woocommerce.android.ui.bookings.note

data class BookingNoteViewState(
    val initialNote: String = "",
    val editedNote: String = "",
    val onNoteChange: (String) -> Unit = {}
)
