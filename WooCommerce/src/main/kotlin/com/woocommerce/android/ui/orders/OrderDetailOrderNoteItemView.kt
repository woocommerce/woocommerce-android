package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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
        orderNote_note.text = getHtmlText(note.note)

        when {
            note.isCustomerNote -> {
                orderNote_type.text = context.getString(R.string.orderdetail_note_public)
                orderNote_icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_note_public))
            }
            note.isSystemNote -> {
                orderNote_type.text = context.getString(R.string.orderdetail_note_system)
                orderNote_icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_note_system))
            }
            else -> {
                orderNote_type.text = context.getString(R.string.orderdetail_note_private)
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
