package com.woocommerce.android.ui.main

import com.woocommerce.android.R

const val MY_STORE_POSITION = 0
const val ORDERS_POSITION = 1
const val PRODUCTS_POSITION = 2
const val POS_POSITION = 3
const val BOOKINGS_POSITION = 4
const val MORE_POSITION = 5

enum class BottomNavigationPosition(val position: Int, val id: Int) {
    MY_STORE(MY_STORE_POSITION, R.id.dashboard),
    ORDERS(ORDERS_POSITION, R.id.orders),
    PRODUCTS(PRODUCTS_POSITION, R.id.products),
    POS(POS_POSITION, R.id.point_of_sale),
    BOOKINGS(BOOKINGS_POSITION, R.id.bookings),
    MORE(MORE_POSITION, R.id.moreMenu)
}

fun findNavigationPositionById(id: Int): BottomNavigationPosition = when (id) {
    BottomNavigationPosition.MY_STORE.id -> BottomNavigationPosition.MY_STORE
    BottomNavigationPosition.ORDERS.id -> BottomNavigationPosition.ORDERS
    BottomNavigationPosition.PRODUCTS.id -> BottomNavigationPosition.PRODUCTS
    BottomNavigationPosition.BOOKINGS.id -> BottomNavigationPosition.BOOKINGS
    BottomNavigationPosition.POS.id -> BottomNavigationPosition.POS
    BottomNavigationPosition.MORE.id -> BottomNavigationPosition.MORE
    else -> BottomNavigationPosition.MY_STORE
}

fun BottomNavigationPosition.getTag(): String = id.toString()
