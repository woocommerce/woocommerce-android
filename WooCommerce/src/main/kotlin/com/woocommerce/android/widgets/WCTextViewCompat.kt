package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.woocommerce.android.R

/**
 * Adds support for using vector drawables in positions like drawableStart, drawableEnd, drawableLeft and drawableRight.
 * To assign the drawables, use the custom attributes:
 * - drawableStartCompat
 * - drawableEndCompat
 * - drawableLeftCompat
 * - drawableRightCompat
 *
 * This class is based off of this [SO post](https://stackoverflow.com/questions/35761636/is-it-possible-to-use-vectordrawable-in-buttons-and-textviews-using-androiddraw/40250753#40250753)
 */
class WCTextViewCompat @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatTextView(context, attrs, defStyle) {
    init {
        attrs?.let {
            val attributeArray = context.obtainStyledAttributes(attrs, R.styleable.WCTextViewCompat)
            val drawableStart: Drawable? = attributeArray.getDrawable(R.styleable.WCTextViewCompat_drawableStartCompat)
            val drawableEnd = attributeArray.getDrawable(R.styleable.WCTextViewCompat_drawableEndCompat)
            val drawableBottom = attributeArray.getDrawable(R.styleable.WCTextViewCompat_drawableBottomCompat)
            val drawableTop = attributeArray.getDrawable(R.styleable.WCTextViewCompat_drawableTopCompat)

            // to support rtl
            setCompoundDrawablesRelativeWithIntrinsicBounds(drawableStart, drawableTop, drawableEnd, drawableBottom)
            attributeArray.recycle()
        }
    }
}
