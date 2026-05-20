package com.woocommerce.android.ui.aisupportchat.diagnostics

import com.woocommerce.android.extensions.rethrow
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.ANALYTICS_SETTING
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.APP_NOTIFICATIONS_ENABLED
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.INTERNET_CONNECTION
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.NOTIFICATION_CHANNELS_ENABLED
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.NOTIFICATION_PERMISSION
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.PUSH_NOTIFICATION_REGISTRATION
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.PUSH_NOTIFICATION_TOKEN
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.STORE_CONNECTION
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.STORE_ORDERS
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.STORE_PRODUCTS
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.WPCOM_SERVERS
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.FailureType
import com.woocommerce.android.ui.troubleshooting.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreAnalyticsCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreNotificationsCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreProductsCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.WPComConnectionCheckUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain orchestrator for the AI Support Chat diagnostics flow. Given a [SupportIssueType],
 * runs the appropriate sequence of checks (wrapping the existing connectivity-tool use cases),
 * stops at the first failure, and emits a fresh [DiagnosticResult] after every status transition.
 *
 */
@Singleton
class SupportDiagnosticsService @Inject constructor(
    private val internetConnectionCheck: InternetConnectionCheckUseCase,
    private val wpComConnectionCheck: WPComConnectionCheckUseCase,
    private val storeConnectionCheck: StoreConnectionCheckUseCase,
    private val storeOrdersCheck: StoreOrdersCheckUseCase,
    private val storeProductsCheck: StoreProductsCheckUseCase,
    private val storeAnalyticsCheck: StoreAnalyticsCheckUseCase,
    private val storeNotificationsCheck: StoreNotificationsCheckUseCase
) {
    fun runDiagnostics(issueType: SupportIssueType): Flow<DiagnosticResult> = flow {
        val tests = testsFor(issueType)
        var statuses = tests.map { DiagnosticStatus(it, TestStatus.Pending) }
        emit(DiagnosticResult(issueType, statuses))

        if (tests.isEmpty()) return@flow

        for ((index, test) in tests.withIndex()) {
            statuses = statuses.replaceAt(index, DiagnosticStatus(test, TestStatus.Running))
            emit(DiagnosticResult(issueType, statuses))

            val outcome = runCheckSafely(test)
            val newStatus = outcome.toTestStatus()
            statuses = statuses.replaceAt(index, DiagnosticStatus(test, newStatus))

            if (newStatus is TestStatus.Failed) {
                emit(
                    DiagnosticResult(
                        issueType = issueType,
                        statuses = statuses,
                        suggestedAction = suggestedActionFor(test, newStatus)
                    )
                )
                return@flow
            }

            emit(DiagnosticResult(issueType, statuses))
        }
    }

    private fun testsFor(issueType: SupportIssueType): List<DiagnosticTest> = when (issueType) {
        SupportIssueType.LOADING_ORDERS ->
            listOf(INTERNET_CONNECTION, WPCOM_SERVERS, STORE_CONNECTION, STORE_ORDERS)
        SupportIssueType.LOADING_PRODUCTS ->
            listOf(INTERNET_CONNECTION, WPCOM_SERVERS, STORE_CONNECTION, STORE_PRODUCTS)
        SupportIssueType.LOADING_ANALYTICS ->
            listOf(INTERNET_CONNECTION, WPCOM_SERVERS, STORE_CONNECTION, ANALYTICS_SETTING)
        SupportIssueType.RECEIVING_NOTIFICATIONS ->
            listOf(
                INTERNET_CONNECTION,
                STORE_CONNECTION,
                NOTIFICATION_PERMISSION,
                APP_NOTIFICATIONS_ENABLED,
                NOTIFICATION_CHANNELS_ENABLED,
                PUSH_NOTIFICATION_TOKEN,
                PUSH_NOTIFICATION_REGISTRATION
            )
        SupportIssueType.OTHER ->
            listOf(INTERNET_CONNECTION, WPCOM_SERVERS, STORE_CONNECTION, STORE_ORDERS, STORE_PRODUCTS)
    }

    private fun runCheck(test: DiagnosticTest): Flow<ConnectivityCheckStatus> = when (test) {
        INTERNET_CONNECTION -> internetConnectionCheck()
        WPCOM_SERVERS -> wpComConnectionCheck()
        STORE_CONNECTION -> storeConnectionCheck()
        STORE_ORDERS -> storeOrdersCheck()
        STORE_PRODUCTS -> storeProductsCheck()
        ANALYTICS_SETTING -> storeAnalyticsCheck()
        NOTIFICATION_PERMISSION -> storeNotificationsCheck.checkPermission()
        APP_NOTIFICATIONS_ENABLED -> storeNotificationsCheck.checkAppNotificationsEnabled()
        NOTIFICATION_CHANNELS_ENABLED -> storeNotificationsCheck.checkNotificationChannelsEnabled()
        PUSH_NOTIFICATION_TOKEN -> storeNotificationsCheck.checkPushToken()
        PUSH_NOTIFICATION_REGISTRATION -> storeNotificationsCheck.checkPushRegistration()
    }

    suspend fun enableAnalytics(): Result<Unit> =
        runCatching {
            enableAnalyticsWithRetry(retries = 0)
        }.rethrow<CancellationException, Unit>()

    private suspend fun enableAnalyticsWithRetry(retries: Int) {
        storeAnalyticsCheck.enableAnalytics()
            .getOrElse { error ->
                if (error is CancellationException) throw error
                if (retries < ENABLE_ANALYTICS_MAX_RETRIES) {
                    enableAnalyticsWithRetry(retries = retries + 1)
                } else {
                    throw error
                }
            }
    }

    suspend fun registerPushNotifications(): Result<Unit> =
        runCatching {
            registerPushNotificationsWithRetry(retries = 0)
        }.rethrow<CancellationException, Unit>()

    private suspend fun registerPushNotificationsWithRetry(retries: Int) {
        storeNotificationsCheck.registerPushNotifications()
            .getOrElse { error ->
                if (error is CancellationException) throw error
                if (retries < REGISTER_PUSH_NOTIFICATIONS_MAX_RETRIES) {
                    registerPushNotificationsWithRetry(retries = retries + 1)
                } else {
                    throw error
                }
            }
    }

    private suspend fun runCheckSafely(test: DiagnosticTest): ConnectivityCheckStatus =
        runCatching { runCheck(test).last() }
            .getOrElse { error ->
                if (error is CancellationException) throw error

                ConnectivityCheckStatus.Failure(
                    error = FailureType.GENERIC,
                    technicalDetails = error.message ?: error::class.java.simpleName
                )
            }

    private fun ConnectivityCheckStatus.toTestStatus(): TestStatus = when (this) {
        is ConnectivityCheckStatus.Success -> TestStatus.Passed
        is ConnectivityCheckStatus.Failure -> TestStatus.Failed(
            failureType = error,
            technicalDetails = technicalDetails,
            durationMs = durationMs
        )
        ConnectivityCheckStatus.NotStarted, ConnectivityCheckStatus.InProgress ->
            TestStatus.Failed(technicalDetails = "Diagnostic did not complete")
    }

    private fun suggestedActionFor(test: DiagnosticTest, status: TestStatus.Failed): SuggestedFixAction? =
        when {
            test == ANALYTICS_SETTING &&
                status.technicalDetails?.contains(StoreAnalyticsCheckUseCase.PLUGIN_NOT_ACTIVE_ERROR_TYPE) == true ->
                SuggestedFixAction.EnableAnalytics

            isNotificationSettingsFailure(test, status) ->
                SuggestedFixAction.OpenNotificationSettings

            test == PUSH_NOTIFICATION_REGISTRATION &&
                status.technicalDetails
                    ?.contains(StoreNotificationsCheckUseCase.ERROR_PUSH_NOTIFICATIONS_UNREGISTERED) == true ->
                SuggestedFixAction.RegisterPushNotifications

            else -> null
        }

    private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
        toMutableList().apply { this[index] = value }

    private fun isNotificationSettingsFailure(test: DiagnosticTest, status: TestStatus.Failed): Boolean =
        NOTIFICATION_SETTINGS_ERRORS[test]?.let { errorType ->
            status.technicalDetails?.contains(errorType) == true
        } == true

    private companion object {
        const val ENABLE_ANALYTICS_MAX_RETRIES = 1
        const val REGISTER_PUSH_NOTIFICATIONS_MAX_RETRIES = 1

        val NOTIFICATION_SETTINGS_ERRORS = mapOf(
            NOTIFICATION_PERMISSION to StoreNotificationsCheckUseCase.ERROR_NOTIFICATION_PERMISSION_DENIED,
            APP_NOTIFICATIONS_ENABLED to StoreNotificationsCheckUseCase.ERROR_APP_NOTIFICATIONS_DISABLED,
            NOTIFICATION_CHANNELS_ENABLED to StoreNotificationsCheckUseCase.ERROR_NOTIFICATION_CHANNELS_DISABLED
        )
    }
}
