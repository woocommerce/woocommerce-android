package com.woocommerce.android.ui.appwidgets.stats.today

import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.ui.base.UIMessageResolver
import javax.inject.Inject

@ActivityScope
class TodayWidgetUIMessageResolver @Inject constructor(
    val activity: TodayWidgetConfigurationActivity
) : UIMessageResolver {
    override val snackbarRoot: ViewGroup by lazy {
        activity.findViewById(R.id.snack_root) as ViewGroup
    }
}
