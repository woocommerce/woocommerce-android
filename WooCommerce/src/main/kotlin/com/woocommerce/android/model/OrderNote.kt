package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

@Parcelize
data class OrderNote(
    val remoteNoteId: Long = 0L,
    val orderId: Long = 0L,
    val author: String = "",
    val dateCreated: Date = Date(),
    val isCustomerNote: Boolean,
    val isSystemNote: Boolean = false,
    val note: String
) : Parcelable

fun WCOrderNoteModel.toAppModel(): OrderNote {
    return OrderNote(
        remoteNoteId = noteId.value,
        orderId = orderId.value,
        author = author,
        dateCreated = DateTimeUtils.dateUTCFromIso8601(this.dateCreated) ?: Date(),
        isCustomerNote = isCustomerNote,
        isSystemNote = isSystemNote,
        note = note
    )
}
