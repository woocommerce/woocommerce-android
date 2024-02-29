package com.woocommerce.android.util

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.constraintlayout.widget.Guideline
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isDisplaySmallerThan720
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

class TabletLayoutSetupHelper @Inject constructor(
    private val context: Context,
    private val isTabletLogicNeeded: IsTabletLogicNeeded,
) : DefaultLifecycleObserver {
    private var screen: Screen? = null

    private lateinit var navHostFragment: NavHostFragment

    fun onViewCreated(screen: Screen) {
        this.screen = screen
        screen.lifecycleKeeper.addObserver(this)

        if (FeatureFlag.BETTER_TABLETS_SUPPORT_PRODUCTS.isEnabled()) {
            initNavFragment(screen.secondPaneNavigation)
            adjustUIForScreenSize(screen)
        }
    }

    fun openItemDetails(
        tabletNavigateTo: () -> Pair<Int, Bundle>,
        navigateWithPhoneNavigation: () -> Unit
    ) {
        if (isTabletLogicNeeded()) {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(navHostFragment.navController.graph.startDestinationId, true)
                .build()
            val navigationData = tabletNavigateTo()
            navHostFragment.navController.navigate(
                resId = navigationData.first,
                args = navigationData.second,
                navOptions = navOptions
            )
        } else {
            navigateWithPhoneNavigation()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        screen!!.lifecycleKeeper.removeObserver(this)
        screen = null
    }

    private fun initNavFragment(navigation: Screen.Navigation) {
        val fragmentManager = navigation.fragmentManager
        val navGraphId = navigation.navGraphId
        val bundle = navigation.initialBundle

        val existingFragment = fragmentManager.findFragmentById(R.id.detail_nav_container)

        if (existingFragment == null) {
            navHostFragment = NavHostFragment.create(navGraphId, bundle)
            fragmentManager.beginTransaction()
                .replace(R.id.detail_nav_container, navHostFragment)
                .commit()
        } else {
            navHostFragment = existingFragment as NavHostFragment
        }
    }

    private fun adjustUIForScreenSize(screen: Screen) {
        if (isTabletLogicNeeded()) {
            adjustLayoutForTablet(screen)
        } else {
            adjustLayoutForNonTablet(screen)
        }
    }

    private fun adjustLayoutForTablet(screen: Screen) {
        val isSmallTablet = context.isDisplaySmallerThan720
        val isPortrait = !DisplayUtils.isLandscape(context)

        if (isSmallTablet && isPortrait) {
            screen.twoPaneLayoutGuideline.setGuidelinePercent(TABLET_PORTRAIT_WIDTH_RATIO)
        } else {
            screen.twoPaneLayoutGuideline.setGuidelinePercent(TABLET_LANDSCAPE_WIDTH_RATIO)
        }
        screen.listPaneContainer.visibility = View.VISIBLE
        screen.detailPaneContainer.visibility = View.VISIBLE
    }

    private fun adjustLayoutForNonTablet(screen: Screen) {
        if (screen.twoPanesWereShownBeforeConfigChange) {
            displayDetailPaneOnly(screen)
        } else {
            displayListPaneOnly(screen)
        }
    }

    private fun displayDetailPaneOnly(screen: Screen) {
        screen.detailPaneContainer.visibility = View.VISIBLE
        screen.twoPaneLayoutGuideline.setGuidelinePercent(0.0f)
        screen.listPaneContainer.visibility = View.GONE
    }

    private fun displayListPaneOnly(screen: Screen) {
        screen.detailPaneContainer.visibility = View.GONE
        screen.listPaneContainer.visibility = View.VISIBLE
        screen.twoPaneLayoutGuideline.setGuidelinePercent(1f)
    }

    private companion object {
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.3f
        private const val TABLET_PORTRAIT_WIDTH_RATIO = 0.40f

        private const val MARGINS_FOR_TABLET: Float = 0.1F
        private const val MARGINS_FOR_SMALL_TABLET_PORTRAIT: Float = 0.025F
    }

    interface Screen {
        val twoPaneLayoutGuideline: Guideline
        val listPaneContainer: View
        val detailPaneContainer: View
        val secondPaneNavigation: Navigation
        val lifecycleKeeper: Lifecycle

        val twoPanesWereShownBeforeConfigChange: Boolean

        data class Navigation(
            val fragmentManager: FragmentManager,
            val navGraphId: Int,
            val initialBundle: Bundle?
        )
    }
}

class IsTabletLogicNeeded @Inject constructor(private val isTablet: IsTablet) {
    operator fun invoke() = isTablet() && FeatureFlag.BETTER_TABLETS_SUPPORT_PRODUCTS.isEnabled()
}
