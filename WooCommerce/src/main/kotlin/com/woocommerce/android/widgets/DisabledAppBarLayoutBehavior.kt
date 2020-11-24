package com.woocommerce.android.widgets

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

/**
 * Custom layout behavior which prevents collapsing toolbar from being expanded
 */
class DisabledAppBarLayoutBehavior() : AppBarLayout.Behavior() {
    /**
     * Prevent the toolbar from being dragged
     */
    init {
        setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                return false
            }
        })
    }

    /**
     * Prevent nested scroll
     */
    override fun onStartNestedScroll(
        parent: CoordinatorLayout,
        child: AppBarLayout,
        directTargetChild: View,
        target: View,
        nestedScrollAxes: Int,
        type: Int
    ): Boolean {
        return false
    }
}
