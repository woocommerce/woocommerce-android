package com.woocommerce.android.ui.base

import com.woocommerce.android.ui.main.MainNavigationRouter

/**
 * The main fragments hosted by the bottom bar should extend this class
 */
abstract class TopLevelFragment : BaseFragment(), TopLevelFragmentView {
    /**
     * The extending class may use this variable to defer a part of its
     * normal initialization until manually requested.
     */
    var deferInit: Boolean = false

    override var isActive: Boolean = false
        get() {
            return if (isAdded && !isHidden) {
                (activity as? MainNavigationRouter)?.isAtNavigationRoot() ?: false || splitViewSupported
            } else {
                false
            }
        }

    /**
     * This method should return true if the current fragment and device supports split
     * screen mode for tablets.
     */
    override val splitViewSupported = false
}
