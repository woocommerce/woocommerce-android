package com.woocommerce.android.ui.base

import android.os.Bundle
import android.view.View

/**
 * All top level fragments and child fragments should extend this class to provide a consistent method
 * of setting the activity title
 */
abstract class BaseFragment : androidx.fragment.app.Fragment(), BaseFragmentView {
    companion object {
        private const val KEY_TITLE = "title"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            activity?.title = it.getString(KEY_TITLE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_TITLE, getFragmentTitle())
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateActivityTitle()
        }
    }

    override fun onResume() {
        super.onResume()
        updateActivityTitle()
    }

    fun updateActivityTitle() {
        // if this is a top level fragment, skip setting the title when it's not active in the bottom nav
        (this as? TopLevelFragment)?.let {
            if (!it.isActive) {
                return
            }
        }
        if (isAdded && !isHidden) {
            activity?.title = getFragmentTitle()
        }
    }
}
