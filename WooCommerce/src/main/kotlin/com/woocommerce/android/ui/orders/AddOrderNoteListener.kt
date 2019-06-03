package com.woocommerce.android.ui.orders

interface AddOrderNoteListener {
    fun onRequestAddOrderNote(noteText: String, isCustomerNote: Boolean)
}
