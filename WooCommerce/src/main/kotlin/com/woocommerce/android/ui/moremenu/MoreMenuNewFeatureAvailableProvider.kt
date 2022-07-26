package com.woocommerce.android.ui.moremenu

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.moremenu.MoreMenuNewFeature.Payments
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@Reusable
class MoreMenuNewFeatureHandler @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    val moreMenuNewFeaturesAvailable: Flow<List<MoreMenuNewFeature>> =
        appPrefsWrapper.observePrefs()
            .map {
                if (appPrefsWrapper.isUserSeenNewFeatureOnMoreScreen()) {
                    emptyList()
                } else {
                    listOf(Payments)
                }
            }

    fun markNewFeatureAsSeen() {
        appPrefsWrapper.setUserSeenNewFeatureOnMoreScreen()
    }
}

enum class MoreMenuNewFeature {
    Payments
}
