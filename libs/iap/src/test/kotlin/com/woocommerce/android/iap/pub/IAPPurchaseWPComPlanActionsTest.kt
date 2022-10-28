package com.woocommerce.android.iap.pub

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsResult
import com.woocommerce.android.iap.internal.core.IAPBillingClientProvider
import com.woocommerce.android.iap.internal.core.IAPBillingClientStateHandler
import com.woocommerce.android.iap.internal.core.IAPBillingClientWrapper
import com.woocommerce.android.iap.internal.core.IAPInMapper
import com.woocommerce.android.iap.internal.core.IAPManager
import com.woocommerce.android.iap.internal.core.IAPOutMapper
import com.woocommerce.android.iap.internal.core.IAPPurchasesUpdatedListener
import com.woocommerce.android.iap.internal.model.IAPSupportedResult
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPI
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl
import com.woocommerce.android.iap.pub.model.IAPError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class IAPPurchaseWPComPlanActionsTest {
    private val logWrapperMock: IAPLogWrapper = mock()
    private val mobilePayAPIMock: IAPMobilePayAPI = mock()
    private val billingClientMock: IAPBillingClientWrapper = mock()

    private lateinit var sut: IAPPurchaseWPComPlanActionsImpl

    @Before
    fun setup() {
        val purchasesUpdatedListener = IAPPurchasesUpdatedListener(logWrapperMock)
        val billingClientProvider: IAPBillingClientProvider = mock {
            on { provideBillingClient() }.thenReturn(billingClientMock)
        }

        val iapBillingClientStateHandler = IAPBillingClientStateHandler(
            billingClientProvider,
            logWrapperMock
        )
        setupBillingClientToBeConnected()
        sut = IAPPurchaseWPComPlanActionsImpl(
            iapMobilePayAPI = mobilePayAPIMock,
            iapManager = buildIapManager(iapBillingClientStateHandler, purchasesUpdatedListener)
        )
    }

    //region Tests
    @Test
    fun `given product in NON USD currency, when iap support checked, then success with false returned`() = runTest {
        // GIVEN
        val productDetails = buildProductDetails("AED")
        whenever(billingClientMock.queryProductDetails(any())).thenReturn(
            ProductDetailsResult(
                buildBillingResult(BillingClient.BillingResponseCode.OK),
                listOf(productDetails)
            )
        )

        // WHEN
        val result = sut.isIAPSupported()

        // THEN
        assertThat((result as IAPSupportedResult.Success).isSupported).isFalse()
    }

    @Test
    fun `given product USD currency, when iap support checked, then success with true returned`() = runTest {
        // GIVEN
        val productDetails = buildProductDetails("USD")
        whenever(billingClientMock.queryProductDetails(any())).thenReturn(
            ProductDetailsResult(
                buildBillingResult(BillingClient.BillingResponseCode.OK),
                listOf(productDetails)
            )
        )

        // WHEN
        val result = sut.isIAPSupported()

        // THEN
        assertThat((result as IAPSupportedResult.Success).isSupported).isTrue()
    }

    @Test
    fun `given product fetching error, when iap support checked, then error returned`() = runTest {
        // GIVEN
        val productDetails = buildProductDetails("USD")
        val responseCode = BillingClient.BillingResponseCode.DEVELOPER_ERROR
        val debugMessage = "debug message"
        whenever(billingClientMock.queryProductDetails(any())).thenReturn(
            ProductDetailsResult(
                buildBillingResult(responseCode, debugMessage),
                listOf(productDetails)
            )
        )

        // WHEN
        val result = sut.isIAPSupported()

        // THEN
        assertThat((result as IAPSupportedResult.Error).errorType).isInstanceOf(
            IAPError.Billing.DeveloperError::class.java
        )
        assertThat((result.errorType as IAPError.Billing).debugMessage).isEqualTo(debugMessage)
    }
    //endregion

    private fun setupBillingClientToBeConnected() {
        whenever(billingClientMock.startConnection(any())).thenAnswer {
            val listener = it.arguments[0] as BillingClientStateListener
            listener.onBillingSetupFinished(BillingResult())
            whenever(billingClientMock.isReady).thenReturn(true)
        }
    }

    private fun buildBillingResult(
        @BillingClient.BillingResponseCode responseCode: Int,
        debugMessage: String = ""
    ) = BillingResult
        .newBuilder()
        .setResponseCode(responseCode)
        .setDebugMessage(debugMessage)
        .build()

    private fun buildIapManager(
        iapBillingClientStateHandler: IAPBillingClientStateHandler,
        purchasesUpdatedListener: IAPPurchasesUpdatedListener
    ): IAPManager {
        val outMapper = IAPOutMapper()
        val inMapper = IAPInMapper()
        return IAPManager(
            iapBillingClientStateHandler,
            outMapper,
            inMapper,
            purchasesUpdatedListener,
            logWrapperMock,
        )
    }
}
