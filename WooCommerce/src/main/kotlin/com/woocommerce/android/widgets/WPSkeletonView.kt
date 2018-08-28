package com.woocommerce.android.widgets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import com.facebook.shimmer.ShimmerFrameLayout

class WPSkeletonView {
    private lateinit var parentView: ViewGroup
    private lateinit var actualView: ViewGroup
    private lateinit var skeletonView: View
    private lateinit var shimmerView: ShimmerFrameLayout

    private var skeletonLayoutId: Int = 0
    private var isShowing = false

    /**
     * Replaces the passed ViewGroup with a skeleton view inflated from the passed layout id
     * and starts a shimmer animation on the skeleton view
     *
     * @param view The view containing the "real" data which will be hidden
     * @param layoutId The resource id of the skeleton layout which will replace the above view
     */
    fun show(view: ViewGroup, @LayoutRes layoutId: Int) {
        if (isShowing) { return }

        val viewParent = view.parent ?: throw IllegalStateException("Source view isn't attached")

        parentView = viewParent as ViewGroup
        actualView = view
        skeletonLayoutId = layoutId

        // create the shimmer view
        shimmerView = ShimmerFrameLayout(parentView.context)
        shimmerView.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        // add our skeleton layout to the shimmer
        skeletonView = LayoutInflater.from(parentView.context).inflate(layoutId, parentView, false)
        shimmerView.addView(skeletonView)

        // hide the view view and add the shimmer
        actualView.visibility = View.GONE
        parentView.addView(shimmerView)

        // start the shimmer animation
        shimmerView.startShimmer()

        isShowing = true
    }

    /**
     * hides the shimmer and skeleton layout, and restore the real data layout
     */
    fun hide() {
        if (!isShowing) { return }

        // stop the shimmer, remove the skeleton view, then and remove the shimmer from the parent
        shimmerView.stopShimmer()
        shimmerView.removeView(skeletonView)
        parentView.removeView(shimmerView)

        // fade in the source view
        fadeIn(actualView)

        isShowing = false
    }

    private fun fadeIn(target: View, animDuration: Long = 300) {
        with (ObjectAnimator.ofFloat(target, View.ALPHA, 0.0f, 1.0f)) {
            duration = animDuration
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    target.visibility = View.VISIBLE
                }
            })
            start()
        }
    }
}
