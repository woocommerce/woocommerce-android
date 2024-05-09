package com.woocommerce.android.pos.ui

sealed class Routing(val route: String) {
    data object PosScreenOne : Routing("pos/posScreenOne")
    data object PosScreenTwo : Routing("pos/posScreenTwo/{id}")
}
