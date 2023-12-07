package com.woocommerce.android.ui.main

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import javax.inject.Inject

class MainAnimatorHelper @Inject constructor() {
    fun createCollapsingToolbarMarginBottomAnimator(from: Int, to: Int, onUpdate: (Int) -> Unit): ValueAnimator {
        return ValueAnimator.ofInt(from, to)
            .also { valueAnimator ->
                valueAnimator.duration = 200L
                valueAnimator.interpolator = AccelerateDecelerateInterpolator()
                valueAnimator.addUpdateListener {
                    onUpdate(it.animatedValue as Int)
                }
            }
    }
}
