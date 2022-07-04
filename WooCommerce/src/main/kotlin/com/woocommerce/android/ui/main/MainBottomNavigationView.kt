package com.woocommerce.android.ui.main

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.ui.NavigationUI
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView.OnItemReselectedListener
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener
import com.woocommerce.android.R
import com.woocommerce.android.util.FeatureFlag
import java.lang.ref.WeakReference

class MainBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BottomNavigationView(context, attrs),
    OnItemSelectedListener,
    OnItemReselectedListener {
    private lateinit var navController: NavController
    private lateinit var listener: MainNavigationListener
    private lateinit var ordersBadge: BadgeDrawable
    private lateinit var moreMenuBadge: BadgeDrawable

    interface MainNavigationListener {
        fun onNavItemSelected(navPos: BottomNavigationPosition)
        fun onNavItemReselected(navPos: BottomNavigationPosition)
    }

    var currentPosition: BottomNavigationPosition
        get() = findNavigationPositionById(selectedItemId)
        set(value) {
            selectedItemId = value.id
        }

    fun init(navController: NavController, listener: MainNavigationListener) {
        this.listener = listener
        this.navController = navController

        addTopDivider()
        createBadges()
        updateVisibilities()

        assignNavigationListeners(true)
        val weakReference = WeakReference(this)
        navController.addOnDestinationChangedListener(
            object : NavController.OnDestinationChangedListener {
                override fun onDestinationChanged(
                    controller: NavController,
                    destination: NavDestination,
                    arguments: Bundle?
                ) {
                    val view = weakReference.get()
                    if (view == null) {
                        navController.removeOnDestinationChangedListener(this)
                        return
                    }
                    view.menu.forEach { item ->
                        if (destination.matchDestination(item.itemId)) {
                            item.isChecked = true
                        }
                    }
                }
            })
    }

    private fun createBadges() {
        ordersBadge = getOrCreateBadge(R.id.orders)
        ordersBadge.isVisible = false
        ordersBadge.backgroundColor = ContextCompat.getColor(context, R.color.color_primary)
        ordersBadge.maxCharacterCount = 3 // this includes the plus sign

        moreMenuBadge = getOrCreateBadge(R.id.moreMenu)
        moreMenuBadge.isVisible = false
        moreMenuBadge.backgroundColor = ContextCompat.getColor(context, R.color.color_primary)
    }

    /**
     * When we changed the background to white, the top shadow provided by BottomNavigationView wasn't
     * dark enough to provide enough separation between the bar and the content above it. For this
     * reason we add a darker top divider here.
     */
    private fun addTopDivider() {
        val divider = View(context)
        val dividerColor = ContextCompat.getColor(context, R.color.divider_color)
        divider.setBackgroundColor(dividerColor)

        val dividerHeight = resources.getDimensionPixelSize(R.dimen.minor_10)
        val dividerParams = LayoutParams(LayoutParams.MATCH_PARENT, dividerHeight)
        divider.layoutParams = dividerParams

        addView(divider)
    }

    /**
     * For use when restoring the navigation bar after the host activity
     * state has been restored.
     */
    fun restoreSelectedItemState(itemId: Int) {
        assignNavigationListeners(false)
        selectedItemId = itemId
        assignNavigationListeners(true)
    }

    fun showMoreMenuBadge(count: Int) {
        showBadge(moreMenuBadge, count)
    }

    fun setOrderBadgeCount(count: Int) {
        showBadge(ordersBadge, count)
    }

    private fun showBadge(badgeDrawable: BadgeDrawable, count: Int) {
        if (count > 0) {
            badgeDrawable.number = count
            badgeDrawable.isVisible = true
        } else {
            badgeDrawable.isVisible = false
        }
    }

    fun clearOrderBadgeCount() {
        ordersBadge.clearNumber()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navSuccess = NavigationUI.onNavDestinationSelected(item, navController)
        if (navSuccess) {
            listener.onNavItemSelected(findNavigationPositionById(item.itemId))
            return true
        }
        return false
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        val navPos = findNavigationPositionById(item.itemId)
        listener.onNavItemReselected(navPos)
    }

    private fun assignNavigationListeners(assign: Boolean) {
        setOnItemSelectedListener(if (assign) this else null)
        setOnItemReselectedListener(if (assign) this else null)
    }

    private fun updateVisibilities() {
        if (FeatureFlag.ANALYTICS_HUB.isEnabled()) {
            menu.findItem(R.id.analytics).isVisible = true
        }
    }

    private fun NavDestination.matchDestination(@IdRes destId: Int): Boolean =
        hierarchy.any { it.id == destId }
}
