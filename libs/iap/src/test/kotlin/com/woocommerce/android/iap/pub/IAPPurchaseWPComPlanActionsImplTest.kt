package com.woocommerce.android.iap.pub

import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsResult
import com.woocommerce.android.iap.internal.core.IAPBillingClientProvider
import com.woocommerce.android.iap.internal.core.IAPBillingClientWrapper
import com.woocommerce.android.iap.internal.core.IAPInMapper
import com.woocommerce.android.iap.internal.core.IAPLifecycleObserver
import com.woocommerce.android.iap.internal.core.IAPManager
import com.woocommerce.android.iap.internal.core.IAPOutMapper
import com.woocommerce.android.iap.internal.core.IAPPurchasesUpdatedListener
import com.woocommerce.android.iap.internal.model.IAPSupportedResult
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPI
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl
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
        val activityMock: AppCompatActivity = mock { on { lifecycle }.thenReturn(mock()) }

        val purchasesUpdatedListener = IAPPurchasesUpdatedListener(logWrapperMock)
        val billingClientProvider: IAPBillingClientProvider = mock {
            on { provideBillingClient(any(), any()) }.thenReturn(billingClientMock)
        }

        val iapLifecycleObserver = IAPLifecycleObserver(
            purchasesUpdatedListener,
            billingClientProvider,
            logWrapperMock
        )


        sut = IAPPurchaseWPComPlanActionsImpl(
            iapMobilePayAPI = mobilePayAPIMock,
            iapManager = buildIapManager(iapLifecycleObserver, purchasesUpdatedListener)
        )
        sut.initIAPWithNewActivity(activityMock)
        setupBillingClientToBeConnected()

        iapLifecycleObserver.onCreate(mock())
    }

    //region Tests
    @Test
    fun `given product in NON USD currency, when iap support checked, then success with false returned`() = runTest {
        // GIVEN
        val productDetails = buildProductDetails("AED")
        whenever(billingClientMock.queryProductDetails(any())).thenReturn(
            ProductDetailsResult(
                buildBillingResultWithOkResponse(),
                listOf(productDetails)
            )
        )

        // WHEN
        val result = sut.isIAPSupported()

        // THEN
        assertThat((result as IAPSupportedResult.Success).isSupported).isFalse()
    }
    //endregion

    private fun setupBillingClientToBeConnected() {
        whenever(billingClientMock.startConnection(any())).thenAnswer {
            val listener = it.arguments[0] as BillingClientStateListener
            listener.onBillingSetupFinished(BillingResult())
            whenever(billingClientMock.isReady).thenReturn(true)
        }
    }

    private fun buildBillingResultWithOkResponse() = BillingResult
        .newBuilder()
        .setResponseCode(
            BillingClient.BillingResponseCode.OK
        ).build()

    private fun buildIapManager(
        iapLifecycleObserver: IAPLifecycleObserver,
        purchasesUpdatedListener: IAPPurchasesUpdatedListener
    ): IAPManager {
        val outMapper = IAPOutMapper()
        val inMapper = IAPInMapper()
        return IAPManager(
            iapLifecycleObserver,
            outMapper,
            inMapper,
            purchasesUpdatedListener,
            logWrapperMock,
        )
    }
}
