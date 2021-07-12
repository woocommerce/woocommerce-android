package com.woocommerce.android.ui.base

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog

open class BaseFragment : Fragment, BaseFragmentView {
    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

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

    override fun getFragmentTitle(): String {
        return activity?.title?.toString() ?: ""
    }

    override fun getFragmentSubtitle(): String = ""

    protected fun ShowDialog.showDialog() {
        WooDialog.showDialog(
            activity = requireActivity(),
            titleId = this.titleId,
            messageId = this.messageId,
            positiveButtonId = this.positiveButtonId,
            posBtnAction = this.positiveBtnAction,
            negativeButtonId = this.negativeButtonId,
            negBtnAction = this.negativeBtnAction
        )
    }
}
