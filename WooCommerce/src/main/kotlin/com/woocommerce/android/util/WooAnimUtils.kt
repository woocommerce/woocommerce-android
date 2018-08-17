package com.woocommerce.android.util

import android.content.Context
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import com.woocommerce.android.util.WooAnimUtils.Duration.SHORT

object WooAnimUtils {
    enum class Duration {
        SHORT,
        MEDIUM,
        LONG;

        fun toMillis(context: Context): Long {
            return when (this) {
                SHORT -> context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
                MEDIUM -> context.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                LONG -> context.resources.getInteger(android.R.integer.config_longAnimTime).toLong()
            }
        }
    }

    fun animateBottomBar(view: View, show: Boolean, duration: Duration = SHORT) {
        animateBar(view, show, false, duration)
    }

    private fun animateBar(view: View?, show: Boolean, isTopBar: Boolean, duration: Duration) {
        val newVisibility = if (show) View.VISIBLE else View.GONE

        if (view == null || view.visibility == newVisibility) {
            return
        }

        val fromY: Float
        val toY: Float
        if (isTopBar) {
            fromY = if (show) -1f else 0f
            toY = if (show) 0f else -1f
        } else {
            fromY = if (show) 1f else 0f
            toY = if (show) 0f else 1f
        }
        val animation = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, fromY,
                Animation.RELATIVE_TO_SELF, toY)

        val durationMillis = duration.toMillis(view.context)
        animation.duration = durationMillis

        if (show) {
            animation.interpolator = DecelerateInterpolator()
        } else {
            animation.interpolator = AccelerateInterpolator()
        }

        view.clearAnimation()
        view.startAnimation(animation)
        view.visibility = newVisibility
    }
}
