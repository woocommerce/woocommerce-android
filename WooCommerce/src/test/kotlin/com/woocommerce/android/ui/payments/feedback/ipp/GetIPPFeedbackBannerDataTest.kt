package com.woocommerce.android.ui.payments.feedback.ipp

import com.woocommerce.android.R
import com.woocommerce.android.extensions.WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import com.woocommerce.android.ui.payments.cardreader.CashOnDeliverySettingsRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GetIPPFeedbackBannerDataTest : BaseUnitTest() {
    private val shouldShowFeedbackBanner: ShouldShowFeedbackBanner = mock()
    private val orderStore: WCOrderStore = mock()
    private val cashOnDeliverySettings: CashOnDeliverySettingsRepository = mock()
    private val siteModel: SiteModel = mock()
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin = mock()
    private val logger: AppLogWrapper = mock()

    private val sut = GetIPPFeedbackBannerData(
        shouldShowFeedbackBanner = shouldShowFeedbackBanner,
        orderStore = orderStore,
        cashOnDeliverySettings = cashOnDeliverySettings,
        siteModel = siteModel,
        getActivePaymentsPlugin = getActivePaymentsPlugin,
        logger = logger,
    )

    @Test
    fun `given banner should not be displayed, then should return null`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(false)

        // when
        val result = sut()

        // then
        assertNull(result)
    }

    @Test
    fun `given banner should not be shown, then log the error`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(false)

        // when
        sut()

        // then
        verify(logger).e(any(), any())
    }

    @Test
    fun `given banner should not be shown, then log the error with proper message`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(false)

        // when
        sut()

        // then
        verify(logger).e(AppLog.T.API, "GetIPPFeedbackBannerData should not be shown.")
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
            assertEquals("https://automattic.survey.fm/woo-app-–-cod-survey", result?.url)
            assertEquals(R.string.feedback_banner_ipp_title_newbie, result?.title)
            assertEquals(R.string.feedback_banner_ipp_message_newbie, result?.message)
            assertEquals("ipp_not_user", result?.campaignName)
        }

    @Test
    fun `given COD enabled and has 0 IPP transactions, then newbie user should be detected`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(true)
        whenever(getActivePaymentsPlugin())
            .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)
        whenever(cashOnDeliverySettings.isCashOnDeliveryEnabled()).thenReturn(true)
        whenever(orderStore.getOrdersForSite(siteModel)).thenReturn(
            listOf(OrderTestUtils.generateOrder(), OrderTestUtils.generateOrder())
        )

        // when
        val result = sut()

        // then
        assertEquals("https://automattic.survey.fm/woo-app-–-cod-survey", result?.url)
        assertEquals(R.string.feedback_banner_ipp_title_newbie, result?.title)
        assertEquals(R.string.feedback_banner_ipp_message_newbie, result?.message)
        assertEquals("ipp_not_user", result?.campaignName)
    }

    @Test
    fun `given COD disabled and has 0 IPP transactions, then return null`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(true)
        whenever(getActivePaymentsPlugin())
            .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)
        whenever(cashOnDeliverySettings.isCashOnDeliveryEnabled()).thenReturn(false)
        whenever(orderStore.getOrdersForSite(siteModel)).thenReturn(
            listOf(OrderTestUtils.generateOrder(), OrderTestUtils.generateOrder())
        )

        // when
        val result = sut()

        // then
        assertNull(result)
    }

    @Test
    fun `given IPP transactions in last 30 days and the transaction count is in beginners range (1 to 9), then beginner user should be detected`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(true)
        whenever(getActivePaymentsPlugin())
            .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)
        val orderList = mutableListOf<OrderEntity>()
        repeat(9) {
            orderList.add(
                OrderTestUtils.generateOrder(
                    metadata = "[{\"id\":22346,\"key\":\"receipt_url\",\"value\":\"\"}]",
                    paymentMethod = WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE,
                    datePaid = Date().formatToYYYYmmDD()
                )
            )
        }
        whenever(orderStore.getOrdersForSite(siteModel)).thenReturn(orderList)

        // when
        val result = sut()

        // then
        assertEquals("https://automattic.survey.fm/woo-app-–-ipp-first-transaction-survey", result?.url)
        assertEquals(R.string.feedback_banner_ipp_message_beginner, result?.message)
        assertEquals(R.string.feedback_banner_ipp_title_beginner, result?.title)
        assertEquals("ipp_new_user", result?.campaignName)
    }

    @Test
    fun `given has IPP transactions and the transaction count is greater than 9, then ninja user should be detected`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(true)
        whenever(getActivePaymentsPlugin())
            .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)
        val orderList = mutableListOf<OrderEntity>()
        repeat(10) {
            orderList.add(
                OrderTestUtils.generateOrder(
                    metadata = "[{\"id\":22346,\"key\":\"receipt_url\",\"value\":\"\"}]",
                    paymentMethod = WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE,
                    datePaid = Date().formatToYYYYmmDD()
                )
            )
        }
        whenever(orderStore.getOrdersForSite(siteModel)).thenReturn(orderList)

        // when
        val result = sut()

        // then
        assertEquals("https://automattic.survey.fm/woo-app-–-ipp-survey-for-power-users", result?.url)
        assertEquals(R.string.feedback_banner_ipp_message_ninja, result?.message)
        assertEquals(R.string.feedback_banner_ipp_title_ninja, result?.title)
        assertEquals("ipp_power_user", result?.campaignName)
    }

    @Test
    fun `given has IPP transactions older than 30 days and the transaction count is greater than 9, then ninja user should be detected`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(true)
        whenever(getActivePaymentsPlugin())
            .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)
        val orderList = mutableListOf<OrderEntity>()
        repeat(10) {
            orderList.add(
                OrderTestUtils.generateOrder(
                    metadata = "[{\"id\":22346,\"key\":\"receipt_url\",\"value\":\"\"}]",
                    paymentMethod = WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE,
                    datePaid = "2018-02-02T16:11:13Z"
                )
            )
        }
        whenever(orderStore.getOrdersForSite(siteModel)).thenReturn(orderList)

        // when
        val result = sut()

        // then
        assertEquals("https://automattic.survey.fm/woo-app-–-ipp-survey-for-power-users", result?.url)
        assertEquals(R.string.feedback_banner_ipp_message_ninja, result?.message)
        assertEquals(R.string.feedback_banner_ipp_title_ninja, result?.title)
        assertEquals("ipp_power_user", result?.campaignName)
    }

    @Test
    fun `given has IPP transactions older than 30 days and the transaction count is lesser than 10, then return null`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(true)
        whenever(getActivePaymentsPlugin())
            .thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)
        val orderList = mutableListOf<OrderEntity>()
        repeat(9) {
            orderList.add(
                OrderTestUtils.generateOrder(
                    metadata = "[{\"id\":22346,\"key\":\"receipt_url\",\"value\":\"\"}]",
                    paymentMethod = WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE,
                    datePaid = "2018-02-02T16:11:13Z"
                )
            )
        }
        whenever(orderStore.getOrdersForSite(siteModel)).thenReturn(orderList)

        // when
        val result = sut()

        // then
        assertNull(result)
    }

    @Test
    fun `given COD is enabled and payments plugin is Stripe, then should detect newbie`() = testBlocking {
        // given
        whenever(shouldShowFeedbackBanner()).thenReturn(true)
        whenever(getActivePaymentsPlugin()).thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.STRIPE)

        // when
        val result = sut()

        // then
        assertEquals(R.string.feedback_banner_ipp_message_newbie, result?.message)
        assertEquals(R.string.feedback_banner_ipp_title_newbie, result?.title)
        assertEquals("https://automattic.survey.fm/woo-app-–-cod-survey", result?.url)
    }
}
