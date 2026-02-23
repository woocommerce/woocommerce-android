package com.woocommerce.android.ui.woopos.paymentsuccess

import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.EmailReceiptTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPaymentSuccessViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val analyticsTracker: WooPosAnalyticsTracker = mock()

    private val viewModel = WooPosPaymentSuccessViewModel(
        analyticsTracker = analyticsTracker,
    )

    @Test
    fun `when onEmailReceiptClicked, then EmailReceiptTapped is tracked`() = runTest {
        // WHEN
        viewModel.onEmailReceiptClicked()

        // THEN
        verify(analyticsTracker).track(EmailReceiptTapped)
    }
}
