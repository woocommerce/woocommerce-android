package com.woocommerce.android.ui.base

import androidx.activity.OnBackPressedCallback
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.BackPressTrackerOwner
import com.woocommerce.android.ui.main.BackResolutionOwner
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog

open class BaseFragment : Fragment, BaseFragmentView, BackResolutionOwner {
    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    open val activityAppBarStatus: AppBarStatus = AppBarStatus.Visible()

    // Opt in only when a consumed back can finish through delayed navigation.
    protected open val tracksPendingBackResolution = false
    protected var hasPendingBackResolution = false
        private set

    /**
     * Lets BackPressListener screens save or confirm before the NavHost consumes a system back event.
     * Registering on resume gives the visible fragment priority in the dispatcher's last-in-first-out order.
     */
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val listener = this@BaseFragment as? BackPressListener ?: return
            hasPendingBackResolution = tracksPendingBackResolution
            val allowBackPress = listener.onRequestAllowBackPress()

            if (allowBackPress) {
                clearPendingBackResolution()
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
        val activity = requireActivity()
        if (consumePendingBackResolution()) {
            (activity as? BackPressTrackerOwner)?.backPressTracker?.armNextTrackSuppression()
        }
        backPressedCallback.isEnabled = false
        try {
            activity.onBackPressedDispatcher.onBackPressed()
        } finally {
            backPressedCallback.isEnabled = true
        }
    }

    internal fun clearPendingBackResolution() {
        hasPendingBackResolution = false
    }

    override fun consumePendingBackResolution(): Boolean {
        return hasPendingBackResolution.also { hasPendingBackResolution = false }
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
