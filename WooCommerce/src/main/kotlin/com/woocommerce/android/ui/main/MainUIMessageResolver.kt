package com.woocommerce.android.ui.main

import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.ui.base.UIMessageResolver
import javax.inject.Inject

/**
 * This class allows for a centralized and injectable way of handling UI-related messaging, for example
 * snackbar messaging.
 */
@ActivityScope
class MainUIMessageResolver @Inject constructor(val activity: MainActivity) : UIMessageResolver() {
    override val snackbarRoot: ViewGroup by lazy {
        activity.findViewById(R.id.snack_root) as ViewGroup
    }
}
