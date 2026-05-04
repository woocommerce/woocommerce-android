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
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    fun `given LOADING_ORDERS and all checks pass, when run, then four tests transition pending-running-passed`() =
        testBlocking {
            stubAll(success = true)

            val emissions = service.runDiagnostics(SupportIssueType.LOADING_ORDERS).toList()

            // 1 initial pending + 2 transitions per test (running, passed) × 4 tests = 9 emissions.
            assertThat(emissions).hasSize(1 + 4 * 2)

            val initial = emissions.first()
            assertThat(initial.statuses.map { it.first })
                .containsExactly(INTERNET_CONNECTION, WPCOM_SERVERS, STORE_CONNECTION, STORE_ORDERS)
            assertThat(initial.statuses.map { it.second })
                .allMatch { it is TestStatus.Pending }

            val finalResult = emissions.last()
            assertThat(finalResult.suggestedAction).isNull()
            assertThat(finalResult.firstFailure).isNull()
            assertThat(finalResult.statuses.map { it.second })
                .allMatch { it is TestStatus.Passed }
            assertThat(finalResult.isComplete).isTrue
        }

    @Test
    fun `given WPCOM_SERVERS fails, when run for LOADING_ORDERS, then later tests stay pending and retry is suggested`() =
        testBlocking {
            whenever(internetCheck.invoke()).thenReturn(flowOf(ConnectivityCheckStatus.Success()))
            whenever(wpComCheck.invoke()).thenReturn(
                flowOf(
                    ConnectivityCheckStatus.Failure(
                        technicalDetails = "WPCom 503"
                    )
                )
            )

            val final = service.runDiagnostics(SupportIssueType.LOADING_ORDERS).toList().last()

            assertThat(final.suggestedAction).isEqualTo(SuggestedFixAction.RetryDiagnostics)

            val failure = final.firstFailure
            assertThat(failure).isNotNull
            assertThat(failure!!.first).isEqualTo(WPCOM_SERVERS)
            assertThat(failure.second.message).isEqualTo("WPCom 503")

            // STORE_CONNECTION + STORE_ORDERS must not have started.
            val storeConnectionStatus = final.statuses.first { it.first == STORE_CONNECTION }.second
            val storeOrdersStatus = final.statuses.first { it.first == STORE_ORDERS }.second
            assertThat(storeConnectionStatus).isInstanceOf(TestStatus.Pending::class.java)
            assertThat(storeOrdersStatus).isInstanceOf(TestStatus.Pending::class.java)
        }

    @Test
    fun `given OTHER issue type, when run, then a single empty pending result is emitted`() = testBlocking {
        val emissions = service.runDiagnostics(SupportIssueType.OTHER).toList()

        assertThat(emissions).hasSize(1)
        assertThat(emissions.single().statuses).isEmpty()
        assertThat(emissions.single().suggestedAction).isNull()
        assertThat(emissions.single().isComplete).isTrue
    }

    @Test
    fun `given LOADING_PRODUCTS, when run, then product check is included instead of order check`() = testBlocking {
        stubAll(success = true)

        val initial = service.runDiagnostics(SupportIssueType.LOADING_PRODUCTS).toList().first()

        assertThat(initial.statuses.map { it.first })
            .containsExactly(INTERNET_CONNECTION, WPCOM_SERVERS, STORE_CONNECTION, STORE_PRODUCTS)
    }

    @Test
    fun `given LOADING_ANALYTICS, when run, then only the three connectivity checks run`() = testBlocking {
        stubAll(success = true)

        val initial = service.runDiagnostics(SupportIssueType.LOADING_ANALYTICS).toList().first()

        assertThat(initial.statuses.map { it.first })
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
}
