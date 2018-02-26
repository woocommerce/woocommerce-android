package com.woocommerce.android.ui.main

import android.support.v4.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.ui.dashboard.DashboardFragment
import com.woocommerce.android.ui.orders.OrderListFragment
import com.woocommerce.android.ui.settings.SettingsFragment

enum class BottomNavigationPosition(val position: Int, val id: Int) {
    DASHBOARD(0, R.id.dashboard),
    ORDERS(1, R.id.orders),
    SETTINGS(2, R.id.settings)
}

fun findNavigationPositionById(id: Int): BottomNavigationPosition = when (id) {
    BottomNavigationPosition.DASHBOARD.id -> BottomNavigationPosition.DASHBOARD
    BottomNavigationPosition.ORDERS.id -> BottomNavigationPosition.ORDERS
    BottomNavigationPosition.SETTINGS.id -> BottomNavigationPosition.SETTINGS
    else -> BottomNavigationPosition.DASHBOARD
}

fun BottomNavigationPosition.getTag(): String = when (this) {
    BottomNavigationPosition.DASHBOARD -> DashboardFragment.TAG
    BottomNavigationPosition.ORDERS -> OrderListFragment.TAG
    BottomNavigationPosition.SETTINGS -> SettingsFragment.TAG
}

fun BottomNavigationPosition.createFragment(): Fragment = when (this) {
    BottomNavigationPosition.DASHBOARD -> DashboardFragment.newInstance()
    BottomNavigationPosition.ORDERS -> OrderListFragment.newInstance()
    BottomNavigationPosition.SETTINGS -> SettingsFragment.newInstance()
}
