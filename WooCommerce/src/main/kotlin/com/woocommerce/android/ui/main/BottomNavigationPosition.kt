package com.woocommerce.android.ui.main

import com.woocommerce.android.R
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.dashboard.DashboardFragment
import com.woocommerce.android.ui.notifications.NotifsListFragment
import com.woocommerce.android.ui.orders.OrderListFragment

enum class BottomNavigationPosition(val position: Int, val id: Int) {
    DASHBOARD(0, R.id.dashboard),
    ORDERS(1, R.id.orders),
    NOTIFICATIONS(2, R.id.notifications)
}

fun findNavigationPositionById(id: Int): BottomNavigationPosition = when (id) {
    BottomNavigationPosition.DASHBOARD.id -> BottomNavigationPosition.DASHBOARD
    BottomNavigationPosition.ORDERS.id -> BottomNavigationPosition.ORDERS
    BottomNavigationPosition.NOTIFICATIONS.id -> BottomNavigationPosition.NOTIFICATIONS
    else -> BottomNavigationPosition.DASHBOARD
}

fun BottomNavigationPosition.getTag(): String = when (this) {
    BottomNavigationPosition.DASHBOARD -> DashboardFragment.TAG
    BottomNavigationPosition.ORDERS -> OrderListFragment.TAG
    BottomNavigationPosition.NOTIFICATIONS -> NotifsListFragment.TAG
}

fun BottomNavigationPosition.createFragment(): TopLevelFragment = when (this) {
    BottomNavigationPosition.DASHBOARD -> DashboardFragment.newInstance()
    BottomNavigationPosition.ORDERS -> OrderListFragment.newInstance()
    BottomNavigationPosition.NOTIFICATIONS -> NotifsListFragment.newInstance()
}
