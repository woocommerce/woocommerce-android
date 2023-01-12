package com.woocommerce.android.ui.payments.feedback.ipp

import com.woocommerce.android.extensions.daysAgo
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import com.woocommerce.android.ui.payments.feedback.ipp.GetIPPFeedbackSurveyUrl.Companion.STATS_TIME_WINDOW_LENGTH_DAYS
import com.woocommerce.android.ui.payments.feedback.ipp.GetIPPFeedbackSurveyUrl.Companion.SURVEY_URL_IPP_BEGINNER
import com.woocommerce.android.ui.payments.feedback.ipp.GetIPPFeedbackSurveyUrl.Companion.SURVEY_URL_IPP_NEWBIE
import com.woocommerce.android.ui.payments.feedback.ipp.GetIPPFeedbackSurveyUrl.Companion.SURVEY_URL_IPP_NINJA
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GetIPPFeedbackSurveyUrlTest : BaseUnitTest() {
    private val shouldShowFeedbackBanner: ShouldShowFeedbackBanner = mock()
    private val ippStore: WCInPersonPaymentsStore = mock()
    private val siteModel: SiteModel = mock()
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin = mock()

    private val sut = GetIPPFeedbackSurveyUrl(
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
    fun `given banner should not be displayed, should throw exception`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner.invoke()) doReturn false

        // then
        assertFailsWith(IllegalStateException::class) {
            runBlocking { sut.invoke() }
        }

        Unit
    }

    @Test
    fun `given no active payments plugin found, should throw exception`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner.invoke()) doReturn true
        whenever(getActivePaymentsPlugin.invoke()) doReturn null

        // then
        assertFailsWith(IllegalStateException::class) {
            runBlocking { sut.invoke() }
        }

        Unit
    }

    @Test
    fun `given banner should be displayed and user is a newbie, should return correct url`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner.invoke()) doReturn true

        whenever(getActivePaymentsPlugin.invoke()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        val fakeNewbieTransactionsSummary = WCPaymentTransactionsSummaryResult(0, "", 0, 0, 0, null, null)
        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn WooPayload(
            fakeNewbieTransactionsSummary
        )

        // when
        val result = sut.invoke()

        // then
        assertEquals(SURVEY_URL_IPP_NEWBIE, result)
    }

    @Test
    fun `given banner should be displayed and user is a beginner, should return correct url`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner.invoke()) doReturn true
        whenever(getActivePaymentsPlugin.invoke()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        val fakeBeginnerTransactionsSummary = WCPaymentTransactionsSummaryResult(2, "", 0, 0, 0, null, null)
        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn WooPayload(
            fakeBeginnerTransactionsSummary
        )

        // when
        val result = sut.invoke()

        // then
        assertEquals(SURVEY_URL_IPP_BEGINNER, result)
    }

    @Test
    fun `given banner should be displayed and user is a ninja, should return correct url`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner.invoke()) doReturn true
        whenever(getActivePaymentsPlugin.invoke()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        val fakeNinjaTransactionsSummary = WCPaymentTransactionsSummaryResult(11, "", 0, 0, 0, null, null)
        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn
            WooPayload(fakeNinjaTransactionsSummary)

        // when
        val result = sut.invoke()

        // then
        assertEquals(SURVEY_URL_IPP_NINJA, result)
    }

    @Test
    fun `given banner should be displayed, should analyze IPP stats from the correct time window`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner.invoke()) doReturn true
        whenever(getActivePaymentsPlugin.invoke()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        val expectedTimeWindowCaptor = ArgumentCaptor.forClass(String::class.java)

        // when
        sut.invoke()
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
    fun `given endpoint returns error, should return null survey url`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner.invoke()) doReturn true
        whenever(getActivePaymentsPlugin.invoke()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        val error = WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NO_CONNECTION, "error")
        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn WooPayload(error)

        // when
        val result = sut.invoke()

        // then
        assertNull(result)
    }

    @Test
    fun `given endpoint returns null response, should return null survey url`() = runBlocking {
        // given
        whenever(shouldShowFeedbackBanner.invoke()) doReturn true
        whenever(getActivePaymentsPlugin.invoke()) doReturn
            WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS

        whenever(ippStore.fetchTransactionsSummary(any(), any(), any())) doReturn WooPayload(null)

        // when
        val result = sut.invoke()

        // then
        assertNull(result)
    }
}
