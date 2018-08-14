package com.woocommerce.android.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator

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

    fun fadeIn(target: View, animDuration: Duration = DEFAULT_DURATION) {
        with (ObjectAnimator.ofFloat(target, View.ALPHA, 0.0f, 1.0f)) {
            duration = animDuration.toMillis(target.context)
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    target.visibility = View.VISIBLE
                }
            })
            start()
        }
    }

    fun fadeOut(target: View, animDuration: Duration = DEFAULT_DURATION) {
        with (ObjectAnimator.ofFloat(target, View.ALPHA, 1.0f, 0.0f)) {
            duration = animDuration.toMillis(target.context)
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    target.visibility = View.GONE
                }
            })
            start()
        }
    }

    fun scaleIn(target: View, animDuration: Duration = DEFAULT_DURATION) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f)

        with (ObjectAnimator.ofPropertyValuesHolder(target, scaleX, scaleY)) {
            duration = animDuration.toMillis(target.context)
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    target.visibility = View.VISIBLE
                }
            })
            start()
        }
    }

    fun scaleOut(target: View, animDuration: Duration = DEFAULT_DURATION) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f)

        with (ObjectAnimator.ofPropertyValuesHolder(target, scaleX, scaleY)) {
            duration = animDuration.toMillis(target.context)
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    target.visibility = View.GONE
                }
            })
            start()
        }
    }
}
