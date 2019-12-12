package com.woocommerce.android.ui.imageviewer

import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.ui.base.UIMessageResolver
import javax.inject.Inject

@ActivityScope
class ImageViewerUIMessageResolver @Inject constructor(val activity: ImageViewerActivity) : UIMessageResolver {
    override val snackbarRoot: ViewGroup by lazy {
        activity.findViewById(R.id.container) as ViewGroup
    }
}
