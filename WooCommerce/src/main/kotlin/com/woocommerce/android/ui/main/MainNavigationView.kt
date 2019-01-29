package com.woocommerce.android.ui.main

import android.content.Context
import android.support.design.internal.BottomNavigationItemView
import android.support.design.internal.BottomNavigationMenuView
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.BottomNavigationView.OnNavigationItemReselectedListener
import android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.extensions.active
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.main.BottomNavigationPosition.DASHBOARD
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration

class MainNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr),
        OnNavigationItemSelectedListener, OnNavigationItemReselectedListener {
    private lateinit var navAdapter: NavAdapter
    private lateinit var fragmentManager: FragmentManager
    private lateinit var listener: MainNavigationListener
    private lateinit var badgeView: View

    interface MainNavigationListener {
        fun onNavItemSelected(navPos: BottomNavigationPosition)

        fun onNavItemReselected(navPos: BottomNavigationPosition)
    }

    val activeFragment: TopLevelFragment
        get() = navAdapter.getFragment(currentPosition)

    var currentPosition: BottomNavigationPosition
        get() = findNavigationPositionById(selectedItemId)
        set(navPos) = updateCurrentPosition(navPos)

    fun init(fm: FragmentManager, listener: MainNavigationListener) {
        this.fragmentManager = fm
        this.listener = listener

        navAdapter = NavAdapter()

        // set up the bottom bar
        val menuView = getChildAt(0) as BottomNavigationMenuView
        val inflater = LayoutInflater.from(context)
        val itemView = menuView.getChildAt(BottomNavigationPosition.NOTIFICATIONS.position) as BottomNavigationItemView
        badgeView = inflater.inflate(R.layout.notification_badge_view, menuView, false)
        itemView.addView(badgeView)

        assignNavigationListeners(true)

        // Default to the dashboard position
        active(DASHBOARD.position)
    }

    fun getFragment(navPos: BottomNavigationPosition): TopLevelFragment = navAdapter.getFragment(navPos)

    fun updatePositionAndDeferInit(navPos: BottomNavigationPosition) {
        updateCurrentPosition(navPos, true)
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

    fun showNotificationBadge(show: Boolean) {
        with(badgeView) {
            if (show && visibility != View.VISIBLE) {
                WooAnimUtils.fadeIn(this, Duration.MEDIUM)
            } else if (!show && visibility == View.VISIBLE) {
                WooAnimUtils.fadeOut(this, Duration.MEDIUM)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navPos = findNavigationPositionById(item.itemId)
        currentPosition = navPos

        listener.onNavItemSelected(navPos)
        return true
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        val navPos = findNavigationPositionById(item.itemId)
        val activeFragment = fragmentManager.findFragmentByTag(navPos.getTag())
        if (!clearFragmentBackStack(activeFragment)) {
            (activeFragment as? TopLevelFragment)?.scrollToTop()
        }

        listener.onNavItemReselected(navPos)
    }

    private fun updateCurrentPosition(navPos: BottomNavigationPosition, deferInit: Boolean = false) {
        assignNavigationListeners(false)
        try {
            selectedItemId = navPos.id
        } finally {
            assignNavigationListeners(true)
        }

        val fragment = navAdapter.getFragment(navPos)
        fragment.deferInit = deferInit

        // Close any child fragments if open
        clearFragmentBackStack(fragment)

        fragmentManager.beginTransaction().replace(R.id.container, fragment, navPos.getTag()).commitNow()
    }

    private fun assignNavigationListeners(assign: Boolean) {
        setOnNavigationItemSelectedListener(if (assign) this else null)
        setOnNavigationItemReselectedListener(if (assign) this else null)
    }

    /**
     * Pop all child fragments to return to the top-level view.
     * returns true if child fragments existed.
     */
    private fun clearFragmentBackStack(fragment: Fragment?): Boolean {
        fragment?.let {
            if (!it.isAdded) {
                return false
            }
            with(it.childFragmentManager) {
                if (backStackEntryCount > 0) {
                    val firstEntry = getBackStackEntryAt(0)
                    popBackStack(firstEntry.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    return true
                }
            }
        }
        return false
    }

    /**
     * Extension function for retrieving an existing fragment from the [FragmentManager]
     * if one exists, if not, create a new instance of the requested fragment.
     */
    private fun FragmentManager.findFragment(position: BottomNavigationPosition): TopLevelFragment {
        return (findFragmentByTag(position.getTag()) ?: position.createFragment()) as TopLevelFragment
    }

    // region Private Classes
    private inner class NavAdapter {
        private val fragments = SparseArray<TopLevelFragment>(BottomNavigationPosition.values().size)

        internal fun getFragment(navPos: BottomNavigationPosition): TopLevelFragment {
            fragments[navPos.position]?.let {
                return it
            }

            val fragment = fragmentManager.findFragment(navPos)
            fragments.put(navPos.position, fragment)
            return fragment
        }
    }
    // endregion
}
