package com.woocommerce.android.ui.orders.details.editing

import androidx.annotation.LayoutRes
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent

abstract class BaseOrderEditFragment : BaseFragment, BackPressListener {
    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    abstract fun hasChanges(): Boolean

    override fun onRequestAllowBackPress(): Boolean {
        return if (hasChanges()) {
            confirmDisard()
            false
        } else {
            true
        }
    }

    private fun confirmDisard() {
        MultiLiveEvent.Event.ShowDialog.buildDiscardDialogEvent(
            positiveBtnAction = { _, _ ->
                findNavController().navigateUp()
            }
        )
    }
}
