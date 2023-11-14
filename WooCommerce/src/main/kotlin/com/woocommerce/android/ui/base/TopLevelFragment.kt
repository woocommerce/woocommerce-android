package com.woocommerce.android.ui.base

import androidx.annotation.LayoutRes
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity

/**
 * The main fragments hosted by the bottom bar should extend this class
 */
abstract class TopLevelFragment : BaseFragment, TopLevelFragmentView {
    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    abstract fun shouldExpandToolbar(): Boolean

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = null,
            hasShadow = false
        )

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

    fun onListSelectionActiveChanged(isActive: Boolean, expandToolbar: Boolean) {
        (activity as? MainActivity)?.let {
            if (isActive) {
                it.enableToolbarExpansion(false)
                it.expandToolbar(expand = false, animate = false)
            } else {
                it.enableToolbarExpansion(expandToolbar)
                it.expandToolbar(expand = expandToolbar, expandToolbar)
            }
        }
    }
}
