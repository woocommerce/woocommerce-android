package com.woocommerce.android.util

import android.content.Context
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.woocommerce.android.R
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

        placeFab(screen.fab!!)
    }

    fun onItemClicked(
        tabletNavigateTo: () -> Pair<Int, Bundle>,
        navigateWithPhoneNavigation: () -> Unit
    ) {
        if (isTabletLogicNeeded()) {
            val navigationData = tabletNavigateTo()
            navHostFragment.navController.navigate(
                navigationData.first,
                navigationData.second,
            )
        } else {
            navigateWithPhoneNavigation()
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        if (!isTabletLogicNeeded()) return

        initNavFragment(screen!!.secondPaneNavigation)
        adjustUIForScreenSize(screen!!.twoPaneLayoutGuideline)
    }

    private fun placeFab(fab: Screen.Fab) {
        val params = fab.view.layoutParams as ConstraintLayout.LayoutParams

        if (isTabletLogicNeeded()) {
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.bottomToTop = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
        } else {
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            params.topToTop = ConstraintLayout.LayoutParams.UNSET

            fab.pinFabAboveBottomNavigationBar()
        }

        fab.view.setOnClickListener { fab.onClick() }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        screen!!.lifecycleKeeper.removeObserver(this)
        screen = null
    }

    private fun initNavFragment(navigation: Screen.Navigation) {
        val fragmentManager = navigation.fragmentManager
        val navGraphId = navigation.navGraphId
        val bundle = navigation.initialBundle

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
        val fab: Fab?

        data class Navigation(
            val fragmentManager: FragmentManager,
            val navGraphId: Int,
            val initialBundle: Bundle?
        )

        data class Fab(
            val view: FloatingActionButton,
            val pinFabAboveBottomNavigationBar: () -> Unit,
            val onClick: () -> Unit,
        )
    }
}

class IsTabletLogicNeeded @Inject constructor(private val isTablet: IsTablet) {
    operator fun invoke() = isTablet() && FeatureFlag.BETTER_TABLETS_SUPPORT_PRODUCTS.isEnabled()
}
