package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.woocommerce.android.R

/**
 * TextView with a caption (header) and optional divider, useful for settings-related screens
 */
class WCCaptionedTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {
    private var view: View = View.inflate(context, R.layout.captioned_textview, this)

    fun show(caption: String, detail: String) {
        view.findViewById<TextView>(R.id.textCaption)?.text = caption
        view.findViewById<TextView>(R.id.textDetail)?.text = detail
    }
}
