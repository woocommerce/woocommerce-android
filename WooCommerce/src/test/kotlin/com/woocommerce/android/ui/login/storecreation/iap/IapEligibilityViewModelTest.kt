package com.woocommerce.android.ui.login.storecreation.iap

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.iap.pub.PurchaseWpComPlanSupportChecker
import com.woocommerce.android.iap.pub.model.IAPError
import com.woocommerce.android.iap.pub.model.IAPSupportedResult
import com.woocommerce.android.ui.login.storecreation.iap.IapEligibilityViewModel.IapEligibilityEvent.NavigateToNextStep
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse

@ExperimentalCoroutinesApi
class IapEligibilityViewModelTest : BaseUnitTest() {
    private val savedState = SavedStateHandle()
    private val planSupportChecker: PurchaseWpComPlanSupportChecker = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val isIAPEnabled: IsIAPEnabled = mock()

    private lateinit var viewModel: IapEligibilityViewModel

    @Before
    fun setup() {
        viewModel = IapEligibilityViewModel(
            savedState,
            planSupportChecker,
            analyticsTrackerWrapper,
            isIAPEnabled
        )
    }

    @Test
    fun `given IAP is disabled, when checking for IAP eligibility, then no request to IAP service is done`() =
        testBlocking {
            whenever(isIAPEnabled.invoke()).thenReturn(false)

            viewModel.checkIapEligibility()

            verify(planSupportChecker, never()).isIAPSupported()
        }

    @Test
    fun `given IAP is disabled, then should navigate to the next of the flow`() =
        testBlocking {
            whenever(isIAPEnabled.invoke()).thenReturn(false)

            viewModel.checkIapEligibility()
            val event = viewModel.event.captureValues().last()

            assertThat(event).isEqualTo(NavigateToNextStep)
        }

    @Test
    fun `given user is eligible for IAP, when checking IAP eligibility, then navigate to the next step of the flow`() =
        testBlocking {
            whenever(isIAPEnabled.invoke()).thenReturn(true)
            givenUserIsEligibleForIAP(isEligible = true)

            viewModel.checkIapEligibility()
            val event = viewModel.event.captureValues().last()

            verify(planSupportChecker).isIAPSupported()
            assertThat(event).isEqualTo(NavigateToNextStep)
        }

    @Test
    fun `given user is not eligible for IAP, when checking IAP eligibility, then show error dialog and hide loading`() =
        testBlocking {
            whenever(isIAPEnabled.invoke()).thenReturn(true)
            givenUserIsEligibleForIAP(isEligible = false)

            viewModel.checkIapEligibility()
            val event = viewModel.event.captureValues().last()

            verify(planSupportChecker).isIAPSupported()
            assertThat(event).isExactlyInstanceOf(MultiLiveEvent.Event.ShowDialog::class.java)
        }

    @Test
    fun `given an error when checking for IAP eligibility, then show error dialog and hide loading`() =
        testBlocking {
            whenever(isIAPEnabled.invoke()).thenReturn(true)
            whenever(planSupportChecker.isIAPSupported()).thenReturn(
                IAPSupportedResult.Error(IAPError.Billing.ServiceDisconnected("An error"))
            )

            viewModel.checkIapEligibility()
            val event = viewModel.event.captureValues().last()
            val isLoadingState = viewModel.isCheckingIapEligibility.captureValues().last()

            verify(planSupportChecker).isIAPSupported()
            assertFalse(isLoadingState)
            assertThat(event).isExactlyInstanceOf(MultiLiveEvent.Event.ShowDialog::class.java)
        }

    private suspend fun givenUserIsEligibleForIAP(isEligible: Boolean) {
        whenever(planSupportChecker.isIAPSupported()).thenReturn(
            IAPSupportedResult.Success(isSupported = isEligible)
        )
    }
}
