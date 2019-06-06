package com.woocommerce.android.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.woocommerce.android.R

/**
 * FlowLayout: taken from the WordPress Android source and modified for Woo.
 *
 * Displays a horizontal list of items that automatically wraps when room on the current
 * line runs out to ensure all items are visible.
 *
 * @attr ref com.woocommerce.android.R.styleable#FlowLayout_horizontalSpacing
 * @attr ref com.woocommerce.android.R.styleable#FlowLayout_verticalSpacing
 */
open class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {
    private var mHorizontalSpacing: Int = 0
    private var mVerticalSpacing: Int = 0

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout, R.attr.flowLayoutStyle, 0)
            try {
                mHorizontalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_horizontalSpacing, 0)
                mVerticalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_verticalSpacing, 0)
            } finally {
                a.recycle()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec) - ViewCompat.getPaddingEnd(this) -
                ViewCompat.getPaddingStart(this)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val growHeight = widthMode != MeasureSpec.UNSPECIFIED
        var width = 0
        var height = paddingTop
        var currentWidth = ViewCompat.getPaddingStart(this)
        var currentHeight = 0
        var newLine = false
        var spacing = 0
        val count = childCount

        for (i in 0 until count) {
            val child = getChildAt(i)
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            val lp = child.layoutParams as LayoutParams
            spacing = mHorizontalSpacing

            if (lp.horizontalSpacing >= 0) {
                spacing = lp.horizontalSpacing
            }

            if (growHeight && currentWidth + child.measuredWidth > widthSize) {
                height += currentHeight + mVerticalSpacing
                currentHeight = 0
                width = Math.max(width, currentWidth - spacing)
                currentWidth = ViewCompat.getPaddingStart(this)
                newLine = true
            } else {
                newLine = false
            }

            lp.x = currentWidth
            lp.y = height
            currentWidth += child.measuredWidth + spacing
            currentHeight = Math.max(currentHeight, child.measuredHeight)
        }

        if (!newLine) {
            width = Math.max(width, currentWidth - spacing)
        }
        width += ViewCompat.getPaddingEnd(this)
        height += currentHeight + paddingBottom
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutParams
            child.layout(lp.x, lp.y, lp.x + child.measuredWidth, lp.y + child.measuredHeight)
        }
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
        return LayoutParams(p.width, p.height)
    }
    class LayoutParams : ViewGroup.MarginLayoutParams {
        internal var x: Int = 0
        internal var y: Int = 0
        var horizontalSpacing: Int = -1

        @SuppressLint("CustomViewStyleable")
        constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout_LayoutParams)
            horizontalSpacing = try {
                a.getDimensionPixelSize(
                        R.styleable.FlowLayout_LayoutParams_layout_horizontalSpacing, -1)
            } finally {
                a.recycle()
            }
        }
        constructor(w: Int, h: Int) : super(w, h)
    }
}
