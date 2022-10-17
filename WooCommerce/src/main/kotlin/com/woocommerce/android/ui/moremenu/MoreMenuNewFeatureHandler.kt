package com.woocommerce.android.ui.moremenu

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.moremenu.MoreMenuNewFeature.Payments
import dagger.Reusable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@Reusable
class MoreMenuNewFeatureHandler @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    val moreMenuNewFeaturesAvailable = appPrefsWrapper.observePrefs()
        .onStart { emit(Unit) }
        .map {
            if (appPrefsWrapper.isUserSeenNewFeatureOnMoreScreen()) {
                emptyList()
            } else {
                listOf(Payments)
            }
        }

    val moreMenuPaymentsFeatureWasClicked = appPrefsWrapper.observePrefs()
        .onStart { emit(Unit) }
        .map { appPrefsWrapper.isPaymentsIconWasClickedOnMoreScreen() }

    fun markPaymentsIconAsClicked() {
        appPrefsWrapper.setPaymentsIconWasClickedOnMoreScreen()
    }

    fun markNewFeatureAsSeen() {
        appPrefsWrapper.setUserSeenNewFeatureOnMoreScreen()
    }
}

enum class MoreMenuNewFeature {
    Payments
}
