package com.woocommerce.android.ui.main

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.woocommerce.android.R
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration

class MainBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {
    private lateinit var navController: NavController
    private lateinit var listener: MainBottomNavigationListener
    private lateinit var badgeView: View

    interface MainBottomNavigationListener {
        fun onBottomNavItemReselected(navPos: BottomNavigationPosition)
    }

    fun init(navController: NavController, listener: MainBottomNavigationListener) {
        this.navController = navController
        this.listener = listener

        // set up the bottom bar
        val menuView = getChildAt(0) as BottomNavigationMenuView
        val inflater = LayoutInflater.from(context)
        val itemView = menuView.getChildAt(BottomNavigationPosition.NOTIFICATIONS.position) as BottomNavigationItemView
        badgeView = inflater.inflate(R.layout.notification_badge_view, menuView, false)
        itemView.addView(badgeView)

        setupWithNavController(navController)

        setOnNavigationItemReselectedListener { item ->
            val navPos = findNavigationPositionById(item.itemId)
            listener.onBottomNavItemReselected(navPos)
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
