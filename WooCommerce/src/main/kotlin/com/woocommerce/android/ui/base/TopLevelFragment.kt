package com.woocommerce.android.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
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
                (activity as? MainNavigationRouter)?.isAtNavigationRoot() ?: false
            } else {
                false
            }
        }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = onCreateFragmentView(inflater, container, savedInstanceState)
}
