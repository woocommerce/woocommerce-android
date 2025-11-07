package com.woocommerce.android.ui.woopos.common.util

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.local.LocalNotification
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import kotlinx.coroutines.flow.first
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WooPosSurveysNotificationScheduler @Inject constructor(
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

    suspend fun schedulePotentialUserSurveyNotification() {
        if (!appPrefs.isWooPosSurveyNotificationPotentialUserShown &&
            !wooPosPreferencesRepository.wasOpenedOnce.first() &&
            isAllowedCountry()
        ) {
            localNotificationScheduler.scheduleNotification(
                LocalNotification.WooPosSurveyPotentialUserNotification(
                    siteId = selectedSite.get().siteId
                )
            )
        }
    }

    suspend fun scheduleCurrentUserSurveyNotification() {
        if (!appPrefs.isWooPosSurveyNotificationCurrentUserShown &&
            wooPosPreferencesRepository.wasOpenedOnce.first() &&
            isAllowedCountry()
        ) {
            localNotificationScheduler.scheduleNotification(
                LocalNotification.WooPosSurveyCurrentUserNotification(
                    delay = TimeUnit.MINUTES.toMillis(CURRENT_USER_NOTIFICATION_DELAY_MINUTES),
                    siteId = selectedSite.get().siteId
                )
            )
        }
    }

    private suspend fun isAllowedCountry(): Boolean {
        val countryCode = wooCommerceStore.getSiteSettingsAsync(selectedSite.get())?.countryCode
        return countryCode?.lowercase() in ALLOWED_COUNTRIES
    }
}
