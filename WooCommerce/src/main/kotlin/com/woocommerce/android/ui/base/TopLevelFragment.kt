package com.woocommerce.android.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.R
import com.woocommerce.android.ui.main.MainNavigationRouter
import kotlinx.android.synthetic.main.fragment_parent.*

/**
 * The main fragments hosted by the bottom bar should extend this class
 */
abstract class TopLevelFragment : androidx.fragment.app.Fragment(), TopLevelFragmentView {
    companion object {
        // Bundle label to store the state of this top-level fragment.
        // If the value associated with this label is true, then this
        // fragment is currently hosting a child fragment (drilled in).
        const val CHILD_FRAGMENT_ACTIVE = "child-fragment-active"
    }

    /**
     * The extending class may use this variable to defer a part of its
     * normal initialization until manually requested.
     */
    var deferInit: Boolean = false
    private var runOnResumeFunc: (() -> Unit)? = null

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

    override fun onResume() {
        super.onResume()

        runOnResumeFunc?.let { frag ->
            frag.invoke()
            runOnResumeFunc = null
        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the current view state of this top-level fragment.
        (activity as? MainNavigationRouter)?. let { router ->
            outState.putBoolean(CHILD_FRAGMENT_ACTIVE, router.isChildFragmentShowing())
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        savedInstanceState?.let { bundle ->
            val childViewActive = bundle.getBoolean(CHILD_FRAGMENT_ACTIVE, false)
            updateParentViewState(childViewActive)
        } ?: updateActivityTitle()
    }

    /**
     * Update the parent view state to match hierarchy status. If [childActive]
     * is true, then set the title bar to appropriately show the "up" option,
     * and set the title to the active child fragments title.
     */
    private fun updateParentViewState(childActive: Boolean) {
        val mainActivity: AppCompatActivity? = activity as? AppCompatActivity
        if (childActive) {
            container?.getChildAt(0)?.visibility = View.GONE
            mainActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            mainActivity?.supportActionBar?.setDisplayShowHomeEnabled(true)
        } else {
            container?.getChildAt(0)?.visibility = View.VISIBLE
            mainActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            mainActivity?.supportActionBar?.setDisplayShowHomeEnabled(false)
            updateActivityTitle()
        }
    }

    fun updateActivityTitle() {
        if (isActive) {
            activity?.title = getFragmentTitle()
        }
    }
}
