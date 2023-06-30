package com.woocommerce.android.ui.payments

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment
import com.woocommerce.android.R

abstract class PaymentsBaseDialogFragment(@LayoutRes val contentLayoutId: Int) : DialogFragment(contentLayoutId) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog_RoundedCorners)
    }
}
