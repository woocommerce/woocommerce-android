package com.woocommerce.android.util

import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout.LayoutParams
import android.view.View.MeasureSpec

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.expand() {
    this.visibility = View.VISIBLE
    this.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
    val targetHeight = this.measuredHeight
    val view = this

    this.layoutParams.height = 0
    val a = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            view.layoutParams.height = if (interpolatedTime == 1f)
                LayoutParams.WRAP_CONTENT
            else
                (targetHeight * interpolatedTime).toInt()
            view.requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    a.duration = 300
    this.startAnimation(a)
}

fun View.collapse() {
    val initialHeight = this.measuredHeight
    val view = this

    val a = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            if (interpolatedTime == 1f) {
                view.visibility = View.GONE
            } else {
                view.layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                view.requestLayout()
            }
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    a.duration = 300
    this.startAnimation(a)
}
