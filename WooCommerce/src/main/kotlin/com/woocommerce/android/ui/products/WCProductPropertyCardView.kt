package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
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
    private var view: View = View.inflate(context, R.layout.product_property_cardview_layout, this)

    fun show(caption: String?) {
        val captionTextView = view.findViewById<MaterialTextView>(R.id.cardCaptionText)
        val divider = view.findViewById<View>(R.id.cardCaptionDivider)
        if (caption.isNullOrBlank()) {
            captionTextView.visibility = View.GONE
            divider.hide()
        } else {
            captionTextView.visibility = View.VISIBLE
            captionTextView.text = caption
            divider.show()
        }
    }
}
