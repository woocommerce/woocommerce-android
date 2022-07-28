package com.woocommerce.android.ui.moremenu

import com.woocommerce.android.ui.moremenu.MoreMenuNewFeature.Payments
import dagger.Reusable
import javax.inject.Inject

@Reusable
class MoreMenuNewFeatureProvider @Inject constructor() {
    fun provideNewFeatures() = listOf(Payments)
}

enum class MoreMenuNewFeature {
    Payments
}
