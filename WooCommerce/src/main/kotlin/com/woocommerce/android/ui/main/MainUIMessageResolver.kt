package com.woocommerce.android.ui.main

import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class MainUIMessageResolver @Inject constructor(activity: MainActivity) : UIMessageResolver {
    override val snackbarRoot: ViewGroup by lazy {
        activity.findViewById(R.id.snack_root) as ViewGroup
    }
}
