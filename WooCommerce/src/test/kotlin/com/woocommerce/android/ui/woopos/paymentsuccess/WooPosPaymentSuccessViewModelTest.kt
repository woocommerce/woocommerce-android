package com.woocommerce.android.ui.woopos.paymentsuccess

import com.woocommerce.android.ui.woopos.cardpayment.WooPosCardPaymentAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
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

    private val analyticsTracker: WooPosCardPaymentAnalyticsTracker = mock()

    private val viewModel = WooPosPaymentSuccessViewModel(
        analyticsTracker = analyticsTracker,
    )

    @Test
    fun `when onEmailReceiptClicked, then trackEmailReceiptTapped is called`() = runTest {
        // WHEN
        viewModel.onEmailReceiptClicked()

        // THEN
        verify(analyticsTracker).trackEmailReceiptTapped()
    }
}
