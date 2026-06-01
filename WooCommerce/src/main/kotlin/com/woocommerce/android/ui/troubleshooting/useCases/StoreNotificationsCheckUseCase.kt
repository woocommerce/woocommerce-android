package com.woocommerce.android.ui.troubleshooting.useCases

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus
import com.woocommerce.android.notifications.push.RegisterDevice
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.FailureType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.time.measureTimedValue

class StoreNotificationsCheckUseCase @Inject constructor(
    private val notificationSystemStatusProvider: NotificationSystemStatusProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus,
    private val registerDevice: RegisterDevice
) {
    fun checkPermission(): Flow<ConnectivityCheckStatus> =
        runNotificationCheck(OPERATION_NOTIFICATION_PERMISSION) {
            if (notificationSystemStatusProvider.hasPostNotificationsPermission()) {
                ConnectivityCheckStatus.Success()
            } else {
                notificationFailure(
                    operation = OPERATION_NOTIFICATION_PERMISSION,
                    errorType = ERROR_NOTIFICATION_PERMISSION_DENIED,
                    message = "The app does not have notification permission."
                )
            }
        }

    fun checkAppNotificationsEnabled(): Flow<ConnectivityCheckStatus> =
        runNotificationCheck(OPERATION_APP_NOTIFICATIONS_ENABLED) {
            if (notificationSystemStatusProvider.areAppNotificationsEnabled()) {
                ConnectivityCheckStatus.Success()
            } else {
                notificationFailure(
                    operation = OPERATION_APP_NOTIFICATIONS_ENABLED,
                    errorType = ERROR_APP_NOTIFICATIONS_DISABLED,
                    message = "Notifications are disabled for the WooCommerce app."
                )
            }
        }

    fun checkNotificationChannelsEnabled(): Flow<ConnectivityCheckStatus> =
        runNotificationCheck(OPERATION_NOTIFICATION_CHANNELS_ENABLED) {
            val disabledChannels = notificationSystemStatusProvider.disabledWooNotificationChannels()
            if (disabledChannels.isEmpty()) {
                ConnectivityCheckStatus.Success()
            } else {
                notificationFailure(
                    operation = OPERATION_NOTIFICATION_CHANNELS_ENABLED,
                    errorType = ERROR_NOTIFICATION_CHANNELS_DISABLED,
                    message = "Disabled channels: ${disabledChannels.joinToString { it.name }}"
                )
            }
        }

    fun checkPushToken(): Flow<ConnectivityCheckStatus> =
        runNotificationCheck(OPERATION_PUSH_NOTIFICATION_TOKEN) {
            if (appPrefsWrapper.getFCMToken().isNotBlank()) {
                ConnectivityCheckStatus.Success()
            } else {
                notificationFailure(
                    operation = OPERATION_PUSH_NOTIFICATION_TOKEN,
                    errorType = ERROR_FCM_TOKEN_MISSING,
                    message = "The device does not have an FCM token."
                )
            }
        }

    fun checkPushRegistration(): Flow<ConnectivityCheckStatus> =
        runNotificationCheck(OPERATION_PUSH_NOTIFICATION_REGISTRATION) {
            val status = pushNotificationRegistrationStatus(selectedSite.get().siteId)
            if (status != PushNotificationRegistrationStatus.Status.UNREGISTERED) {
                ConnectivityCheckStatus.Success()
            } else {
                notificationFailure(
                    operation = OPERATION_PUSH_NOTIFICATION_REGISTRATION,
                    errorType = ERROR_PUSH_NOTIFICATIONS_UNREGISTERED,
                    message = "The device is not registered for push notifications."
                )
            }
        }

    suspend fun registerPushNotifications(): Result<Unit> =
        runCatching {
            if (appPrefsWrapper.getFCMToken().isBlank()) {
                error("The device does not have an FCM token.")
            }

            registerDevice(RegisterDevice.Trigger.APP_FOREGROUND)

            val status = pushNotificationRegistrationStatus(selectedSite.get().siteId)
            check(status != PushNotificationRegistrationStatus.Status.UNREGISTERED) {
                "Push notification registration did not complete."
            }
        }

    private fun runNotificationCheck(
        operationName: String,
        check: suspend () -> ConnectivityCheckStatus
    ): Flow<ConnectivityCheckStatus> = flow {
        emit(ConnectivityCheckStatus.InProgress)

        val (status, duration) = measureTimedValue {
            check()
        }

        emit(
            when (status) {
                is ConnectivityCheckStatus.Success -> status.copy(durationMs = duration.inWholeMilliseconds)
                is ConnectivityCheckStatus.Failure -> status.copy(durationMs = duration.inWholeMilliseconds)
                ConnectivityCheckStatus.NotStarted,
                ConnectivityCheckStatus.InProgress -> notificationFailure(
                    operation = operationName,
                    errorType = FailureType.GENERIC.name,
                    message = "Notification diagnostic did not complete."
                )
            }
        )
    }

    private fun notificationFailure(
        operation: String,
        errorType: String,
        message: String
    ) = ConnectivityCheckStatus.Failure(
        error = FailureType.GENERIC,
        technicalDetails = formatErrorDetails(
            operation = operation,
            errorType = errorType,
            message = message
        )
    )

    companion object {
        const val ERROR_NOTIFICATION_PERMISSION_DENIED = "NOTIFICATION_PERMISSION_DENIED"
        const val ERROR_APP_NOTIFICATIONS_DISABLED = "APP_NOTIFICATIONS_DISABLED"
        const val ERROR_NOTIFICATION_CHANNELS_DISABLED = "NOTIFICATION_CHANNELS_DISABLED"
        const val ERROR_FCM_TOKEN_MISSING = "FCM_TOKEN_MISSING"
        const val ERROR_PUSH_NOTIFICATIONS_UNREGISTERED = "PUSH_NOTIFICATIONS_UNREGISTERED"

        const val OPERATION_NOTIFICATION_PERMISSION = "Checking notification permission"
        const val OPERATION_APP_NOTIFICATIONS_ENABLED = "Checking app notification settings"
        const val OPERATION_NOTIFICATION_CHANNELS_ENABLED = "Checking notification channels"
        const val OPERATION_PUSH_NOTIFICATION_TOKEN = "Checking push notification token"
        const val OPERATION_PUSH_NOTIFICATION_REGISTRATION = "Checking push registration"
    }
}
