package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

@Parcelize
data class OrderNote(
    val remoteNoteId: Long = 0L,
    val author: String = "",
    val dateCreated: Date = Date(),
    val isCustomerNote: Boolean,
    val isSystemNote: Boolean = false,
    val localOrderId: Int = 0,
    val localSiteId: Int = 0,
    val note: String
) : Parcelable {
    fun toDataModel() = WCOrderNoteModel().also { orderNoteModel ->
        orderNoteModel.isCustomerNote = this.isCustomerNote
        orderNoteModel.note = this.note
    }
}

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
