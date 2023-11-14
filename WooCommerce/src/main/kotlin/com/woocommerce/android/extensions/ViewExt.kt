package com.woocommerce.android.extensions

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.View.MeasureSpec
import android.view.View.OnTouchListener
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

const val EXPAND_COLLAPSE_ANIMATION_DURATION_MILLIS = 300L

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.expand(duration: Long = EXPAND_COLLAPSE_ANIMATION_DURATION_MILLIS) {
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

fun View.collapse(duration: Long = EXPAND_COLLAPSE_ANIMATION_DURATION_MILLIS) {
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
    val animationDuration = EXPAND_COLLAPSE_ANIMATION_DURATION_MILLIS
    val parent = parent as ViewGroup
    val transition = ChangeBounds()
        .setDuration(animationDuration)
        .addTarget(parent)
    TransitionManager.beginDelayedTransition(parent, transition)
    isVisible = true
}

fun Group.collapse() {
    if (!this.isVisible) return
    val animationDuration = EXPAND_COLLAPSE_ANIMATION_DURATION_MILLIS
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

/**
 * Return a Flow of events matching what an [OnTouchListener] would return.
 *
 * @param handled defines what the [OnTouchListener.onTouch] returns, defaults to returning false
 */
@SuppressLint("ClickableViewAccessibility")
fun View.touchEvents(
    handled: (MotionEvent) -> Boolean = { false }
): Flow<MotionEvent> = callbackFlow {
    val listener = OnTouchListener { _, motionEvent ->
        trySend(motionEvent)
        return@OnTouchListener handled(motionEvent)
    }

    setOnTouchListener(listener)

    awaitClose { setOnTouchListener(null) }
}

/**
 * Returns a Flow of events that are triggered when a scroll is started on a view.
 */
fun View.scrollStartEvents(): Flow<Unit> {
    return touchEvents()
        .map { it.action }
        .distinctUntilChanged()
        .filter { it == MotionEvent.ACTION_MOVE }
        .map { }
}
