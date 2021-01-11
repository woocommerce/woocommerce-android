package com.woocommerce.android.ui.base

import androidx.annotation.LayoutRes
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter

/**
 * The main fragments hosted by the bottom bar should extend this class
 */
abstract class TopLevelFragment : BaseFragment, TopLevelFragmentView {
    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    /**
     * The extending class may use this variable to defer a part of its
     * normal initialization until manually requested.
     */
    var deferInit: Boolean = false

    override var isActive: Boolean = false
        get() {
            return if (isAdded && !isHidden) {
                (activity as? MainNavigationRouter)?.isAtNavigationRoot() ?: false
            } else {
                false
            }
        }

    abstract fun isScrolledToTop(): Boolean

    /**
     * Called when the fragment shows or hides a search view so we can properly disable the collapsing
     * toolbar when a search is active
     */
    fun onSearchViewActiveChanged(isActive: Boolean) {
        (activity as? MainActivity)?.let {
            if (isActive) {
                it.enableToolbarExpansion(false)
                it.expandToolbar(false, false)
            } else {
                it.enableToolbarExpansion(true)
                it.expandToolbar(true, true)
            }
        }
    }
}
