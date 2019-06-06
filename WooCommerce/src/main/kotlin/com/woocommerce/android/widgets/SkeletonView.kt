package com.woocommerce.android.widgets

import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.facebook.shimmer.ShimmerFrameLayout
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration

class SkeletonView {
    private lateinit var parentView: ViewGroup
    private lateinit var actualView: ViewGroup
    private lateinit var skeletonView: View
    private lateinit var shimmerView: ShimmerFrameLayout

    private val handler = Handler()

    private var isShowing = false

    /**
     * Replaces the passed ViewGroup with a skeleton view inflated from the passed layout id
     * and starts a shimmer animation on the skeleton view
     *
     * @param viewActual The view containing the data which will be hidden during loading
     * @param viewSkeleton The skeleton view which will replace the actual view during loading
     * @param delayed Whether to show the skeleton after a brief delay, which avoids the skeleton appearing
     * and then immediately disappearing if the network request completes very quickly
     */
    fun show(viewActual: ViewGroup, viewSkeleton: View, delayed: Boolean = false) {
        if (isShowing) { return }

        val viewParent = viewActual.parent ?: throw IllegalStateException("Source view isn't attached")

        parentView = viewParent as ViewGroup
        actualView = viewActual

        // create the shimmer view
        shimmerView = ShimmerFrameLayout(parentView.context)
        shimmerView.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        // add our skeleton layout to the shimmer view
        skeletonView = viewSkeleton
        shimmerView.addView(skeletonView)

        // hide the actual data view
        actualView.visibility = View.GONE

        isShowing = true

        // add the shimmer view then start the shimmer animation - if we're delayed, add the shimmer view
        // as invisible then start it after a brief delay unless a call to hide() was made in the interim
        if (delayed) {
            shimmerView.visibility = View.INVISIBLE
            parentView.addView(shimmerView)
            handler.postDelayed({
                if (isShowing) {
                    shimmerView.visibility = View.VISIBLE
                    shimmerView.startShimmer()
                }
            }, 250)
        } else {
            parentView.addView(shimmerView)
            shimmerView.startShimmer()
        }
    }

    /**
     * Wrapper for show() which accepts a layoutId for the skeleton view
     */
    fun show(viewActual: ViewGroup, @LayoutRes layoutId: Int, delayed: Boolean = false) {
        if (isShowing) { return }

        val viewParent = viewActual.parent ?: throw IllegalStateException("Source view isn't attached")
        val viewSkeleton = LayoutInflater.from(viewActual.context).inflate(layoutId, viewParent as ViewGroup, false)
        show(viewActual, viewSkeleton, delayed)
    }

    /**
     * hides the shimmer and skeleton layout then restores the real data layout
     */
    fun hide() {
        if (!isShowing) { return }

        // stop the shimmer, remove the skeleton view, then remove the shimmer view from the parent
        shimmerView.stopShimmer()
        shimmerView.removeView(skeletonView)
        parentView.removeView(shimmerView)

        // fade in the real data view
        WooAnimUtils.fadeIn(actualView, Duration.MEDIUM)

        isShowing = false
    }

    fun findViewById(@IdRes id: Int): View? = parentView.findViewById(id)
}
