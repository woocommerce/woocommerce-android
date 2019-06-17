package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
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
class WCTextViewCompat @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : AppCompatTextView(context, attrs, defStyle) {
    init {
        attrs?.let {
            val attributeArray = context.obtainStyledAttributes(attrs, R.styleable.WCTextViewCompat)
            var drawableStart: Drawable? = null
            var drawableEnd: Drawable? = null
            var drawableBottom: Drawable? = null
            var drawableTop: Drawable? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                drawableStart = attributeArray.getDrawable(R.styleable.WCTextViewCompat_drawableStartCompat)
                drawableEnd = attributeArray.getDrawable(R.styleable.WCTextViewCompat_drawableEndCompat)
                drawableBottom = attributeArray.getDrawable(R.styleable.WCTextViewCompat_drawableBottomCompat)
                drawableTop = attributeArray.getDrawable(R.styleable.WCTextViewCompat_drawableTopCompat)
            } else {
                val drawableStartId = attributeArray
                        .getResourceId(R.styleable.WCTextViewCompat_drawableStartCompat, -1)
                val drawableEndId = attributeArray.getResourceId(R.styleable.WCTextViewCompat_drawableEndCompat, -1)
                val drawableBottomId = attributeArray
                        .getResourceId(R.styleable.WCTextViewCompat_drawableBottomCompat, -1)
                val drawableTopId = attributeArray.getResourceId(R.styleable.WCTextViewCompat_drawableTopCompat, -1)

                if (drawableStartId != -1) {
                    drawableStart = AppCompatResources.getDrawable(context, drawableStartId)
                }
                if (drawableEndId != -1) {
                    drawableEnd = AppCompatResources.getDrawable(context, drawableEndId)
                }
                if (drawableBottomId != -1) {
                    drawableBottom = AppCompatResources.getDrawable(context, drawableBottomId)
                }
                if (drawableTopId != -1) {
                    drawableTop = AppCompatResources.getDrawable(context, drawableTopId)
                }
            }

            // to support rtl
            setCompoundDrawablesRelativeWithIntrinsicBounds(drawableStart, drawableTop, drawableEnd, drawableBottom)
            attributeArray.recycle()
        }
    }
}
