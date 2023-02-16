package com.woocommerce.android.ui.login.storecreation.iap

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iap.pub.PurchaseWpComPlanSupportChecker
import com.woocommerce.android.iap.pub.model.IAPError
import com.woocommerce.android.iap.pub.model.IAPSupportedResult
import com.woocommerce.android.iap.pub.model.PurchaseStatus
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
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
    private val iapManager: PurchaseWPComPlanActions = mock()

    private lateinit var viewModel: IapEligibilityViewModel

    @Before
    fun setup() {
        viewModel = IapEligibilityViewModel(
            savedState,
            planSupportChecker,
            analyticsTrackerWrapper,
            isIAPEnabled,
            iapManager
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
            givenWpPlanPurchasedReturns(PurchaseStatus.NOT_PURCHASED)

            viewModel.checkIapEligibility()
            val event = viewModel.event.captureValues().last()

            verify(planSupportChecker).isIAPSupported()
            verify(iapManager).isWPComPlanPurchased()
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
            assertThat((event as MultiLiveEvent.Event.ShowDialog).messageId)
                .isEqualTo(R.string.store_creation_iap_eligibility_check_error_not_available_for_country)
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
            assertThat((event as MultiLiveEvent.Event.ShowDialog).messageId)
                .isEqualTo(R.string.store_creation_iap_eligibility_check_generic_error)
        }

    @Test
    fun `given a purchased subscription not acknowledged, when checking IAP eligibility, then show not acknowledged error`() =
        testBlocking {
            whenever(isIAPEnabled.invoke()).thenReturn(true)
            givenUserIsEligibleForIAP(isEligible = true)
            givenWpPlanPurchasedReturns(PurchaseStatus.PURCHASED)

            viewModel.checkIapEligibility()
            val event = viewModel.event.captureValues().last()
            val isLoadingState = viewModel.isCheckingIapEligibility.captureValues().last()

            assertFalse(isLoadingState)
            assertThat(event).isExactlyInstanceOf(MultiLiveEvent.Event.ShowDialog::class.java)
            assertThat((event as MultiLiveEvent.Event.ShowDialog).messageId)
                .isEqualTo(R.string.store_creation_iap_eligibility_existing_purchase_not_acknowledged)
        }

    @Test
    fun `given an acknowledged purchased subscription, when checking IAP eligibility, then show existing subscription error`() =
        testBlocking {
            whenever(isIAPEnabled.invoke()).thenReturn(true)
            givenUserIsEligibleForIAP(isEligible = true)
            givenWpPlanPurchasedReturns(PurchaseStatus.PURCHASED_AND_ACKNOWLEDGED)

            viewModel.checkIapEligibility()
            val event = viewModel.event.captureValues().last()
            val isLoadingState = viewModel.isCheckingIapEligibility.captureValues().last()

            assertFalse(isLoadingState)
            assertThat(event).isExactlyInstanceOf(MultiLiveEvent.Event.ShowDialog::class.java)
            assertThat((event as MultiLiveEvent.Event.ShowDialog).messageId)
                .isEqualTo(R.string.store_creation_iap_eligibility_check_error_existing_subscription)
        }

    @Test
    fun `given an error, when checking for active subscriptions, then show error dialog and hide loading`() =
        testBlocking {
            whenever(isIAPEnabled.invoke()).thenReturn(true)
            givenUserIsEligibleForIAP(isEligible = true)
            givenWpPlanPurchasedReturnsError(IAPError.Billing.ServiceDisconnected(debugMessage = ""))

            viewModel.checkIapEligibility()
            val event = viewModel.event.captureValues().last()
            val isLoadingState = viewModel.isCheckingIapEligibility.captureValues().last()

            assertFalse(isLoadingState)
            assertThat(event).isExactlyInstanceOf(MultiLiveEvent.Event.ShowDialog::class.java)
            assertThat((event as MultiLiveEvent.Event.ShowDialog).messageId)
                .isEqualTo(R.string.store_creation_iap_eligibility_check_generic_error)
        }

    private suspend fun givenUserIsEligibleForIAP(isEligible: Boolean) {
        whenever(planSupportChecker.isIAPSupported()).thenReturn(
            IAPSupportedResult.Success(isSupported = isEligible)
        )
    }

    private suspend fun givenWpPlanPurchasedReturns(purchasedStatus: PurchaseStatus) {
        whenever(iapManager.isWPComPlanPurchased())
            .thenReturn(WPComIsPurchasedResult.Success(purchasedStatus))
    }

    private suspend fun givenWpPlanPurchasedReturnsError(error: IAPError) {
        whenever(iapManager.isWPComPlanPurchased())
            .thenReturn(WPComIsPurchasedResult.Error(error))
    }
}
