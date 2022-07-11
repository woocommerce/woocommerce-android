package com.woocommerce.android.ui.orders.notes

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailNoteItemBinding
import com.woocommerce.android.model.OrderNote

class OrderDetailOrderNoteItemView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailNoteItemBinding.inflate(LayoutInflater.from(ctx), this, true)

    @SuppressLint("SetTextI18n")
    fun initView(note: OrderNote) {
        val date = DateFormat.getTimeFormat(context).format(note.dateCreated)
        val type = when {
            note.isCustomerNote -> context.getString(R.string.orderdetail_note_public)
            note.isSystemNote -> context.getString(R.string.orderdetail_note_system)
            else -> context.getString(R.string.orderdetail_note_private)
        }
        val header = if (note.isSystemNote) "$date ($type)" else "$date - ${note.author} ($type)"

        binding.orderNoteHeader.text = header
        binding.orderNoteNote.text = HtmlCompat.fromHtml(note.note, HtmlCompat.FROM_HTML_MODE_LEGACY)

        @DrawableRes val drawableId = when {
            note.isCustomerNote -> R.drawable.ic_note_public
            note.isSystemNote -> R.drawable.ic_note_system
            else -> R.drawable.ic_note_private
        }
        binding.orderNoteIcon.setImageDrawable(ContextCompat.getDrawable(context, drawableId))
    }
}
