package com.woocommerce.android.ui.payments.feedback.ipp

import com.woocommerce.android.extensions.daysAgo
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import com.woocommerce.android.ui.payments.feedback.ipp.GetIPPFeedbackBannerData.Companion.BANNER_MESSAGE_BEGINNER
import com.woocommerce.android.ui.payments.feedback.ipp.GetIPPFeedbackBannerData.Companion.BANNER_MESSAGE_NEWBIE
import com.woocommerce.android.ui.payments.feedback.ipp.GetIPPFeedbackBannerData.Companion.BANNER_MESSAGE_NINJA
import com.woocommerce.android.ui.payments.feedback.ipp.GetIPPFeedbackBannerData.Companion.STATS_TIME_WINDOW_LENGTH_DAYS
import com.woocommerce.android.ui.payments.feedback.ipp.GetIPPFeedbackBannerData.Companion.SURVEY_URL_IPP_BEGINNER
import com.woocommerce.android.ui.payments.feedback.ipp.GetIPPFeedbackBannerData.Companion.SURVEY_URL_IPP_NEWBIE
import com.woocommerce.android.ui.payments.feedback.ipp.GetIPPFeedbackBannerData.Companion.SURVEY_URL_IPP_NINJA
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
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
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class GetIPPFeedbackBannerDataTest : BaseUnitTest() {
    private val shouldShowFeedbackBanner: ShouldShowFeedbackBanner = mock()
    private val ippStore: WCInPersonPaymentsStore = mock()
    private val siteModel: SiteModel = mock()
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin = mock()

    private val sut = GetIPPFeedbackBannerData(
        shouldShowFeedbackBanner = shouldShowFeedbackBanner,
        ippStore = ippStore,
        siteModel = siteModel,
        getActivePaymentsPlugin = getActivePaymentsPlugin,
    )

    @Before
    fun setup() {
        val fakeSummary = WCPaymentTransactionsSummaryResult(99, "", 0, 0, 0, null, null)
        runBlocking {
            whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn WooPayload(fakeSummary)
        }
    }

    @Test
    fun `given banner should not be displayed, then should throw exception`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner()) doReturn false

        // then
        assertFailsWith(IllegalStateException::class) {
            runBlocking { sut() }
        }

        Unit
    }

    @Test
    fun `given no active payments plugin found, then should throw exception`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner()) doReturn true
        whenever(getActivePaymentsPlugin()) doReturn null

        // then
        assertFailsWith(IllegalStateException::class) {
            runBlocking { sut() }
        }

        Unit
    }

    @Test
    fun `given banner should be displayed and user is a newbie, then should return correct url`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner()) doReturn true

        whenever(getActivePaymentsPlugin()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        val fakeNewbieTransactionsSummary = WCPaymentTransactionsSummaryResult(0, "", 0, 0, 0, null, null)
        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn WooPayload(
            fakeNewbieTransactionsSummary
        )

        // when
        val result = sut()

        // then
        assertEquals(SURVEY_URL_IPP_NEWBIE, result.url)
    }

    @Test
    fun `given banner should be displayed and user is a beginner, then should return correct url`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner()) doReturn true
        whenever(getActivePaymentsPlugin()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        val fakeBeginnerTransactionsSummary = WCPaymentTransactionsSummaryResult(2, "", 0, 0, 0, null, null)
        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn WooPayload(
            fakeBeginnerTransactionsSummary
        )

        // when
        val result = sut()

        // then
        assertEquals(SURVEY_URL_IPP_BEGINNER, result.url)
    }

    @Test
    fun `given banner should be displayed and user is a ninja, then should return correct url`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner()) doReturn true
        whenever(getActivePaymentsPlugin()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        val fakeNinjaTransactionsSummary = WCPaymentTransactionsSummaryResult(11, "", 0, 0, 0, null, null)
        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn
            WooPayload(fakeNinjaTransactionsSummary)

        // when
        val result = sut()

        // then
        assertEquals(SURVEY_URL_IPP_NINJA, result.url)
    }

    @Test
    fun `given banner should be displayed and user is a newbie, then should display correct message`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner()) doReturn true
        whenever(getActivePaymentsPlugin()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        val fakeNinjaTransactionsSummary = WCPaymentTransactionsSummaryResult(0, "", 0, 0, 0, null, null)
        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn
            WooPayload(fakeNinjaTransactionsSummary)

        // when
        val result = sut()

        // then
        assertEquals(BANNER_MESSAGE_NEWBIE, result.message)
    }

    @Test
    fun `given banner should be displayed and user is a beginner, then should display correct message`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner()) doReturn true
        whenever(getActivePaymentsPlugin()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        val fakeNinjaTransactionsSummary = WCPaymentTransactionsSummaryResult(1, "", 0, 0, 0, null, null)
        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn
            WooPayload(fakeNinjaTransactionsSummary)

        // when
        val result = sut()

        // then
        assertEquals(BANNER_MESSAGE_BEGINNER, result.message)
    }

    @Test
    fun `given banner should be displayed and user is a ninja, then should display correct message`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner()) doReturn true
        whenever(getActivePaymentsPlugin()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        val fakeNinjaTransactionsSummary = WCPaymentTransactionsSummaryResult(11, "", 0, 0, 0, null, null)
        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn
            WooPayload(fakeNinjaTransactionsSummary)

        // when
        val result = sut()

        // then
        assertEquals(BANNER_MESSAGE_NINJA, result.message)
    }

    @Test
    fun `given banner should be displayed, then should analyze IPP stats from the correct time window`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner()) doReturn true
        whenever(getActivePaymentsPlugin()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        val expectedTimeWindowCaptor = ArgumentCaptor.forClass(String::class.java)

        // when
        sut()
        verify(ippStore).fetchTransactionsSummary(
            any(),
            any(),
            expectedTimeWindowCaptor.capture()
        )

        // then
        val desiredTimeWindowStart = Date().daysAgo(STATS_TIME_WINDOW_LENGTH_DAYS).formatToYYYYmmDD()
        assertEquals(desiredTimeWindowStart, expectedTimeWindowCaptor.value)
    }

    @Test
    fun `given endpoint returns error, then should throw error`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner()) doReturn true
        whenever(getActivePaymentsPlugin()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        val error = WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NO_CONNECTION, "error")
        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn WooPayload(error)

        // then
        assertFailsWith(IllegalStateException::class) {
            runBlocking { sut() }
        }

        Unit
    }

    @Test
    fun `given endpoint returns null response, then should throw exception`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner()) doReturn true
        whenever(getActivePaymentsPlugin()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn WooPayload(null)

        // then
        assertFailsWith(IllegalStateException::class) {
            runBlocking { sut() }
        }

        Unit
    }

    @Test
    fun `given successful endpoint response, when transactions count is negative, then should throw exception `() =
        runBlocking {
            // given
            whenever(shouldShowFeedbackBanner()) doReturn true
            whenever(getActivePaymentsPlugin()) doReturn
                WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

            val fakeTransactionsSummary = WCPaymentTransactionsSummaryResult(-1, "", 0, 0, 0, null, null)
            whenever(
                ippStore.fetchTransactionsSummary(
                    any(),
                    any(),
                    any()
                )
            ) doReturn WooPayload(fakeTransactionsSummary)

            // then
            assertFailsWith(IllegalStateException::class) {
                runBlocking { sut() }
            }

            Unit
        }
}
