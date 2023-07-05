package com.woocommerce.android.ui.products

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R

class WCProductPropertyButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private var view: View = View.inflate(context, R.layout.product_property_button_view_layout, this)

    fun show(
        text: String,
        icon: Drawable?,
        onClick: () -> Unit,
//        tooltipTitle: String?,
//        tooltipDescription: String?,
//        tooltipIcon: Drawable?
    ) {
        with(view.findViewById<MaterialButton>(R.id.productButton)) {
            this.text = text
            this.icon = icon
            this.setOnClickListener { onClick() }
        }
    }

    fun show(
        text: String,
        icon: Drawable?,
        onClick: () -> Unit,
        linkText: Spanned,
        onLinkClick: () -> Unit,
    ) {
        with(view.findViewById<MaterialButton>(R.id.productButton)) {
            this.text = text
            this.icon = icon
            this.setOnClickListener { onClick() }
        }

        with(view.findViewById<MaterialTextView>(R.id.link)) {
            this.text = linkText
            this.setOnClickListener { onLinkClick() }
        }
    }
}
