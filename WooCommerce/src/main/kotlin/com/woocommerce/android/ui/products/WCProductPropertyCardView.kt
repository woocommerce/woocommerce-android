package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show

/**
 * CardView with an optional caption (title), used for product detail properties
 */
class WCProductPropertyCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : MaterialCardView(context, attrs, defStyle) {
    fun show(caption: String?) {
        val captionTextView = findViewById<TextView>(R.id.cardCaptionText)
        if (caption.isNullOrBlank()) {
            captionTextView.visibility = View.GONE
            findViewById<View>(R.id.cardCaptionDivider).hide()
        } else {
            captionTextView.visibility = View.VISIBLE
            captionTextView.text = caption
            findViewById<View>(R.id.cardCaptionDivider).show()
        }
    }
}
