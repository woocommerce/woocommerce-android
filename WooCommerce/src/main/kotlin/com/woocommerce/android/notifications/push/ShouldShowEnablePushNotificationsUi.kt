package com.woocommerce.android.notifications.push

import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus.Status
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Checks whether the "Enable Push Notifications" UI should be shown.
 *
 * This is part of the Woo Core push notifications system for app-password authenticated sites.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShouldShowEnablePushNotificationsUi @Inject constructor(
    private val selectedSite: SelectedSite,
    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus,
    private val featureFlagRepository: FeatureFlagRepository
) {
    operator fun invoke(): Flow<Boolean> = selectedSite.observe()
        .flatMapLatest { site ->
            flow {
                if (!featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M2)) {
                    emit(false)
                    return@flow
                }

                if (site == null || site.connectionType != SiteConnectionType.ApplicationPasswords) {
                    emit(false)
                } else {
                    emitAll(
                        pushNotificationRegistrationStatus.observe(site.siteId).map { registrationStatus ->
                            registrationStatus != Status.REGISTERED_WOO_ONLY &&
                                registrationStatus != Status.REGISTERED_BOTH
                        }
                    )
                }
            }
        }
}
