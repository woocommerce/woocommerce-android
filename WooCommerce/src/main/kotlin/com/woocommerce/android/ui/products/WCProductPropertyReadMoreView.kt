package com.woocommerce.android.ui.products

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AlertDialog
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.TextView
import com.woocommerce.android.R

/**
 * Used by product detail to show product purchase note. If content is more than a certain number of lines it gets
 * cut off, and a "Read More" button appears below it.
 */
class WCProductPropertyReadMoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private var textCaption: TextView
    private var textContent: TextView
    private var textReadMore: TextView

    init {
        with(View.inflate(context, R.layout.product_property_read_more_view, this)) {
            textCaption = findViewById(R.id.textCaption)
            textContent = findViewById(R.id.textContent)
            textReadMore = findViewById(R.id.textReadMore)
        }
    }

    fun show(caption: String, content: String, maxLines: Int) {
        // go no further if nothing has changed
        if (textContent.text.toString().equals(content) && textContent.maxLines == maxLines) {
            return
        }

        textCaption.text = caption
        textContent.text = content

        textContent.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                textContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (textContent.lineCount > maxLines) {
                    textContent.maxLines = maxLines
                    textReadMore.visibility = View.VISIBLE
                    textReadMore.setOnClickListener { showFullContent(caption, content) }
                } else {
                    textReadMore.visibility = View.GONE
                }
            }
        })
    }

    private fun showFullContent(caption: String, content: String) {
        AlertDialog.Builder(context)
                .setTitle(caption)
                .setMessage(content)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
    }
}
