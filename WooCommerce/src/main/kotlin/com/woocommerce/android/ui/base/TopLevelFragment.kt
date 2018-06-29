package com.woocommerce.android.ui.base

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.fragment_parent.*

/**
 * The main fragments hosted by the bottom bar should extend this class to enforce
 * consistent navigation across top-level fragments and their children.
 */
abstract class TopLevelFragment : Fragment(), TopLevelFragmentView {
    companion object {
        // Bundle label to store the state of this top-level fragment.
        // If the value associated with this label is true, then this
        // fragment is currently hosting a child fragment (drilled in).
        const val CHILD_FRAGMENT_ACTIVE = "child-fragment-active"
    }

    var isActive: Boolean = false
        get() = field && childFragmentManager.backStackEntryCount == 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.addOnBackStackChangedListener(this)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set activity title
        activity?.title = getFragmentTitle()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            // Update the activity title
            activity?.title = getFragmentTitle()
        }
    }

    override fun onDestroyView() {
        container.removeAllViews()
        super.onDestroyView()
    }

    override fun getFragmentFromBackStack(tag: String): Fragment? {
        return childFragmentManager.findFragmentByTag(tag)
    }

    override fun popToState(tag: String): Boolean {
        return childFragmentManager.popBackStackImmediate(tag, 0)
    }

    override fun loadChildFragment(fragment: Fragment, tag: String) {
        childFragmentManager.beginTransaction()
                .replace(R.id.container, fragment, tag)
                .addToBackStack(tag)
                .commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the current view state of this top-level fragment.
        outState.putBoolean(CHILD_FRAGMENT_ACTIVE, childFragmentManager.backStackEntryCount > 0)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        savedInstanceState?.let { bundle ->
            val childViewActive = bundle.getBoolean(CHILD_FRAGMENT_ACTIVE, false)
            updateParentViewState(childViewActive)
        }
    }

    /**
     * Set the top-level fragment view to [View.GONE] if a child has been added
     * to prevent click events from passing through to the covered view.
     *
     * If all child views have been removed, set the top-level fragment view to
     * [View.VISIBLE] to enable click events.
     */
    override fun onBackStackChanged() {
        updateParentViewState(childFragmentManager.backStackEntryCount > 0)
    }

    /**
     * Update the parent view state to match hierarchy status. If [childActive]
     * is true, then set the title bar to appropriately show the "up" option,
     * and set the title to the active child fragments title.
     */
    private fun updateParentViewState(childActive: Boolean) {
        val mainActivity: AppCompatActivity? = activity as AppCompatActivity
        if (childActive) {
            container.getChildAt(0).visibility = View.GONE
            mainActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            mainActivity?.supportActionBar?.setDisplayShowHomeEnabled(true)
        } else {
            container.getChildAt(0).visibility = View.VISIBLE
            mainActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            mainActivity?.supportActionBar?.setDisplayShowHomeEnabled(false)
            mainActivity?.title = getFragmentTitle()
        }
    }
}
