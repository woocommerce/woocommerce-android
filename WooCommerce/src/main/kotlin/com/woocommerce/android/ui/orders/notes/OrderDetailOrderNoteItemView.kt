package com.woocommerce.android.ui.orders.notes

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.woocommerce.android.R
import com.woocommerce.android.model.OrderNote
import kotlinx.android.synthetic.main.order_detail_note_item.view.*

class OrderDetailOrderNoteItemView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_note_item, this)
    }

    fun initView(note: OrderNote) {
        orderNote_created.text = DateFormat.getTimeFormat(context).format(note.dateCreated)
        orderNote_note.text = getHtmlText(note.note)

        when {
            note.isCustomerNote -> {
                orderNote_icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_note_public))
            }
            note.isSystemNote -> {
                orderNote_icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_note_system))
            }
            else -> {
                orderNote_icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_note_private))
            }
        }
    }

    private fun getHtmlText(txt: String): Spanned {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            Html.fromHtml(txt)
        } else {
            Html.fromHtml(txt, Html.FROM_HTML_MODE_LEGACY)
        }
    }
}
