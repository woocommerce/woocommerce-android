package com.woocommerce.android.ui.main

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemReselectedListener
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener
import com.woocommerce.android.R
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration

class MainBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr),
        OnNavigationItemSelectedListener, OnNavigationItemReselectedListener {
    private lateinit var listener: MainBottomNavigationListener
    private lateinit var badgeView: View

    interface MainBottomNavigationListener {
        fun onBottomNavItemSelected(navPos: BottomNavigationPosition)
        fun onBottomNavItemReselected(navPos: BottomNavigationPosition)
    }

    fun init(listener: MainBottomNavigationListener) {
        this.listener = listener

        // set up the bottom bar
        val menuView = getChildAt(0) as BottomNavigationMenuView
        val inflater = LayoutInflater.from(context)
        val itemView = menuView.getChildAt(BottomNavigationPosition.NOTIFICATIONS.position) as BottomNavigationItemView
        badgeView = inflater.inflate(R.layout.notification_badge_view, menuView, false)
        itemView.addView(badgeView)

        setOnNavigationItemSelectedListener(this)
        setOnNavigationItemReselectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navPos = findNavigationPositionById(item.itemId)
        listener.onBottomNavItemSelected(navPos)
        return true
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        val navPos = findNavigationPositionById(item.itemId)
        listener.onBottomNavItemReselected(navPos)
    }

    /**
     * If the passed id isn't the current one, temporarily disable the listener and make it current
     */
    fun ensureSelectedItemId(id: Int) {
        if (selectedItemId != id) {
            setOnNavigationItemSelectedListener(null)
            selectedItemId = id
            setOnNavigationItemSelectedListener(this)
        }
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
}
