package com.woocommerce.android.ui.base

import android.content.Context
import android.os.Bundle

/**
 * All top level fragments and child fragments should extend this class to provide a consistent method
 * of setting the activity title
 */
abstract class BaseFragment : androidx.fragment.app.Fragment(), BaseFragmentView {
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateActivityTitle()
        }
    }

    /*
     * First we use onAttach() to set the title as soon as possible...
     */
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        updateActivityTitle()
    }

    /*
     * ...then set the title in onActivityCreated() for fragments whose title depends on loaded
     * data (for example, product detail sets the title to the product name)
     */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        updateActivityTitle()
    }

    fun updateActivityTitle() {
        if (isAdded && !isHidden) {
            activity?.title = getFragmentTitle()
        }
    }
}
