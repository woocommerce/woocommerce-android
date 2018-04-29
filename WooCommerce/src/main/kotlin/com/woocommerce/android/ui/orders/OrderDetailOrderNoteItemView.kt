package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.util.DateUtils
import kotlinx.android.synthetic.main.order_detail_note_item.view.*
import org.wordpress.android.fluxc.model.WCOrderNoteModel

class OrderDetailOrderNoteItemView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_note_item, this)
    }

    fun initView(note: WCOrderNoteModel) {
        orderNote_created.text = DateUtils.getFriendlyLongDateAtTimeString(context, note.dateCreated).capitalize()
        orderNote_note.text = note.note

        orderNote_type.text = when (note.customerNote) {
            true -> context.getString(R.string.orderdetail_note_public)
            else -> context.getString(R.string.orderdetail_note_private)
        }
    }
}
