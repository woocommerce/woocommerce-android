package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.util.WooLog

/**
 * Used by product detail to show product property name & value, with an optional ratingBar
 */
class WCProductPropertyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private var view: View? = null
    private var propertyGroupImg: ImageView? = null
    private var propertyGroupIcon: ImageView? = null
    private var propertyNameText: TextView? = null
    private var propertyValueText: TextView? = null
    private var ratingBar: RatingBar? = null

    fun show(
        orientation: Int,
        caption: String,
        detail: CharSequence?,
        @DrawableRes propertyIcon: Int? = null
    ) {
        ensureViewCreated(orientation)

        propertyNameText?.text = caption

        if (propertyIcon != null) {
            propertyGroupIcon?.visibility = View.VISIBLE
            propertyGroupIcon?.setImageDrawable(context.getDrawable(propertyIcon))
        } else {
            propertyGroupIcon?.visibility = View.GONE
        }

        if (detail.isNullOrEmpty()) {
            propertyValueText?.visibility = View.GONE
        } else {
            propertyValueText?.text = detail
        }
    }

    fun showPropertyName(show: Boolean) {
        propertyNameText?.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * Adds a click listener to the property view
     */
    fun setClickListener(onClickListener: ((view: View) -> Unit)? = null) {
        if (onClickListener != null) {
            propertyGroupImg?.visibility = View.VISIBLE
            view?.setOnClickListener(onClickListener)
            this.isClickable = true
        } else {
            removeClickListener()
        }
    }

    fun removeClickListener() {
        this.isClickable = false
        this.background = null
        view?.setOnClickListener(null)
        propertyGroupImg?.visibility = View.GONE
    }

    fun setMaxLines(maxLines: Int) {
        propertyValueText?.maxLines = maxLines
    }

    fun setRating(rating: Float) {
        ensureViewCreated()

        try {
            ratingBar?.rating = rating
            ratingBar?.visibility = View.VISIBLE
        } catch (e: NumberFormatException) {
            WooLog.e(WooLog.T.UTILS, e)
        }
    }

    private fun ensureViewCreated(orientation: Int = LinearLayout.VERTICAL) {
        if (view == null) {
            view = if (orientation == LinearLayout.VERTICAL) {
                View.inflate(context, R.layout.product_property_view_vert_layout, this)
            } else {
                View.inflate(context, R.layout.product_property_view_horz_layout, this)
            }
            propertyGroupImg = view?.findViewById(R.id.imgProperty)
            propertyGroupIcon = view?.findViewById(R.id.imgPropertyIcon)
            propertyNameText = view?.findViewById(R.id.textPropertyName)
            propertyValueText = view?.findViewById(R.id.textPropertyValue)
            ratingBar = view?.findViewById(R.id.ratingBar)
        }
    }
}
