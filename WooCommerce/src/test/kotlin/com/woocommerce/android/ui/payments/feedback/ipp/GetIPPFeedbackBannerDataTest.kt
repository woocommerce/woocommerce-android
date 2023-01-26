@file:Suppress("MaxLineLength")
package com.woocommerce.android.ui.payments.feedback.ipp

import com.woocommerce.android.R
import com.woocommerce.android.extensions.daysAgo
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentTransactionsSummaryResult
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GetIPPFeedbackBannerDataTest : BaseUnitTest() {
    private val shouldShowFeedbackBanner: ShouldShowFeedbackBanner = mock()
    private val ippStore: WCInPersonPaymentsStore = mock()
    private val siteModel: SiteModel = mock()
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin = mock()
    private val logger: AppLogWrapper = mock()

    private val sut = GetIPPFeedbackBannerData(
        shouldShowFeedbackBanner = shouldShowFeedbackBanner,
        ippStore = ippStore,
        siteModel = siteModel,
        getActivePaymentsPlugin = getActivePaymentsPlugin,
        logger = logger,
    )

    @Before
    fun setup() {
        val fakeSummary = WCPaymentTransactionsSummaryResult(99, "", 0, 0, 0, null, null)
        testBlocking {
            whenever(ippStore.fetchTransactionsSummary(any(), any(), any())).thenReturn(WooPayload(fakeSummary))
        }
    }

    @Test
    fun `given banner should not be displayed, then should throw exception`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(false)

        // then
        assertFailsWith(IllegalStateException::class) {
            testBlocking { sut() }
        }

        Unit
    }

    @Test
    fun `given banner should be shown and no active payments plugin found, then newbie should be detected`() =
        testBlocking {
            // given
            whenever(shouldShowFeedbackBanner()).thenReturn(true)
            whenever(getActivePaymentsPlugin()).thenReturn(null)

            // when
            val result = sut()

            // then
            assertEquals("https://automattic.survey.fm/woo-app-–-cod-survey", result.url)
            assertEquals(R.string.feedback_banner_ipp_title_newbie, result.title)
            assertEquals(R.string.feedback_banner_ipp_message_newbie, result.message)
            assertEquals("ipp_not_user", result.campaignName)
        }

    @Test
    fun `given COD enabled and zero IPP transactions ever, then newbie user should be detected`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(true)

        whenever(getActivePaymentsPlugin())
            .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)

        val fakeNewbieTransactionsSummary = WCPaymentTransactionsSummaryResult(0, "", 0, 0, 0, null, null)
        whenever(ippStore.fetchTransactionsSummary(any(), any(), anyOrNull()))
            .thenReturn(WooPayload(fakeNewbieTransactionsSummary))

        // when
        val result = sut()

        // then
        assertEquals("https://automattic.survey.fm/woo-app-–-cod-survey", result.url)
        assertEquals(R.string.feedback_banner_ipp_title_newbie, result.title)
        assertEquals(R.string.feedback_banner_ipp_message_newbie, result.message)
        assertEquals("ipp_not_user", result.campaignName)
    }

    @Test
    fun `given COD enabled and at least one IPP transaction in last 30 days, then should detect a beginner`() =
        testBlocking {
            // given
            whenever(shouldShowFeedbackBanner()).thenReturn(true)
            whenever(getActivePaymentsPlugin())
                .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)

            val fakeBeginnerTransactionsSummary = WCPaymentTransactionsSummaryResult(1, "", 0, 0, 0, null, null)
            whenever(ippStore.fetchTransactionsSummary(any(), any(), any()))
                .thenReturn(WooPayload(fakeBeginnerTransactionsSummary))

            // when
            val result = sut()

            // then
            assertEquals("https://automattic.survey.fm/woo-app-–-ipp-first-transaction-survey", result.url)
            assertEquals(R.string.feedback_banner_ipp_message_beginner, result.message)
            assertEquals(R.string.feedback_banner_ipp_title_beginner, result.title)
            assertEquals("ipp_new_user", result.campaignName)
        }

    @Test
    fun `given COD enabled and zero IPP transactions in last 30 days and at least one IPP transaction ever, then should detect a beginner`() =
        testBlocking {
            // given
            whenever(shouldShowFeedbackBanner()).thenReturn(true)
            whenever(getActivePaymentsPlugin())
                .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)

            val fakeTransactionsSummaryZeroTransactions = WCPaymentTransactionsSummaryResult(0, "", 0, 0, 0, null, null)
            val fakeTransactionsSummaryOneTransaction = WCPaymentTransactionsSummaryResult(1, "", 0, 0, 0, null, null)

            val expectedTimeWindowCaptor = ArgumentCaptor.forClass(String::class.java)
            whenever(ippStore.fetchTransactionsSummary(any(), any(), expectedTimeWindowCaptor.capture())).thenReturn(
                WooPayload(fakeTransactionsSummaryZeroTransactions),
                WooPayload(fakeTransactionsSummaryOneTransaction)
            )

            // when
            val result = sut()

            // then
            assertEquals(R.string.feedback_banner_ipp_message_beginner, result.message)
            assertEquals(R.string.feedback_banner_ipp_title_beginner, result.title)
            assertEquals("https://automattic.survey.fm/woo-app-–-ipp-first-transaction-survey", result.url)
            assertEquals("https://automattic.survey.fm/woo-app-–-ipp-first-transaction-survey", result.url)
            assertEquals("ipp_new_user", result.campaignName)
            assertNull(expectedTimeWindowCaptor.allValues.last())
            assertEquals(Date().daysAgo(30).formatToYYYYmmDD(), expectedTimeWindowCaptor.firstValue)
        }

    @Test
    fun `given COD enabled and more than 10 IPP transactions done in last 30 days, then should detect a ninja`() =
        testBlocking {
            // given
            whenever(shouldShowFeedbackBanner()).thenReturn(true)
            whenever(getActivePaymentsPlugin())
                .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)

            val fakeNinjaTransactionsSummary = WCPaymentTransactionsSummaryResult(11, "", 0, 0, 0, null, null)
            whenever(ippStore.fetchTransactionsSummary(any(), any(), any()))
                .thenReturn(WooPayload(fakeNinjaTransactionsSummary))

            // when
            val result = sut()

            // then
            assertEquals("https://automattic.survey.fm/woo-app-–-ipp-survey-for-power-users", result.url)
            assertEquals(R.string.feedback_banner_ipp_message_ninja, result.message)
            assertEquals(R.string.feedback_banner_ipp_title_ninja, result.title)
            assertEquals("ipp_power_user", result.campaignName)
        }

    @Test
    fun `given COD is enabled and payments plugin is Stripe, then should detect newbie`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(true)
        whenever(getActivePaymentsPlugin()).thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.STRIPE)

        // when
        val result = sut()

        // then
        assertEquals(R.string.feedback_banner_ipp_message_newbie, result.message)
        assertEquals(R.string.feedback_banner_ipp_title_newbie, result.title)
        assertEquals("https://automattic.survey.fm/woo-app-–-cod-survey", result.url)
    }

    @Test
    fun `given banner should be displayed, then should analyze IPP stats from the correct time window`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(true)
        whenever(getActivePaymentsPlugin())
            .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)

        val expectedTimeWindowCaptor = ArgumentCaptor.forClass(String::class.java)

        // when
        sut()
        verify(ippStore).fetchTransactionsSummary(
            any(),
            any(),
            expectedTimeWindowCaptor.capture()
        )

        // then
        val timeWindowLengthDays = 30
        val desiredTimeWindowStart = Date().daysAgo(timeWindowLengthDays).formatToYYYYmmDD()
        assertEquals(desiredTimeWindowStart, expectedTimeWindowCaptor.value)
    }

    @Test
    fun `given COD enabled and endpoint returns error, then should detect newbie`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(true)
        whenever(getActivePaymentsPlugin())
            .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)

        val error = WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NO_CONNECTION, "error")
        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())).thenReturn(WooPayload(error))

        // when
        val result = sut()

        // then
        assertEquals(R.string.feedback_banner_ipp_message_newbie, result.message)
        assertEquals(R.string.feedback_banner_ipp_title_newbie, result.title)
        assertEquals("https://automattic.survey.fm/woo-app-–-cod-survey", result.url)
    }

    @Test
    fun `given endpoint returns error, when use case invoked, then should log error`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(true)
        whenever(getActivePaymentsPlugin())
            .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)

        val errorMessage = "error"
        val error = WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NO_CONNECTION, errorMessage)
        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())).thenReturn(WooPayload(error))

        // when
        sut()

        // then
        verify(logger).e(AppLog.T.API, "Error fetching transactions summary: $error")
    }

    @Test
    fun `given endpoint returns null response, then should detect newbie`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(true)
        whenever(getActivePaymentsPlugin())
            .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)

        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())).thenReturn(WooPayload(null))

        // when
        val result = sut()

        // then
        assertEquals(R.string.feedback_banner_ipp_message_newbie, result.message)
        assertEquals(R.string.feedback_banner_ipp_title_newbie, result.title)
        assertEquals("https://automattic.survey.fm/woo-app-–-cod-survey", result.url)
    }

    @Test
    fun `given successful endpoint response, when transactions count is negative, then should throw exception `() =
        testBlocking {
            // given
            whenever(shouldShowFeedbackBanner()).thenReturn(true)
            whenever(getActivePaymentsPlugin())
                .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)

            val fakeTransactionsSummary = WCPaymentTransactionsSummaryResult(-1, "", 0, 0, 0, null, null)
            whenever(
                ippStore.fetchTransactionsSummary(
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(WooPayload(fakeTransactionsSummary))

            // then
            assertFailsWith(IllegalStateException::class) {
                runBlocking { sut() }
            }

            Unit
        }
}
