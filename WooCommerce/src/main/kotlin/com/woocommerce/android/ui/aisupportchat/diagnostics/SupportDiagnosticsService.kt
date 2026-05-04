package com.woocommerce.android.ui.aisupportchat.diagnostics

import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.INTERNET_CONNECTION
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.STORE_CONNECTION
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.STORE_ORDERS
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.STORE_PRODUCTS
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.WPCOM_SERVERS
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreProductsCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.WPComConnectionCheckUseCase
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
    private val storeProductsCheck: StoreProductsCheckUseCase
) {
    fun runDiagnostics(issueType: SupportIssueType): Flow<DiagnosticResult> = flow {
        val tests = testsFor(issueType)
        var statuses: List<Pair<DiagnosticTest, TestStatus>> =
            tests.map { it to TestStatus.Pending }
        emit(DiagnosticResult(issueType, statuses))

        if (tests.isEmpty()) return@flow

        for ((index, test) in tests.withIndex()) {
            statuses = statuses.replaceAt(index, test to TestStatus.Running)
            emit(DiagnosticResult(issueType, statuses))

            val outcome = runCheck(test).last()
            val newStatus = outcome.toTestStatus()
            statuses = statuses.replaceAt(index, test to newStatus)

            if (newStatus is TestStatus.Failed) {
                emit(
                    DiagnosticResult(
                        issueType = issueType,
                        statuses = statuses,
                        suggestedAction = suggestedActionFor(test)
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
            listOf(INTERNET_CONNECTION, WPCOM_SERVERS, STORE_CONNECTION)
        SupportIssueType.RECEIVING_NOTIFICATIONS ->
            listOf(INTERNET_CONNECTION, WPCOM_SERVERS, STORE_CONNECTION)
        SupportIssueType.OTHER -> emptyList()
    }

    private fun runCheck(test: DiagnosticTest): Flow<ConnectivityCheckStatus> = when (test) {
        INTERNET_CONNECTION -> internetConnectionCheck()
        WPCOM_SERVERS -> wpComConnectionCheck()
        STORE_CONNECTION -> storeConnectionCheck()
        STORE_ORDERS -> storeOrdersCheck()
        STORE_PRODUCTS -> storeProductsCheck()
    }

    private fun ConnectivityCheckStatus.toTestStatus(): TestStatus = when (this) {
        is ConnectivityCheckStatus.Success -> TestStatus.Passed
        is ConnectivityCheckStatus.Failure -> TestStatus.Failed(message = technicalDetails)
        ConnectivityCheckStatus.NotStarted, ConnectivityCheckStatus.InProgress ->
            TestStatus.Failed(message = "Diagnostic did not complete")
    }

    private fun suggestedActionFor(failedTest: DiagnosticTest): SuggestedFixAction =
        when (failedTest) {
            INTERNET_CONNECTION,
            WPCOM_SERVERS,
            STORE_CONNECTION,
            STORE_ORDERS,
            STORE_PRODUCTS -> SuggestedFixAction.RetryDiagnostics
        }

    private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
        toMutableList().apply { this[index] = value }
}
