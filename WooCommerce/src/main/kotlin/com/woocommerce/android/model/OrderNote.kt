package com.woocommerce.android.model

import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

data class OrderNote(
    val remoteNoteId: Long,
    val author: String,
    val dateCreated: Date,
    val isCustomerNote: Boolean,
    val isSystemNote: Boolean,
    val localOrderId: Int,
    val localSiteId: Int,
    val note: String
)

fun WCOrderNoteModel.toAppModel(): OrderNote {
    return OrderNote(
            remoteNoteId,
            author,
            DateTimeUtils.dateUTCFromIso8601(this.dateCreated) ?: Date(),
            isCustomerNote,
            isSystemNote,
            localOrderId,
            localSiteId,
            note
    )
}
