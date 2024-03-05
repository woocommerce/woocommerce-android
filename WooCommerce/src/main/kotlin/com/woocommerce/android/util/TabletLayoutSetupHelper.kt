package com.woocommerce.android.util

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
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
    private var navHostFragment: NavHostFragment? = null

    fun onRootFragmentCreated(screen: Screen) {
        if (FeatureFlag.BETTER_TABLETS_SUPPORT_PRODUCTS.isEnabled()) {
            this@TabletLayoutSetupHelper.screen = screen
            initNavFragment(screen)

            screen.listFragment.parentFragmentManager.registerFragmentLifecycleCallbacks(
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentViewCreated(
                        fm: FragmentManager,
                        f: Fragment,
                        v: View,
                        savedInstanceState: Bundle?
                    ) {
                        if (f == screen.listFragment) {
                            adjustUIForScreenSize(screen)
                        }
                    }

                    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                        if (f == screen.listFragment) {
                            this@TabletLayoutSetupHelper.screen = null
                            navHostFragment = null
                        }
                    }
                },
                false
            )

            screen.listFragment.childFragmentManager.registerFragmentLifecycleCallbacks(
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentViewCreated(
                        fm: FragmentManager,
                        f: Fragment,
                        v: View,
                        savedInstanceState: Bundle?
                    ) {
                        if (f != navHostFragment) {
                            setDetailsMargins(v)
                        }
                    }
                },
                true
            )
        }
    }

    fun openItemDetails(
        tabletNavigateTo: () -> Pair<Int, Bundle>,
        navigateWithPhoneNavigation: () -> Unit
    ) {
        if (isTabletLogicNeeded()) {
            val navOptions =
                NavOptions.Builder()
                    .setPopUpTo(navHostFragment!!.navController.graph.startDestinationId, true)
                    .setEnterAnim(R.anim.activity_fade_in)
                    .setExitAnim(R.anim.activity_fade_out)
                    .build()
            val navigationData = tabletNavigateTo()
            navHostFragment!!.navController.navigate(
                resId = navigationData.first,
                args = navigationData.second,
                navOptions = navOptions
            )
        } else {
            navigateWithPhoneNavigation()
        }
    }

    @Suppress("NestedBlockDepth")
    private fun setDetailsMargins(rootView: View) {
        if (isTabletLogicNeeded()) {
            if (rootView !is ViewGroup) return

            val isSmallTablet = context.isDisplaySmallerThan720
            val isPortrait = !DisplayUtils.isLandscape(context)
            val windowWidth = DisplayUtils.getWindowPixelWidth(context)
            rootView.children.filter { it !is Toolbar }.forEach { viewToApplyMargins ->
                val layoutParams = viewToApplyMargins.layoutParams
                if (layoutParams is MarginLayoutParams) {
                    if (isSmallTablet && isPortrait) {
                        val marginHorizontal = (windowWidth * MARGINS_FOR_SMALL_TABLET_PORTRAIT).toInt()
                        layoutParams.setMargins(
                            marginHorizontal, layoutParams.topMargin, marginHorizontal, layoutParams.bottomMargin
                        )
                    } else {
                        val marginHorizontal = (windowWidth * MARGINS_FOR_TABLET).toInt()
                        layoutParams.setMargins(
                            marginHorizontal, layoutParams.topMargin, marginHorizontal, layoutParams.bottomMargin
                        )
                    }

                    viewToApplyMargins.layoutParams = layoutParams
                }
            }
        }
    }

    private fun initNavFragment(screen: Screen) {
        val fragmentManager = screen.listFragment.childFragmentManager
        val navGraphId = screen.navigation.detailsNavGraphId
        val bundle = screen.navigation.detailsInitialBundle

        val existingFragment = fragmentManager.findFragmentById(R.id.detail_nav_container)

        navHostFragment = if (existingFragment == null) {
            NavHostFragment.create(navGraphId, bundle).apply {
                fragmentManager
                    .beginTransaction()
                    .replace(R.id.detail_nav_container, this)
                    .commit()
            }
        } else {
            existingFragment as NavHostFragment
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

        val navigation: Navigation

        val twoPanesWereShownBeforeConfigChange: Boolean

        val listFragment: Fragment

        data class Navigation(
            val detailsNavGraphId: Int,
            val detailsInitialBundle: Bundle?
        )
    }
}

class IsTabletLogicNeeded @Inject constructor(private val isTablet: IsTablet) {
    operator fun invoke() = isTablet() && FeatureFlag.BETTER_TABLETS_SUPPORT_PRODUCTS.isEnabled()
}
