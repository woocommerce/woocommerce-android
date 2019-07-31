package com.woocommerce.android.ui.main

import android.content.Context
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemReselectedListener
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener
import com.woocommerce.android.R
import com.woocommerce.android.extensions.active
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.main.BottomNavigationPosition.DASHBOARD
import com.woocommerce.android.ui.main.BottomNavigationPosition.REVIEWS
import com.woocommerce.android.ui.main.BottomNavigationPosition.ORDERS
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration

class MainBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr),
        OnNavigationItemSelectedListener, OnNavigationItemReselectedListener {
    private lateinit var navAdapter: NavAdapter
    private lateinit var fragmentManager: FragmentManager
    private lateinit var listener: MainNavigationListener
    private lateinit var notifsBadgeView: View
    private lateinit var ordersBadgeView: View
    private lateinit var ordersBadgeTextView: TextView

    companion object {
        private var previousNavPos: BottomNavigationPosition? = null
        private const val ORDER_BADGE_MAX = 99
        private const val ORDER_BADGE_MAX_LABEL = "$ORDER_BADGE_MAX+"
    }

    interface MainNavigationListener {
        fun onNavItemSelected(navPos: BottomNavigationPosition)
        fun onNavItemReselected(navPos: BottomNavigationPosition)
    }

    var currentPosition: BottomNavigationPosition
        get() = findNavigationPositionById(selectedItemId)
        set(navPos) = updateCurrentPosition(navPos)

    fun init(fm: FragmentManager, listener: MainNavigationListener) {
        this.fragmentManager = fm
        this.listener = listener

        navAdapter = NavAdapter()

        // set up the bottom bar and add the badge views
        val menuView = getChildAt(0) as BottomNavigationMenuView
        val inflater = LayoutInflater.from(context)

        val ordersItemView = menuView.getChildAt(ORDERS.position) as BottomNavigationItemView
        ordersBadgeView = inflater.inflate(R.layout.order_badge_view, menuView, false)
        ordersBadgeTextView = ordersBadgeView.findViewById<TextView>(R.id.textOrderCount)
        ordersItemView.addView(ordersBadgeView)

        val notifsItemView = menuView.getChildAt(REVIEWS.position) as BottomNavigationItemView
        notifsBadgeView = inflater.inflate(R.layout.notification_badge_view, menuView, false)
        notifsItemView.addView(notifsBadgeView)

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
        with(notifsBadgeView) {
            if (show && visibility != View.VISIBLE) {
                WooAnimUtils.fadeIn(this, Duration.MEDIUM)
            } else if (!show && visibility == View.VISIBLE) {
                WooAnimUtils.fadeOut(this, Duration.MEDIUM)
            }
        }
    }

    fun showOrderBadge(count: Int) {
        if (count <= 0) {
            hideOrderBadge()
            return
        }

        val label = if (count > ORDER_BADGE_MAX) ORDER_BADGE_MAX_LABEL else count.toString()
        ordersBadgeTextView.text = label
        if (ordersBadgeView.visibility != View.VISIBLE) {
            WooAnimUtils.fadeIn(ordersBadgeView, Duration.MEDIUM)
        }
    }

    /**
     * If the order badge is showing, hide the TextView which shows the order count
     */
    fun hideOrderBadgeCount() {
        if (ordersBadgeView.visibility == View.VISIBLE) {
            ordersBadgeTextView.text = null
        }
    }

    fun hideOrderBadge() {
        if (ordersBadgeView.visibility == View.VISIBLE) {
            WooAnimUtils.fadeOut(ordersBadgeView, Duration.MEDIUM)
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

        // hide previous fragment if it exists
        val fragmentTransaction = fragmentManager.beginTransaction()
        previousNavPos?.let {
            val previousFragment = navAdapter.getFragment(it)
            fragmentTransaction.hide(previousFragment)
        }

        // add the fragment if it hasn't been added yet
        val tag = navPos.getTag()
        if (fragmentManager.findFragmentByTag(tag) == null) {
            fragmentTransaction.add(R.id.container, fragment, tag)
        }

        // show the new fragment
        fragmentTransaction.show(fragment)
        fragmentTransaction.commitAllowingStateLoss()

        previousNavPos = navPos
    }

    private fun assignNavigationListeners(assign: Boolean) {
        setOnNavigationItemSelectedListener(if (assign) this else null)
        setOnNavigationItemReselectedListener(if (assign) this else null)
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
