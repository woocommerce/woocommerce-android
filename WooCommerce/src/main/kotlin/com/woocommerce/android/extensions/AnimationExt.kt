package com.woocommerce.android.extensions

import android.animation.ObjectAnimator
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup.LayoutParams

fun View.showSlideDown() {
    visibility = View.VISIBLE
    val newLayoutParams: LayoutParams = layoutParams
    newLayoutParams.height = 1
    layoutParams = newLayoutParams
    measure(
        View.MeasureSpec.makeMeasureSpec(
            Resources.getSystem().displayMetrics.widthPixels,
            View.MeasureSpec.EXACTLY
        ),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    val height: Int = measuredHeight
    val valueAnimator = ObjectAnimator.ofInt(1, height)
    valueAnimator.addUpdateListener { animation ->
        val value = animation.animatedValue as Int
        if (height > value) {
            val animatedViewLayoutParams = layoutParams
            animatedViewLayoutParams.height = value
            this.layoutParams = animatedViewLayoutParams
        } else {
            val animatedViewLayoutParams = layoutParams
            animatedViewLayoutParams.height = LayoutParams.WRAP_CONTENT
            layoutParams = animatedViewLayoutParams
        }
    }
    valueAnimator.start()
}

fun View.hideSlideUp() {
    post {
        val height = height
        val valueAnimator = ObjectAnimator.ofInt(height, 0)
        valueAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            if (value > 0) {
                val newLayoutParams = layoutParams
                newLayoutParams.height = value
                layoutParams = newLayoutParams
            } else {
                visibility = View.GONE
            }
        }
        valueAnimator.start()
    }
}
