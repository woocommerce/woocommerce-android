package com.woocommerce.android.widgets

import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.woocommerce.android.widgets.AppBarStateChangeListener.State.COLLAPSED
import com.woocommerce.android.widgets.AppBarStateChangeListener.State.EXPANDED
import com.woocommerce.android.widgets.AppBarStateChangeListener.State.IDLE
import kotlin.math.abs

abstract class AppBarStateChangeListener : OnOffsetChangedListener {
    enum class State {
        EXPANDED, COLLAPSED, IDLE
    }

    private var mCurrentState = IDLE
    override fun onOffsetChanged(appBarLayout: AppBarLayout, i: Int) {
        mCurrentState = when {
            i == 0 -> onExpanded(appBarLayout)
            abs(i) >= appBarLayout.totalScrollRange -> onCollapsed(appBarLayout)
            else -> onIdle(appBarLayout)
        }
    }

    private fun onIdle(appBarLayout: AppBarLayout): State {
        if (mCurrentState != IDLE) {
            onStateChanged(appBarLayout, IDLE)
        }
        return IDLE
    }

    private fun onCollapsed(appBarLayout: AppBarLayout): State {
        if (mCurrentState != COLLAPSED) {
            onStateChanged(appBarLayout, COLLAPSED)
        }
        return COLLAPSED
    }

    private fun onExpanded(appBarLayout: AppBarLayout): State {
        if (mCurrentState != EXPANDED) {
            onStateChanged(appBarLayout, EXPANDED)
        }
        return EXPANDED
    }

    abstract fun onStateChanged(appBarLayout: AppBarLayout, state: State)
}
