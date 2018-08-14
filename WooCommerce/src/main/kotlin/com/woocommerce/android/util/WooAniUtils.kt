package com.woocommerce.android.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator

object WooAniUtils {
    enum class Duration {
        SHORT,
        MEDIUM,
        LONG;

        fun toMillis(context: Context): Long {
            return when (this) {
                LONG -> context.resources.getInteger(android.R.integer.config_longAnimTime).toLong()
                MEDIUM -> context.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                else -> context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            }
        }
    }

    private val DEFAULT_DURATION = Duration.MEDIUM

    fun scaleIn(target: View, duration: Duration = DEFAULT_DURATION) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f)

        val animator = ObjectAnimator.ofPropertyValuesHolder(target, scaleX, scaleY)
        animator.duration = duration.toMillis(target.context)
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                target.visibility = View.VISIBLE
            }
        })

        animator.start()
    }

    fun scaleOut(target: View, duration: Duration = DEFAULT_DURATION) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f)

        val animator = ObjectAnimator.ofPropertyValuesHolder(target, scaleX, scaleY)
        animator.duration = duration.toMillis(target.context)
        animator.interpolator = AccelerateInterpolator()

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                target.visibility = View.GONE
            }
        })

        animator.start()
    }
}
