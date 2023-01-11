package com.woocommerce.android.ui.payments.feedback.ipp

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.daysAgo
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import com.woocommerce.android.ui.payments.cardreader.CashOnDeliverySettingsRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import java.util.Calendar
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShouldShowFeedbackBannerTest : BaseUnitTest() {
    private val appPrefs: AppPrefsWrapper = mock()
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin = mock()
    private val cashOnDeliverySettings: CashOnDeliverySettingsRepository = mock()

    private val sut = ShouldShowFeedbackBanner(
        prefs = appPrefs,
        getActivePaymentsPlugin = getActivePaymentsPlugin,
        cashOnDeliverySettings = cashOnDeliverySettings,
    )

    @Before
    fun setup() = runBlocking {
        whenever(cashOnDeliverySettings.isCashOnDeliveryEnabled()) doReturn (true)
        whenever(appPrefs.isIPPFeedbackSurveyCompleted()) doReturn (false)
        whenever(appPrefs.isIPPFeedbackBannerDismissedForever()) doReturn (false)
        whenever(getActivePaymentsPlugin.invoke()) doReturn
            (WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS)
        Unit
    }

    @Test
    fun `given COD is not enabled, then banner should not be shown`() = runBlocking {
        // given
        whenever(cashOnDeliverySettings.isCashOnDeliveryEnabled()).thenReturn(false)

        // when
        val result = sut.invoke()

        // then
        assertFalse(result)
    }

    @Test
    fun `given Stripe plugin is used, then banner should not be shown`() = runBlocking {
        // given
        whenever(getActivePaymentsPlugin.invoke()).thenReturn(WCInPersonPaymentsStore.InPersonPaymentsPluginType.STRIPE)

        // when
        val result = sut.invoke()

        // then
        assertFalse(result)
    }

    @Test
    fun `given survey has been completed, then banner should not be shown`() = runBlocking {
        // given
        whenever(appPrefs.isIPPFeedbackSurveyCompleted()).thenReturn(true)

        // when
        val result = sut.invoke()

        // then
        assertFalse(result)
    }

    @Test
    fun `given survey has been dismissed forever, then banner should not be shown`() = runBlocking {
        // given
        whenever(appPrefs.isIPPFeedbackBannerDismissedForever()).thenReturn(true)

        // when
        val result = sut.invoke()

        // then
        assertFalse(result)
    }

    @Test
    fun `given survey has been dismissed less than 7 days ago, then banner should not be shown`() = runBlocking {
        // given
        val now = Calendar.getInstance().time
        val sixDaysAgo = now.daysAgo(6)
        whenever(appPrefs.getIPPFeedbackBannerLastDismissed()).thenReturn(sixDaysAgo.time)

        // when
        val result = sut.invoke()

        // then
        assertFalse(result)
    }

    @Test
    fun `given survey has been dismissed more than or equal to 7 days ago, then banner should be shown`() =
        runBlocking {
            // given
            val now = Calendar.getInstance().time
            val eightDaysAgo = now.daysAgo(7)
            whenever(appPrefs.getIPPFeedbackBannerLastDismissed()).thenReturn(eightDaysAgo.time)

            // when
            val result = sut.invoke()

            // then
            assertTrue(result)
        }

    @Test
    fun `given survey has not been dismissed, then banner should be shown`() = runBlocking {
        // given
        whenever(appPrefs.getIPPFeedbackBannerLastDismissed()).thenReturn(-1L)

        // when
        val result = sut.invoke()

        // then
        assertTrue(result)
    }
}
