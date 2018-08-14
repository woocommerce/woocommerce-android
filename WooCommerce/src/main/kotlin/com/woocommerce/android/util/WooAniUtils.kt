package com.woocommerce.android.util

import android.animation.Animator
import android.content.Context
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

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
        with (target) {
            scaleX = 0f
            scaleY = 0f
            visibility = View.VISIBLE
            animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setDuration(duration.toMillis(target.context))
        }
    }

    fun scaleOut(target: View, duration: Duration = DEFAULT_DURATION) {
        with(target) {
            scaleX = 1f
            scaleY = 1f
            animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setDuration(duration.toMillis(target.context))
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator?) {
                            // noop
                        }
                        override fun onAnimationRepeat(animation: Animator?) {
                            // noop
                        }
                        override fun onAnimationEnd(animation: Animator?) {
                            target.visibility = View.GONE
                            target.scaleX = 1f
                            target.scaleY = 1f
                        }
                        override fun onAnimationCancel(animation: Animator?) {
                            // noop
                        }
                    })
        }
    }
}
