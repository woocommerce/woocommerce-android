package com.woocommerce.android.util

import android.content.Context
import android.os.Bundle
import androidx.constraintlayout.widget.Guideline
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

class TabletLayoutSetupHelper @Inject constructor(
    private val context: Context,
) : DefaultLifecycleObserver {
    private var screen: Screen? = null

    private lateinit var navHostFragment: NavHostFragment

    fun onViewCreated(screen: Screen) {
        if (!FeatureFlag.BETTER_TABLETS_SUPPORT_PRODUCTS.isEnabled()) return

        this.screen = screen
        screen.lifecycleKeeper.addObserver(this)
    }

    fun onItemClicked(
        getTabletActionToNavigateTo: () -> NavDirections,
        navigateWithPhoneNavigation: () -> Unit
    ) {
        if (FeatureFlag.BETTER_TABLETS_SUPPORT_PRODUCTS.isEnabled() &&
            (DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context))
        ) {
            navHostFragment.navController.navigateSafely(getTabletActionToNavigateTo())
        } else {
            navigateWithPhoneNavigation()
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        if (!FeatureFlag.BETTER_TABLETS_SUPPORT_PRODUCTS.isEnabled()) return

        if (DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)) {
            initNavFragment(screen!!.secondPaneNavigation)
            adjustUIForScreenSize(screen!!.twoPaneLayoutGuideline)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        screen!!.lifecycleKeeper.removeObserver(this)
        screen = null
    }

    private fun initNavFragment(navigation: Screen.Navigation) {
        val fragmentManager = navigation.fragmentManager
        val navGraphId = navigation.navGraphId
        val bundle = navigation.bundle

        navHostFragment = NavHostFragment.create(navGraphId, bundle)

        fragmentManager.beginTransaction()
            .replace(R.id.detail_nav_container, navHostFragment)
            .commit()
    }

    private fun adjustUIForScreenSize(twoPaneLayoutGuideline: Guideline) {
        when {
            DisplayUtils.isTablet(context) -> {
                twoPaneLayoutGuideline.setGuidelinePercent(TABLET_PANES_WIDTH_RATIO)
            }

            DisplayUtils.isXLargeTablet(context) -> {
                twoPaneLayoutGuideline.setGuidelinePercent(XL_TABLET_PANES_WIDTH_RATIO)
            }

            else -> twoPaneLayoutGuideline.setGuidelinePercent(1.0f)
        }
    }

    private companion object {
        const val TABLET_PANES_WIDTH_RATIO = 0.5F
        const val XL_TABLET_PANES_WIDTH_RATIO = 0.33F
    }

    interface Screen {
        val twoPaneLayoutGuideline: Guideline
        val secondPaneNavigation: Navigation
        val lifecycleKeeper: Lifecycle

        data class Navigation(
            val fragmentManager: FragmentManager,
            val navGraphId: Int,
            val bundle: Bundle?
        )
    }
}
