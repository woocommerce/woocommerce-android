package com.woocommerce.android.ui.main

import android.app.Activity
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class MainUIMessageResolver @Inject constructor(activity: Activity) : UIMessageResolver {
    init {
        if (activity !is MainActivity) {
            throw IllegalStateException("MainUIMessageResolver should be provided only in MainModule")
        }
    }
    override val snackbarRoot: ViewGroup by lazy {
        activity.findViewById(R.id.snack_root) as ViewGroup
    }
}
