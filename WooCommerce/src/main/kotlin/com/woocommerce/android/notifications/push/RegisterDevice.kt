package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.sitepicker.sitevisibility.GetWooVisibleSites
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class RegisterDevice @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val accountStore: AccountStore,
    private val pushNotificationRepository: PushNotificationRepository,
    private val featureFlagRepository: FeatureFlagRepository,
    private val selectedSite: SelectedSite,
    private val getWooVisibleSites: GetWooVisibleSites,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {
    private val orchestrationMutex = Mutex()

    @Volatile
    private var activeJob: Job? = null

    fun kickoff(trigger: Trigger) {
        appCoroutineScope.launch {
            invoke(trigger)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend operator fun invoke(trigger: Trigger) {
        if (trigger == Trigger.TOKEN_REFRESH && activeJob != null) {
            WooLog.d(WooLog.T.NOTIFICATIONS, "Cancelling in-progress push registration before $trigger")
            activeJob?.cancel()
        }

        coroutineScope {
            orchestrationMutex.withLock {
                WooLog.d(WooLog.T.NOTIFICATIONS, "Starting push registration for $trigger")
                val registrationJob = launch {
                    try {
                        register(trigger)
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (throwable: Throwable) {
                        WooLog.e(WooLog.T.NOTIFICATIONS, "Push registration kickoff failed for $trigger", throwable)
                    }
                }

                activeJob = registrationJob
                try {
                    registrationJob.join()
                } finally {
                    if (activeJob === registrationJob) {
                        activeJob = null
                    }
                }
            }
        }
    }

    private suspend fun register(trigger: Trigger) {
        val token = appPrefsWrapper.getFCMToken()
        if (token.isEmpty()) {
            WooLog.d(WooLog.T.NOTIFICATIONS, "Skipping push registration for $trigger because FCM token is empty")
            return
        }

        val shouldForce = trigger == Trigger.TOKEN_REFRESH

        if (featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1)) {
            pushNotificationRepository.clearWooPushRegistrationsForStaleToken(token)

            val sites = when (trigger) {
                Trigger.LOGIN_SUCCESS,
                Trigger.TOKEN_REFRESH -> getWooVisibleSites()

                Trigger.APP_FOREGROUND,
                Trigger.SITE_SWITCH -> listOfNotNull(selectedSite.getIfExists())
            }
            supervisorScope {
                sites.map { site ->
                    async {
                        val shouldRegisterSite = shouldForce ||
                            pushNotificationRepository.shouldRegisterWooPushForSite(token, site.siteId)

                        if (shouldRegisterSite) {
                            WooLog.d(
                                WooLog.T.NOTIFICATIONS,
                                "Registering Woo push for site ${site.siteId} for $trigger"
                            )
                            pushNotificationRepository.registerPushTokenInWooCoreSystem(
                                token = token,
                                selectedSite = site,
                                allowWpComFallback = trigger != Trigger.SITE_SWITCH
                            )
                        }
                    }
                }.awaitAll()
            }
        } else {
            migrateWooPushRegistrationsToWpCom(trigger, token)
        }

        // For WPCom, site switching doesn't affect registration
        val shouldEvaluateWpCom = trigger != Trigger.SITE_SWITCH &&
            (shouldForce || !pushNotificationRepository.isWpComPushRegistered())

        if (shouldEvaluateWpCom && accountStore.hasAccessToken()) {
            WooLog.d(WooLog.T.NOTIFICATIONS, "Registering WP.com push for $trigger")
            pushNotificationRepository.registerPushTokenInWpComSystem(token)
        } else {
            WooLog.d(WooLog.T.NOTIFICATIONS, "Skipping WP.com push registration for $trigger")
        }
    }

    private suspend fun migrateWooPushRegistrationsToWpCom(trigger: Trigger, token: String) {
        if (trigger == Trigger.SITE_SWITCH) return

        val wooRegisteredSiteIds = pushNotificationRepository.getWooPushRegisteredSiteIds()
        if (wooRegisteredSiteIds.isEmpty()) return

        WooLog.d(
            WooLog.T.NOTIFICATIONS,
            "Migrating Woo push registrations back to WP.com for sites $wooRegisteredSiteIds"
        )

        val wpComFallbackSiteIds = if (accountStore.hasAccessToken()) {
            getWooVisibleSites()
                .filter { it.siteId in wooRegisteredSiteIds && it.connectionType == SiteConnectionType.Jetpack }
                .map { it.siteId }
                .toSet()
        } else {
            emptySet()
        }

        val isWpComTakenOver = wpComFallbackSiteIds.isNotEmpty() &&
            ensureWpComPushRegistered(token) &&
            pushNotificationRepository.enableWpComNotificationsForSites(wpComFallbackSiteIds).isSuccess

        // Sites with a working WP.com fallback are unregistered only after WP.com takes over, so a
        // WP.com failure leaves them on Woo push until the next run retries. Sites without a
        // fallback are unregistered unconditionally.
        val siteIdsToUnregister = if (isWpComTakenOver) {
            wooRegisteredSiteIds
        } else {
            wooRegisteredSiteIds - wpComFallbackSiteIds
        }
        pushNotificationRepository.unregisterWooPushRegisteredSites(siteIdsToUnregister)
    }

    private suspend fun ensureWpComPushRegistered(token: String): Boolean {
        if (pushNotificationRepository.isWpComPushRegistered()) return true

        return !pushNotificationRepository.registerPushTokenInWpComSystem(token).isError
    }

    enum class Trigger {
        LOGIN_SUCCESS,
        APP_FOREGROUND,
        SITE_SWITCH,
        TOKEN_REFRESH
    }
}
