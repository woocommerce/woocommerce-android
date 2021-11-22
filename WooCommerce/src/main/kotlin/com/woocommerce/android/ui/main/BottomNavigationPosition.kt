package com.woocommerce.android.ui.main

import com.woocommerce.android.R

enum class BottomNavigationPosition(val position: Int, val id: Int) {
    MY_STORE(0, R.id.dashboard),
    ANALYTICS(1, R.id.dashboard),
    ORDERS(2, R.id.orders),
    PRODUCTS(3, R.id.products),
    REVIEWS(4, R.id.reviews)
}

fun findNavigationPositionById(id: Int): BottomNavigationPosition = when (id) {
    BottomNavigationPosition.MY_STORE.id -> BottomNavigationPosition.MY_STORE
    BottomNavigationPosition.ANALYTICS.id -> BottomNavigationPosition.ANALYTICS
    BottomNavigationPosition.ORDERS.id -> BottomNavigationPosition.ORDERS
    BottomNavigationPosition.PRODUCTS.id -> BottomNavigationPosition.PRODUCTS
    BottomNavigationPosition.REVIEWS.id -> BottomNavigationPosition.REVIEWS
    else -> BottomNavigationPosition.MY_STORE
}

fun BottomNavigationPosition.getTag(): String = id.toString()
