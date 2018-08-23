package com.woocommerce.android.ui.orders

import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.ui.base.UIMessageResolver
import javax.inject.Inject

@ActivityScope
class AddNoteUIMessageResolver @Inject constructor(val activity: AddOrderNoteActivity) : UIMessageResolver {
    override val snackbarRoot: ViewGroup by lazy {
        activity.findViewById(R.id.snack_root) as ViewGroup
    }
}
