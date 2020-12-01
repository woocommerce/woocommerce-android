package com.woocommerce.android.ui.orders.notes

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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
    fun initView(note: OrderNote, showBottomPadding: Boolean) {
        val date = DateFormat.getTimeFormat(context).format(note.dateCreated)
        val type = when {
            note.isCustomerNote -> context.getString(R.string.orderdetail_note_public)
            note.isSystemNote -> context.getString(R.string.orderdetail_note_system)
            else -> context.getString(R.string.orderdetail_note_private)
        }
        val header = if (note.isSystemNote) "$date ($type)" else "$date - ${note.author} ($type)"

        binding.orderNoteHeader.text = header
        binding.orderNoteNote.text = getHtmlText(note.note)

        when {
            note.isCustomerNote -> {
                binding.orderNoteIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_note_public))
            }
            note.isSystemNote -> {
                binding.orderNoteIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_note_system))
            }
            else -> {
                binding.orderNoteIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_note_private))
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
