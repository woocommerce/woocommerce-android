package com.woocommerce.android.ui.products

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.util.WooLog

/**
 * TextView with a caption (header), detail, and optional ratingBar, used by product detail
 */
class WCProductPropertyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) :
        ConstraintLayout(context, attrs, defStyle) {
    private var view: View? = null
    private var captionText: TextView? = null
    private var detailText: TextView? = null
    private var ratingBar: RatingBar? = null

    fun show(orientation: Int, caption: String, detail: String?) {
        ensureViewCreated(orientation)

        captionText?.text = caption
        if (detail.isNullOrEmpty()) {
            detailText?.visibility = View.GONE
        } else {
            detailText?.text = detail
        }
    }

    fun setRating(ratingStr: String) {
        ensureViewCreated()

        try {
            ratingBar?.rating = ratingStr.toFloat()
            detailText?.visibility = View.GONE
            ratingBar?.visibility = View.VISIBLE
        } catch (e: NumberFormatException) {
            WooLog.e(WooLog.T.UTILS, e)
        }
    }

    private fun ensureViewCreated(orientation: Int = LinearLayout.VERTICAL) {
        if (view == null) {
            view = if (orientation == LinearLayout.VERTICAL) {
                View.inflate(context, R.layout.captioned_textview_vert, this)
            } else {
                View.inflate(context, R.layout.captioned_textview_horz, this)
            }
            captionText = view?.findViewById(R.id.textCaption)
            detailText = view?.findViewById(R.id.textDetail)
            ratingBar = view?.findViewById(R.id.ratingBar)
        }
    }
}
