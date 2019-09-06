package com.woocommerce.android.util

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import kotlin.properties.Delegates

fun FragmentActivity.navigateBackWithResult(result: Bundle, @IdRes navHostId: Int) {
    val childFragmentManager = supportFragmentManager.findFragmentById(navHostId)?.childFragmentManager
    var backStackListener: FragmentManager.OnBackStackChangedListener by Delegates.notNull()
    backStackListener = FragmentManager.OnBackStackChangedListener {
        (childFragmentManager?.fragments?.get(0) as NavigationResult).onNavigationResult(result)
        childFragmentManager.removeOnBackStackChangedListener(backStackListener)
    }
    childFragmentManager?.addOnBackStackChangedListener(backStackListener)
    findNavController(navHostId).popBackStack()
}
