package com.woocommerce.android.ui.main

import android.app.Activity
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.UIMessageResolver
import javax.inject.Inject

class MainUIMessageResolver @Inject constructor(activity: Activity) : UIMessageResolver {
    override val snackbarRoot: ViewGroup by lazy {
        requireNotNull(activity.findViewById(R.id.snack_root)) {
            "To be able to use UIMessageResolver, the activity has to contain a Layout with id snack_root"
        }
    }
}
