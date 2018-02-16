package com.woocommerce.android.extensions

import android.annotation.SuppressLint
import android.support.design.internal.BottomNavigationItemView
import android.support.design.internal.BottomNavigationMenuView
import android.support.design.widget.BottomNavigationView
import android.util.Log

@SuppressLint("RestrictedApi")
fun BottomNavigationView.disableShiftMode() {
    val menuView = getChildAt(0) as BottomNavigationMenuView
    try {
        menuView.javaClass.getDeclaredField("mShiftingMode").also { shiftMode ->
            shiftMode.isAccessible = true
            shiftMode.setBoolean(menuView, false)
            shiftMode.isAccessible = false
        }
        for (i in 0 until menuView.childCount) {
            (menuView.getChildAt(i) as BottomNavigationItemView).also { item ->
                item.setShiftingMode(false)
                item.setChecked(item.itemData.isChecked)
            }
        }
    } catch (e: NoSuchFieldException) {
        Log.e("BottomNavigationHelper", "Unable to get shift mode field", e)
    } catch (e: IllegalAccessException) {
        Log.e("BottomNavigationHelper", "Unable to change value of shift mode", e)
    }
}

fun BottomNavigationView.active(position: Int) {
    menu.getItem(position).isChecked = true
}
