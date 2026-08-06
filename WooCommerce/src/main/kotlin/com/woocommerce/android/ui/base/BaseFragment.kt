package com.woocommerce.android.ui.base

import androidx.activity.OnBackPressedCallback
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.BackPressTrackerOwner
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog

open class BaseFragment : Fragment, BaseFragmentView {
    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    open val activityAppBarStatus: AppBarStatus = AppBarStatus.Visible()

    /**
     * Lets BackPressListener screens save or confirm before the NavHost consumes a system back event.
     * Registering on resume gives the visible fragment priority in the dispatcher's last-in-first-out order.
     */
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val listener = this@BaseFragment as? BackPressListener ?: return
            if (listener.onRequestAllowBackPress()) {
                continueBackNavigation()
            } else {
                trackConsumedBackPress()
            }
        }
    }

    @CallSuper
    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            updateActivityTitle()
        }
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        updateActivityTitle()
        updateActivitySubtitle()
        if (this is BackPressListener) {
            backPressedCallback.remove()
            backPressedCallback.isEnabled = true
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
        }
    }

    internal fun continueBackNavigation() {
        backPressedCallback.isEnabled = false
        try {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        } finally {
            backPressedCallback.isEnabled = true
        }
    }

    private fun trackConsumedBackPress() {
        val activity = requireActivity()
        (activity as? BackPressTrackerOwner)?.backPressTracker?.trackBackPressed(activity)
            ?: AnalyticsTracker.trackBackPressed(activity)
    }

    fun updateActivityTitle() {
        if (isAdded && !isHidden) {
            activity?.title = getFragmentTitle()
        }
    }

    private fun updateActivitySubtitle() {
        if (isAdded && !isHidden && activity is MainActivity) {
            (activity as MainActivity).setSubtitle(getFragmentSubtitle())
        }
    }

    /**
     * Returns the title which should be displayed in the Activity's Toolbar.
     * This is not used if [activityAppBarStatus] returns [AppBarStatus.Hidden].
     */
    override fun getFragmentTitle(): String {
        return activity?.title?.toString() ?: ""
    }

    /**
     * Returns the title which should be displayed as a subtitle in the Activity's Toolbar.
     * This is not used if [activityAppBarStatus] returns [AppBarStatus.Hidden].
     */
    override fun getFragmentSubtitle(): String = ""

    protected fun ShowDialog.showDialog() = showIn(requireActivity())
}
