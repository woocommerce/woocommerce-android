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
import java.lang.ref.WeakReference

class MainBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BottomNavigationView(context, attrs),
    OnItemSelectedListener,
    OnItemReselectedListener {
    private var navController: NavController? = null
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
        this.navController = navController
        this.listener = listener

        addTopDivider()
        createBadges()

        assignNavigationListeners(true)
        val weakReference = WeakReference(this)
        this.navController?.addOnDestinationChangedListener(
            object : NavController.OnDestinationChangedListener {
                override fun onDestinationChanged(
                    controller: NavController,
                    destination: NavDestination,
                    arguments: Bundle?
                ) {
                    val view = weakReference.get()
                    if (view == null) {
                        this@MainBottomNavigationView.navController?.removeOnDestinationChangedListener(this)
                        return
                    }
                    view.menu.forEach { item ->
                        if (destination.matchDestination(item.itemId)) {
                            item.isChecked = true
                        }
                    }
                }
            }
        )
    }

    private fun createBadges() {
        ordersBadge = getOrCreateBadge(R.id.orders)
        ordersBadge.isVisible = false
        ordersBadge.backgroundColor = ContextCompat.getColor(context, R.color.color_primary)
        ordersBadge.maxCharacterCount = 3 // this includes the plus sign

        moreMenuBadge = getOrCreateBadge(R.id.moreMenu)
        moreMenuBadge.isVisible = false
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

    fun showMoreMenuUnseenReviewsBadge(count: Int) {
        moreMenuBadge.backgroundColor = ContextCompat.getColor(context, R.color.color_primary)
        moreMenuBadge.number = count
        moreMenuBadge.isVisible = true
    }

    fun showMoreMenuNewFeatureBadge() {
        moreMenuBadge.backgroundColor = ContextCompat.getColor(context, R.color.color_secondary)
        moreMenuBadge.isVisible = true
    }

    fun hideMoreMenuBadge() {
        moreMenuBadge.isVisible = false
    }

    fun setOrderBadgeCount(count: Int) {
        if (count > 0) {
            ordersBadge.number = count
            ordersBadge.isVisible = true
        } else {
            ordersBadge.isVisible = false
        }
    }

    fun clearOrderBadgeCount() {
        ordersBadge.clearNumber()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        navController?.let { navController ->
            val navSuccess = NavigationUI.onNavDestinationSelected(item, navController)
            if (navSuccess) {
                listener.onNavItemSelected(findNavigationPositionById(item.itemId))
                return true
            }
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

    private fun NavDestination.matchDestination(@IdRes destId: Int): Boolean =
        hierarchy.any { it.id == destId }
}
