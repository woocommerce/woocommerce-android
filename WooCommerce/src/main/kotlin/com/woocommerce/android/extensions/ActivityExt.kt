package com.woocommerce.android.extensions

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.properties.Delegates

/**
 * Used for passing back some result from a fragment when using the Navigation component
 * It replaces the startActivityForResult() & setResult() call, since the Navigation component uses a single activity.
 */
fun FragmentActivity.navigateBackWithResult(requestCode: Int, result: Bundle, @IdRes navHostId: Int, @IdRes dest: Int) {
    val childFragmentManager = supportFragmentManager.findFragmentById(navHostId)?.childFragmentManager
    var backStackListener: FragmentManager.OnBackStackChangedListener by Delegates.notNull()
    backStackListener = FragmentManager.OnBackStackChangedListener {
        (childFragmentManager?.fragments?.get(0) as? NavigationResult)?.onNavigationResult(requestCode, result)
        childFragmentManager?.removeOnBackStackChangedListener(backStackListener)
    }
    childFragmentManager?.addOnBackStackChangedListener(backStackListener)
    findNavController(navHostId).popBackStack(dest, false)
}

/**
 * Used for passing back some result from a fragment using the Navigation component
 * to one of the top level activities.
 *
 * It reuses the logic from the above method but uses the getActiveTopLevelFragment()
 * from [MainActivity] to get the current active fragment
 */
fun MainActivity.navigateBackWithResult(requestCode: Int, result: Bundle, @IdRes navHostId: Int, @IdRes dest: Int) {
    val childFragmentManager = supportFragmentManager.findFragmentById(navHostId)?.childFragmentManager
    var backStackListener: FragmentManager.OnBackStackChangedListener by Delegates.notNull()
    backStackListener = FragmentManager.OnBackStackChangedListener {
        (getActiveTopLevelFragment() as? NavigationResult)?.onNavigationResult(requestCode, result)
        childFragmentManager?.removeOnBackStackChangedListener(backStackListener)
    }
    childFragmentManager?.addOnBackStackChangedListener(backStackListener)
    findNavController(navHostId).popBackStack(dest, false)
}

/**
 * Used to configure and intercept the close button from any fragment
 * called through the MainActivity
 */
fun MainActivity.configureToolbarWithCloseButton() {
    (toolbar as? Toolbar)?.let {
        it.setNavigationOnClickListener { onBackPressed() }
    }
    supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)
}
