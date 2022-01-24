package com.woocommerce.android.extensions

import android.graphics.Rect
import android.view.TouchDelegate
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout.LayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.transition.ChangeBounds
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import androidx.transition.TransitionManager

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.expand(duration: Long = 300L) {
    if (!this.isVisible) {
        this.visibility = View.VISIBLE
        this.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = this.measuredHeight
        val view = this

        this.layoutParams.height = 1
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                view.layoutParams.height = if (interpolatedTime == 1f) {
                    LayoutParams.WRAP_CONTENT
                } else {
                    (targetHeight * interpolatedTime).toInt()
                }
                view.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        a.duration = duration
        this.startAnimation(a)
    }
}

fun View.collapse(duration: Long = 300L) {
    if (this.isVisible) {
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

        a.duration = duration
        this.startAnimation(a)
    }
}

fun Group.expand() {
    if (this.isVisible) return
    val animationDuration = 300L
    val parent = parent as ViewGroup
    val transition = ChangeBounds()
        .setDuration(animationDuration)
        .addTarget(parent)
    TransitionManager.beginDelayedTransition(parent, transition)
    isVisible = true
}

fun Group.collapse() {
    if (!this.isVisible) return
    val animationDuration = 300L
    val parent = parent as ConstraintLayout
    val views = referencedIds.map { parent.getViewById(it) }
    val originalHeights = views.map { it.layoutParams.height }
    val transition = ChangeBounds()
        .setDuration(animationDuration)

    transition.addListener(object : TransitionListenerAdapter() {
        override fun onTransitionEnd(transition: Transition) {
            super.onTransitionEnd(transition)
            isVisible = false
            views.forEachIndexed { index, view ->
                view.updateLayoutParams { height = originalHeights[index] }
            }
        }
    })

    TransitionManager.beginDelayedTransition(parent, transition)
    views.forEach {
        it.updateLayoutParams { height = 0 }
        visibility = View.INVISIBLE
    }
}

fun View.expandHitArea(horizontal: Int, vertical: Int) {
    val parent = this.parent as View
    parent.post {
        val rect = Rect()
        getHitRect(rect)

        rect.left -= horizontal
        rect.top -= vertical
        rect.right += horizontal
        rect.bottom += vertical

        parent.touchDelegate = TouchDelegate(rect, this)
    }
}

fun ViewGroup.setEnabledRecursive(enabled: Boolean) {
    isEnabled = enabled
    children.forEach {
        it.isEnabled = enabled
        if (it is ViewGroup) it.setEnabledRecursive(enabled)
    }
}
