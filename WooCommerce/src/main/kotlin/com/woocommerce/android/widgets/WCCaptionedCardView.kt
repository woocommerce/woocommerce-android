package com.woocommerce.android.widgets

import android.content.Context
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.woocommerce.android.R

/**
 * CardView with an optional caption (title), useful for settings-related screens
 */
class WCCaptionedCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : CardView(context, attrs, defStyle) {
    private var view: View = View.inflate(context, R.layout.captioned_cardview, this)

    fun show(caption: String?) {
        with(view.findViewById<TextView>(R.id.cardCaptionText)) {
            if (caption.isNullOrBlank()) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                text = caption
            }
        }
    }
}
