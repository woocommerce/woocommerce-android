package com.woocommerce.android.util

import android.content.Context
import android.os.Bundle
import androidx.constraintlayout.widget.Guideline
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

class TabletHelper @Inject constructor(
    private val context: Context,
) : DefaultLifecycleObserver {
    private var screen: Screen? = null

    fun onViewCreated(screen: Screen) {
        this.screen = screen
        screen.lifecycleKeeper.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        if (DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)) {
            initNavFragment(screen!!.navigation)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        adjustUIForScreenSize(screen!!.twoPaneLayoutGuideline)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        screen!!.lifecycleKeeper.removeObserver(this)
        screen = null
    }

    private fun initNavFragment(navigation: Screen.Navigation) {
        val navController = navigation.navController!!
        val navGraphId = navigation.navGraph
        val bundle = navigation.bundle

        navController.setGraph(navGraphId, bundle)
    }

    private fun adjustUIForScreenSize(twoPaneLayoutGuideline: Guideline) {
        if (DisplayUtils.isTablet(context)) {
            twoPaneLayoutGuideline.setGuidelinePercent(TABLET_PANES_WIDTH_RATIO)
        } else if (DisplayUtils.isXLargeTablet(context)) {
            twoPaneLayoutGuideline.setGuidelinePercent(XL_TABLET_PANES_WIDTH_RATIO)
        } else {
            twoPaneLayoutGuideline.setGuidelinePercent(1.0f)
        }
    }

    private companion object {
        const val TABLET_PANES_WIDTH_RATIO = 0.5F
        const val XL_TABLET_PANES_WIDTH_RATIO = 0.68F
    }

    interface Screen {
        val twoPaneLayoutGuideline: Guideline
        val navigation: Navigation
        val lifecycleKeeper: Lifecycle

        data class Navigation(
            val navController: NavController?,
            val navGraph: Int,
            val bundle: Bundle?
        )
    }
}
