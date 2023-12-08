package com.woocommerce.android.ui.main

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class MainAnimatorHelper @Inject constructor(private val resourceProvider: ResourceProvider) {
    var toolbarHeight = 0

    private val collapsingToolbarMarginBottomAnimator by lazy {
        ValueAnimator.ofInt(
            resourceProvider.getDimensionPixelSize(R.dimen.expanded_toolbar_bottom_margin),
            resourceProvider.getDimensionPixelSize(R.dimen.expanded_toolbar_bottom_margin_with_subtitle)
        ).apply {
            duration = COLLAPSING_ANIMATION_DURATION
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    private val toolbarAnimator by lazy {
        ValueAnimator.ofInt(
            0,
            toolbarHeight
        ).apply {
            duration = TOOLBAR_ANIMATION_DURATION
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    fun animateToolbarHeight(show: Boolean, onUpdate: (Int) -> Unit) {
        toolbarAnimator.removeAllUpdateListeners()
        if (show) {
            toolbarAnimator.apply {
                addUpdateListener { onUpdate(it.animatedValue as Int) }
            }.start()
        } else {
            toolbarAnimator.apply {
                addUpdateListener { onUpdate(it.animatedValue as Int) }
            }.reverse()
        }
    }

    fun animateCollapsingToolbarMarginBottom(show: Boolean, onUpdate: (Int) -> Unit) {
        collapsingToolbarMarginBottomAnimator.removeAllUpdateListeners()
        if (show) {
            collapsingToolbarMarginBottomAnimator.apply {
                addUpdateListener { onUpdate(it.animatedValue as Int) }
            }.start()
        } else {
            collapsingToolbarMarginBottomAnimator.apply {
                addUpdateListener { onUpdate(it.animatedValue as Int) }
            }.reverse()
        }
    }

    fun animateTitleChange(collapsingToolbar: CollapsingToolbarLayout, newTitle: CharSequence) {
        val currentTitle = StringBuilder(collapsingToolbar.title ?: "")
        val ticks = currentTitle.length + newTitle.length
        ValueAnimator.ofInt(0, ticks).apply {
            duration = COLLAPSING_ANIMATION_DURATION
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val tick = it.animatedValue as Int
                collapsingToolbar.title = if (tick < currentTitle.length) {
                    currentTitle.substring(0, currentTitle.length - tick)
                } else {
                    newTitle.substring(0, tick - currentTitle.length)
                }
            }
        }.start()
    }

    private companion object {
        private const val COLLAPSING_ANIMATION_DURATION = 200L
        private const val TOOLBAR_ANIMATION_DURATION = 300L
    }
}
