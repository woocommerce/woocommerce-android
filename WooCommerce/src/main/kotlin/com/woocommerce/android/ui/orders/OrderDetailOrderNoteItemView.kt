package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Build
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.text.Html
import android.text.Spanned
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

    fun initView(note: WCOrderNoteModel, showProgress: Boolean) {
        orderNote_created.text = DateUtils.getFriendlyLongDateAtTimeString(context, note.dateCreated).capitalize()
        orderNote_note.text = getHtmlText(note.note)

        if (note.isCustomerNote) {
            orderNote_type.text = context.getString(R.string.orderdetail_note_public)
            orderNote_icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_note_public))
        } else {
            orderNote_type.text = context.getString(R.string.orderdetail_note_private)
            orderNote_icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_note_private))
        }

        orderNote_progress.visibility = if (showProgress) View.VISIBLE else View.GONE
        orderNote_icon.visibility = if (showProgress) View.INVISIBLE else View.VISIBLE
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
