package com.woocommerce.android.ui.main

import androidx.annotation.IdRes
import com.woocommerce.android.R

// Note that the id must match the fragment id in the main navigation graph
enum class BottomNavigationPosition(val position: Int, @IdRes val id: Int) {
    DASHBOARD(0, R.id.dashboardFragment),
    ORDERS(1, R.id.orderListFragment),
    NOTIFICATIONS(2, R.id.notifsListFragment)
}

fun findNavigationPositionById(id: Int): BottomNavigationPosition = when (id) {
    BottomNavigationPosition.DASHBOARD.id -> BottomNavigationPosition.DASHBOARD
    BottomNavigationPosition.ORDERS.id -> BottomNavigationPosition.ORDERS
    BottomNavigationPosition.NOTIFICATIONS.id -> BottomNavigationPosition.NOTIFICATIONS
    else -> BottomNavigationPosition.DASHBOARD
}
