package com.woocommerce.android.ui.orders.details.editing

import androidx.annotation.LayoutRes
import com.woocommerce.android.ui.base.BaseFragment

open class BaseOrderEditFragment : BaseFragment {
    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)
}
