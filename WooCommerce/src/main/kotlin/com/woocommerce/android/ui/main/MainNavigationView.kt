package com.woocommerce.android.ui.main

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemReselectedListener
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener
import com.woocommerce.android.R
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration

class MainNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr),
        OnNavigationItemSelectedListener, OnNavigationItemReselectedListener {
    private lateinit var navController: NavController
    private lateinit var listener: MainNavigationListener
    private lateinit var badgeView: View

    interface MainNavigationListener {
        fun onNavItemSelected(navPos: BottomNavigationPosition)
        fun onNavItemReselected(navPos: BottomNavigationPosition)
    }

    fun init(navController: NavController, listener: MainNavigationListener) {
        this.navController = navController
        this.listener = listener

        // set up the bottom bar
        val menuView = getChildAt(0) as BottomNavigationMenuView
        val inflater = LayoutInflater.from(context)
        val itemView = menuView.getChildAt(BottomNavigationPosition.NOTIFICATIONS.position) as BottomNavigationItemView
        badgeView = inflater.inflate(R.layout.notification_badge_view, menuView, false)
        itemView.addView(badgeView)

        this.setupWithNavController(navController)

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
        listener.onNavItemSelected(navPos)
        return true
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        val navPos = findNavigationPositionById(item.itemId)
        listener.onNavItemReselected(navPos)
    }

    private fun assignNavigationListeners(assign: Boolean) {
        setOnNavigationItemSelectedListener(if (assign) this else null)
        setOnNavigationItemReselectedListener(if (assign) this else null)
    }
}
