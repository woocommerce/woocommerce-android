package com.woocommerce.android.iap.pub

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.woocommerce.android.iap.internal.core.IAPBillingClientProvider
import com.woocommerce.android.iap.internal.core.IAPBillingClientStateHandler
import com.woocommerce.android.iap.internal.core.IAPBillingClientWrapper
import com.woocommerce.android.iap.internal.core.IAPInMapper
import com.woocommerce.android.iap.internal.core.IAPManager
import com.woocommerce.android.iap.internal.core.IAPOutMapper
import com.woocommerce.android.iap.internal.core.IAPPurchasesUpdatedListener
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWpComPlanSupportCheckerImpl
import com.woocommerce.android.iap.pub.model.IAPError
import com.woocommerce.android.iap.pub.model.IAPSupportedResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class IAPPurchaseWpComPlanSupportCheckerTest {
    private val billingClientMock: IAPBillingClientWrapper = mock()
    private val testPreparationHelper = IAPTestPreparationHelper(billingClientMock)

    private lateinit var sut: IAPPurchaseWpComPlanSupportCheckerImpl

    @Before
    fun setup() {
        setupSut(buildBillingResult(BillingClient.BillingResponseCode.OK))
    }

    @Test
    fun `given product in NON USD currency, when iap support checked, then success with false returned`() = runTest {
        // GIVEN
        testPreparationHelper.setupQueryProductDetails(
            responseCode = BillingClient.BillingResponseCode.OK,
            currency = "AED"
        )

        // WHEN
        val result = sut.isIAPSupported()

        // THEN
        assertThat((result as IAPSupportedResult.Success).isSupported).isFalse()
    }

    @Test
    fun `given product USD currency, when iap support checked, then success with true returned`() = runTest {
        // GIVEN
        testPreparationHelper.setupQueryProductDetails(responseCode = BillingClient.BillingResponseCode.OK)

        // WHEN
        val result = sut.isIAPSupported()

        // THEN
        assertThat((result as IAPSupportedResult.Success).isSupported).isTrue()
    }

    @Test
    fun `given product fetching error, when iap support checked, then error returned`() = runTest {
        // GIVEN
        val responseCode = BillingClient.BillingResponseCode.DEVELOPER_ERROR
        val debugMessage = "debug message"

        testPreparationHelper.setupQueryProductDetails(
            responseCode = responseCode,
            debugMessage = debugMessage
        )

        // WHEN
        val result = sut.isIAPSupported()

        // THEN
        assertThat((result as IAPSupportedResult.Error).errorType).isInstanceOf(
            IAPError.Billing.DeveloperError::class.java
        )
        assertThat((result.errorType as IAPError.Billing).debugMessage).isEqualTo(debugMessage)
    }

    @Test
    fun `given connection to service failed, when iap support checked, then error returned`() = runTest {
        // GIVEN
        val debugMessage = "debug message"
        setupSut(buildBillingResult(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, debugMessage))

        // WHEN
        val result = sut.isIAPSupported()

        // THEN
        assertThat((result as IAPSupportedResult.Error).errorType).isInstanceOf(
            IAPError.Billing.ServiceUnavailable::class.java
        )
        assertThat((result.errorType as IAPError.Billing).debugMessage).isEqualTo(debugMessage)
    }

    private fun setupSut(connectionResult: BillingResult) {
        val purchasesUpdatedListener = IAPPurchasesUpdatedListener(mock())
        val billingClientProvider: IAPBillingClientProvider = mock {
            on { provideBillingClient() }.thenReturn(billingClientMock)
        }
        val outMapper = IAPOutMapper()
        testPreparationHelper.setupBillingClient(connectionResult)
        val iapBillingClientStateHandler = IAPBillingClientStateHandler(
            billingClientProvider,
            outMapper,
            mock(),
        )
        val iapManager = IAPManager(
            iapBillingClientStateHandler,
            outMapper,
            IAPInMapper(),
            purchasesUpdatedListener,
            mock(),
            mock(),
            mock(),
        )
        sut = IAPPurchaseWpComPlanSupportCheckerImpl(
            iapManager = iapManager,
            iapProduct = iapProduct,
        )
    }
}
