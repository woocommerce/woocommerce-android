package com.woocommerce.android.ui.woopos.common.util

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.local.LocalNotification
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.util.FeatureFlag
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosSurveysNotificationSchedular @Inject constructor(
    private val localNotificationScheduler: LocalNotificationScheduler,
    private val appPrefs: AppPrefsWrapper,
    private val selectedSite: SiteModel,
    private val wooCommerceStore: WooCommerceStore,
) {
    companion object {
        private val ALLOWED_COUNTRIES = setOf("us", "gb")
    }

    suspend fun schedularPotentialUserSurveyNotification() {
        if (FeatureFlag.WOO_POS_SURVEYS.isEnabled() &&
            !appPrefs.isWooPosSurveyNotificationPotentialUserShown &&
            isAllowedCountry()
        ) {
            localNotificationScheduler.scheduleNotification(
                LocalNotification.WooPosSurveyPotentialUserNotification(
                    siteId = selectedSite.siteId
                )
            )
        }
    }

    private suspend fun isAllowedCountry(): Boolean {
        val countryCode = wooCommerceStore.getSiteSettingsAsync(selectedSite)?.countryCode
        return countryCode?.lowercase() in ALLOWED_COUNTRIES
    }
}
