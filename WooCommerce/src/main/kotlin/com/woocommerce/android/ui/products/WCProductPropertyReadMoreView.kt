package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
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
    private var textCaption: MaterialTextView
    private var textContent: MaterialTextView
    private var btnReadMore: MaterialButton

    init {
        with(View.inflate(context, R.layout.product_property_read_more_view_layout, this)) {
            textCaption = findViewById(R.id.textCaption)
            textContent = findViewById(R.id.textContent)
            btnReadMore = findViewById(R.id.btnReadMore)
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
                    btnReadMore.visibility = View.VISIBLE
                    btnReadMore.setOnClickListener { showFullContent(caption, content) }
                } else {
                    btnReadMore.visibility = View.GONE
                }
            }
        })
    }

    private fun showFullContent(caption: String, content: String) {
        val customView = View.inflate(context, R.layout.view_alert_dialog, null)
        customView.findViewById<MaterialTextView>(R.id.product_purchase_note).text = content
        MaterialAlertDialogBuilder(context)
                .setTitle(caption)
                .setView(customView)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
    }
}
