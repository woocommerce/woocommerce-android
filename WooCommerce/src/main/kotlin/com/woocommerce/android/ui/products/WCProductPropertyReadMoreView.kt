package com.woocommerce.android.ui.products

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Button
import android.widget.TextView
import com.woocommerce.android.R

/**
 * Used by product detail to show product purchase note. If message is more than a certain number of lines it gets
 * cut off, and a "Read More" button appears below it.
 */
class WCProductPropertyReadMoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private var textCaption: TextView
    private var textContent: TextView
    private var buttonReadMore: Button

    init {
        with(View.inflate(context, R.layout.product_property_read_more_view, this)) {
            textCaption = findViewById(R.id.textCaption)
            textContent = findViewById(R.id.textContent)
            buttonReadMore = findViewById(R.id.buttonReadMore)
        }
    }

    fun show(caption: String, content: String, maxLines: Int) {
        textCaption.text = caption
        textContent.text = content

        textContent.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                textContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (textContent.lineCount > maxLines) {
                    textContent.maxLines = maxLines
                    buttonReadMore.visibility = View.VISIBLE
                } else {
                    buttonReadMore.visibility = View.GONE
                }
            }
        })
    }
}
