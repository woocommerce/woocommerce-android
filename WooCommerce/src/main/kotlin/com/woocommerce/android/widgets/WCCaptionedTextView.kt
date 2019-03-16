package com.woocommerce.android.widgets

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.widget.RatingBar
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.util.WooLog

/**
 * TextView with a caption (header), detail, and optional ratingBar
 */
class WCCaptionedTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
        ConstraintLayout(context, attrs, defStyle) {
    private var view: View = View.inflate(context, R.layout.captioned_textview, this)
    private var captionText: TextView
    private var detailText: TextView
    private var ratingBar: RatingBar

    init {
        captionText = view.findViewById(R.id.textCaption)
        detailText = view.findViewById(R.id.textDetail)
        ratingBar = view.findViewById(R.id.ratingBar)
    }

    fun show(caption: String, detail: String?) {
        captionText.text = caption
        if (detail.isNullOrEmpty()) {
            detailText.visibility = View.GONE
        } else {
            detailText.text = detail
        }
    }

    fun setRating(ratingStr: String) {
        try {
            ratingBar.rating = ratingStr.toFloat()
            detailText.visibility = View.GONE
            ratingBar.visibility = View.VISIBLE
        } catch (e: NumberFormatException) {
            WooLog.e(WooLog.T.UTILS, e)
        }
    }
}
