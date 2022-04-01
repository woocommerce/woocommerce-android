package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R

/**
 * Similar to WCProductPropertyReadMoreView but without a caption and no preset horizontal margins
 */
class WCReadMoreTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private var textContent: MaterialTextView
    private var btnReadMore: MaterialTextView
    private val defaultMaxLines = context.resources.getInteger(R.integer.default_max_lines_read_more_textview)

    init {
        with(View.inflate(context, R.layout.wc_read_more_textview_layout, this)) {
            textContent = findViewById(R.id.textContent)
            btnReadMore = findViewById(R.id.btnReadMore)
        }
    }

    fun show(content: String, @StringRes dialogCaptionId: Int, maxLines: Int = defaultMaxLines) {
        if (textContent.text.toString() == content && textContent.maxLines == maxLines) {
            return
        }

        textContent.text = content

        textContent.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                textContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (textContent.lineCount > maxLines) {
                    textContent.maxLines = maxLines
                    btnReadMore.visibility = View.VISIBLE
                    btnReadMore.setOnClickListener { showFullContent(content, dialogCaptionId) }
                } else {
                    btnReadMore.visibility = View.GONE
                }
            }
        })
    }

    private fun showFullContent(content: String, @StringRes dialogCaptionId: Int) {
        val customView = View.inflate(context, R.layout.view_alert_dialog, null)
        customView.findViewById<MaterialTextView>(R.id.product_purchase_note).text = content
        MaterialAlertDialogBuilder(context)
            .setTitle(dialogCaptionId)
            .setView(customView)
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}
