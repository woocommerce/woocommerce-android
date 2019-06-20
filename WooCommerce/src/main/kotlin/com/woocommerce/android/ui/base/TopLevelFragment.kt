package com.woocommerce.android.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.main.MainNavigationRouter
import kotlinx.android.synthetic.main.fragment_parent.*

/**
 * The main fragments hosted by the bottom bar should extend this class
 */
abstract class TopLevelFragment : androidx.fragment.app.Fragment(), TopLevelFragmentView {
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
    ): View? {
        val layout: FrameLayout = inflater.inflate(R.layout.fragment_parent,
                container, false) as FrameLayout
        val view: View? = onCreateFragmentView(inflater, layout, savedInstanceState)
        view?.let {
            layout.addView(view)
        }
        return layout
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateActivityTitle()
        }
    }

    override fun onDestroyView() {
        container.removeAllViews()
        super.onDestroyView()
    }

    fun updateActivityTitle() {
        if (isActive) {
            activity?.title = getFragmentTitle()
        }
    }
}
