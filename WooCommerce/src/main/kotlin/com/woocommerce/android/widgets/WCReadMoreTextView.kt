package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.WcReadMoreTextviewLayoutBinding

/**
 * Simple TextView which shows a "Read more" button after a set number of lines
 */
class WCReadMoreTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private val defaultMaxLines = context.resources.getInteger(R.integer.default_max_lines_read_more_textview)
    private val binding = WcReadMoreTextviewLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    fun show(content: String, @StringRes dialogCaptionId: Int, maxLines: Int = defaultMaxLines) {
        if (binding.textContent.text.toString() == content && binding.textContent.maxLines == maxLines) {
            return
        }

        binding.textContent.text = content

        binding.textContent.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.textContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (binding.textContent.lineCount > maxLines) {
                    binding.textContent.maxLines = maxLines
                    binding.btnReadMore.visibility = View.VISIBLE
                    binding.btnReadMore.setOnClickListener { showFullContent(content, dialogCaptionId) }
                } else {
                    binding.btnReadMore.visibility = View.GONE
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
