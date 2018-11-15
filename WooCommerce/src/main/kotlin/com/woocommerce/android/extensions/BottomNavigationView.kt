package com.woocommerce.android.extensions

import android.support.design.widget.BottomNavigationView

fun BottomNavigationView.active(position: Int) {
    menu.getItem(position).isChecked = true
}
