package com.woocommerce.android.ui.base

import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog

open class BaseFragment : Fragment, BaseFragmentView {
    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    open val activityAppBarStatus: AppBarStatus = AppBarStatus.Visible()

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

    protected fun ShowDialog.showDialog() {
        WooDialog.showDialog(
            activity = requireActivity(),
            titleId = this.titleId,
            messageId = this.messageId,
            positiveButtonId = this.positiveButtonId,
            posBtnAction = this.positiveBtnAction,
            negativeButtonId = this.negativeButtonId,
            negBtnAction = this.negativeBtnAction,
            neutralButtonId = this.neutralButtonId,
            neutBtAction = this.neutralBtnAction,
            cancellable = this.cancelable,
            onDismiss = this.onDismiss
        )
    }
}
