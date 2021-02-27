package com.woocommerce.android.ui.base

import androidx.annotation.LayoutRes
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import javax.inject.Inject

/**
 * The main fragments hosted by the bottom bar should extend this class
 */
abstract class TopLevelFragment : BaseFragment, TopLevelFragmentView {
    @Inject internal lateinit var fabManager: FabManager

    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    abstract fun shouldExpandToolbar(): Boolean

    abstract val hasFab: Boolean

    override fun onDestroyView() {
        (activity as? MainNavigationRouter)?.let {
            if (!it.currentDestinationHasFab()) {
                fabManager.hideFabImmediately()
            }
        }
        super.onDestroyView()
    }

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
