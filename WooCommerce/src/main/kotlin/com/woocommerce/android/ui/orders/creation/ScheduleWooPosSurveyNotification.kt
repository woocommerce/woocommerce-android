package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.local.LocalNotification
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.util.FeatureFlag
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class ScheduleWooPosSurveyNotification @Inject constructor(
    private val localNotificationScheduler: LocalNotificationScheduler,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SiteModel,
) {
    operator fun invoke() {
        if (FeatureFlag.WOO_POS_SURVEYS.isEnabled() &&
            !appPrefsWrapper.isWooPosSurveyNotificationPotentialUserShown
        ) {
            localNotificationScheduler.scheduleNotification(
                LocalNotification.WooPosSurveyPotentialUserNotification(
                    siteId = selectedSite.siteId
                )
            )
        }
    }
}
