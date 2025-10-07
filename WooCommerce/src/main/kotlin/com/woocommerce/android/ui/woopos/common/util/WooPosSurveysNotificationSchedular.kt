package com.woocommerce.android.ui.woopos.common.util

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.local.LocalNotification
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.util.FeatureFlag
import kotlinx.coroutines.flow.first
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WooPosSurveysNotificationSchedular @Inject constructor(
    private val localNotificationScheduler: LocalNotificationScheduler,
    private val appPrefs: AppPrefsWrapper,
    private val wooPosPreferencesRepository: WooPosPreferencesRepository,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
) {
    companion object {
        private val ALLOWED_COUNTRIES = setOf("us", "gb")
        private const val CURRENT_USER_NOTIFICATION_DELAY_MINUTES = 5L
    }

    suspend fun schedularPotentialUserSurveyNotification() {
        if (!appPrefs.isWooPosSurveyNotificationPotentialUserShown &&
            !wooPosPreferencesRepository.wasOpenedOnce.first() &&
            areNotificationsAllowed()
        ) {
            localNotificationScheduler.scheduleNotification(
                LocalNotification.WooPosSurveyPotentialUserNotification(
                    siteId = selectedSite.get().siteId
                )
            )
        }
    }

    suspend fun schedularCurrentUserSurveyNotification() {
        if (!appPrefs.isWooPosSurveyNotificationCurrentUserShown &&
            wooPosPreferencesRepository.wasOpenedOnce.first() &&
            areNotificationsAllowed()
        ) {
            localNotificationScheduler.scheduleNotification(
                LocalNotification.WooPosSurveyCurrentUserNotification(
                    delay = TimeUnit.MINUTES.toMillis(CURRENT_USER_NOTIFICATION_DELAY_MINUTES),
                    siteId = selectedSite.get().siteId
                )
            )
        }
    }

    private suspend fun areNotificationsAllowed(): Boolean =
        isAllowedCountry() && FeatureFlag.WOO_POS_SURVEYS.isEnabled()

    private suspend fun isAllowedCountry(): Boolean {
        val countryCode = wooCommerceStore.getSiteSettingsAsync(selectedSite.get())?.countryCode
        return countryCode?.lowercase() in ALLOWED_COUNTRIES
    }
}
