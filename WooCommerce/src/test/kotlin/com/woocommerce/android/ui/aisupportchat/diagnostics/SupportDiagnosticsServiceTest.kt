package com.woocommerce.android.ui.aisupportchat.diagnostics

import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.INTERNET_CONNECTION
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.STORE_CONNECTION
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.STORE_ORDERS
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.STORE_PRODUCTS
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest.WPCOM_SERVERS
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.FailureType
import com.woocommerce.android.ui.troubleshooting.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreProductsCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.WPComConnectionCheckUseCase
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SupportDiagnosticsServiceTest : BaseUnitTest() {
    private val internetCheck: InternetConnectionCheckUseCase = mock()
    private val wpComCheck: WPComConnectionCheckUseCase = mock()
    private val storeConnectionCheck: StoreConnectionCheckUseCase = mock()
    private val storeOrdersCheck: StoreOrdersCheckUseCase = mock()
    private val storeProductsCheck: StoreProductsCheckUseCase = mock()

    private val service = SupportDiagnosticsService(
        internetConnectionCheck = internetCheck,
        wpComConnectionCheck = wpComCheck,
        storeConnectionCheck = storeConnectionCheck,
        storeOrdersCheck = storeOrdersCheck,
        storeProductsCheck = storeProductsCheck
    )

    @Test
    fun `given LOADING_ORDERS, when run, then order diagnostics are included`() =
        testBlocking {
            stubAll(success = true)

            val initial = service.runDiagnostics(SupportIssueType.LOADING_ORDERS).toList().first()

            assertThat(initial.statuses.map(DiagnosticStatus::test))
                .containsExactly(INTERNET_CONNECTION, WPCOM_SERVERS, STORE_CONNECTION, STORE_ORDERS)
        }

    @Test
    fun `given all LOADING_ORDERS checks pass, when run, then each status transition is emitted`() =
        testBlocking {
            stubAll(success = true)

            val emissions = service.runDiagnostics(SupportIssueType.LOADING_ORDERS).toList()

            assertThat(emissions).hasSize(1 + 4 * 2)
        }

    @Test
    fun `given all LOADING_ORDERS checks pass, when run, then final statuses are passed`() =
        testBlocking {
            stubAll(success = true)

            val final = service.runDiagnostics(SupportIssueType.LOADING_ORDERS).toList().last()

            assertThat(final.statuses.map(DiagnosticStatus::status))
                .allMatch { it is TestStatus.Passed }
        }

    @Test
    fun `given all LOADING_ORDERS checks pass, when run, then final result is complete`() =
        testBlocking {
            stubAll(success = true)

            val final = service.runDiagnostics(SupportIssueType.LOADING_ORDERS).toList().last()

            assertThat(final.isComplete).isTrue
        }

    @Test
    fun `given WPCOM_SERVERS fails, when run for LOADING_ORDERS, then retry is suggested`() =
        testBlocking {
            stubWpComFailure()

            val final = service.runDiagnostics(SupportIssueType.LOADING_ORDERS).toList().last()

            assertThat(final.suggestedAction).isEqualTo(SuggestedFixAction.RetryDiagnostics)
        }

    @Test
    fun `given WPCOM_SERVERS fails, when run for LOADING_ORDERS, then failure metadata is preserved`() =
        testBlocking {
            stubWpComFailure()

            val final = service.runDiagnostics(SupportIssueType.LOADING_ORDERS).toList().last()

            assertThat(final.firstFailure).isEqualTo(
                DiagnosticStatus(
                    test = WPCOM_SERVERS,
                    status = TestStatus.Failed(
                        failureType = FailureType.TIMEOUT,
                        technicalDetails = "WPCom 503",
                        durationMs = 250L
                    )
                )
            )
        }

    @Test
    fun `given WPCOM_SERVERS fails, when run for LOADING_ORDERS, then later tests stay pending`() =
        testBlocking {
            stubWpComFailure()

            val final = service.runDiagnostics(SupportIssueType.LOADING_ORDERS).toList().last()
            val pendingStatuses = final.statuses
                .filter { it.test == STORE_CONNECTION || it.test == STORE_ORDERS }
                .map(DiagnosticStatus::status)

            assertThat(pendingStatuses).allMatch { it is TestStatus.Pending }
        }

    @Test
    fun `given check throws, when run, then retry is suggested`() = testBlocking {
        stubInternetThrows()

        val final = service.runDiagnostics(SupportIssueType.LOADING_ORDERS).toList().last()

        assertThat(final.suggestedAction).isEqualTo(SuggestedFixAction.RetryDiagnostics)
    }

    @Test
    fun `given check throws, when run, then failure metadata is emitted`() = testBlocking {
        stubInternetThrows()

        val final = service.runDiagnostics(SupportIssueType.LOADING_ORDERS).toList().last()

        assertThat(final.firstFailure).isEqualTo(
            DiagnosticStatus(
                test = INTERNET_CONNECTION,
                status = TestStatus.Failed(
                    failureType = FailureType.GENERIC,
                    technicalDetails = "No selected site"
                )
            )
        )
    }

    @Test
    fun `given OTHER issue type, when run, then all tests are included`() = testBlocking {
        stubAll(success = true)

        val initial = service.runDiagnostics(SupportIssueType.OTHER).toList().first()

        assertThat(initial.statuses.map(DiagnosticStatus::test))
            .containsExactly(INTERNET_CONNECTION, WPCOM_SERVERS, STORE_CONNECTION, STORE_ORDERS, STORE_PRODUCTS)
    }

    @Test
    fun `given LOADING_PRODUCTS, when run, then product check is included instead of order check`() = testBlocking {
        stubAll(success = true)

        val initial = service.runDiagnostics(SupportIssueType.LOADING_PRODUCTS).toList().first()

        assertThat(initial.statuses.map(DiagnosticStatus::test))
            .containsExactly(INTERNET_CONNECTION, WPCOM_SERVERS, STORE_CONNECTION, STORE_PRODUCTS)
    }

    @Test
    fun `given LOADING_ANALYTICS, when run, then only the three connectivity checks run`() = testBlocking {
        stubAll(success = true)

        val initial = service.runDiagnostics(SupportIssueType.LOADING_ANALYTICS).toList().first()

        assertThat(initial.statuses.map(DiagnosticStatus::test))
            .containsExactly(INTERNET_CONNECTION, WPCOM_SERVERS, STORE_CONNECTION)
    }

    @Test
    fun `given RECEIVING_NOTIFICATIONS, when run, then only the three connectivity checks run`() = testBlocking {
        stubAll(success = true)

        val initial = service.runDiagnostics(SupportIssueType.RECEIVING_NOTIFICATIONS).toList().first()

        assertThat(initial.statuses.map(DiagnosticStatus::test))
            .containsExactly(INTERNET_CONNECTION, WPCOM_SERVERS, STORE_CONNECTION)
    }

    private fun stubAll(success: Boolean) {
        val outcome: ConnectivityCheckStatus = if (success) {
            ConnectivityCheckStatus.Success()
        } else {
            ConnectivityCheckStatus.Failure()
        }
        whenever(internetCheck.invoke()).thenReturn(flowOf(outcome))
        whenever(wpComCheck.invoke()).thenReturn(flowOf(outcome))
        whenever(storeConnectionCheck.invoke()).thenReturn(flowOf(outcome))
        whenever(storeOrdersCheck.invoke()).thenReturn(flowOf(outcome))
        whenever(storeProductsCheck.invoke()).thenReturn(flowOf(outcome))
    }

    private fun stubWpComFailure() {
        whenever(internetCheck.invoke()).thenReturn(flowOf(ConnectivityCheckStatus.Success()))
        whenever(wpComCheck.invoke()).thenReturn(
            flowOf(
                ConnectivityCheckStatus.Failure(
                    error = FailureType.TIMEOUT,
                    technicalDetails = "WPCom 503",
                    durationMs = 250L
                )
            )
        )
    }

    private fun stubInternetThrows() {
        whenever(internetCheck.invoke()).thenReturn(
            flow {
                emit(ConnectivityCheckStatus.InProgress)
                throw IllegalStateException("No selected site")
            }
        )
    }
}
