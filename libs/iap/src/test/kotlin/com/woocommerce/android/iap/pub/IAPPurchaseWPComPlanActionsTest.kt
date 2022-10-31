package com.woocommerce.android.iap.pub

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.woocommerce.android.iap.internal.core.IAPBillingClientProvider
import com.woocommerce.android.iap.internal.core.IAPBillingClientStateHandler
import com.woocommerce.android.iap.internal.core.IAPBillingClientWrapper
import com.woocommerce.android.iap.internal.core.IAPInMapper
import com.woocommerce.android.iap.internal.core.IAPManager
import com.woocommerce.android.iap.internal.core.IAPOutMapper
import com.woocommerce.android.iap.internal.core.IAPPurchasesUpdatedListener
import com.woocommerce.android.iap.internal.model.IAPProduct
import com.woocommerce.android.iap.internal.model.IAPSupportedResult
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPI
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl
import com.woocommerce.android.iap.pub.model.IAPError
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.iap.pub.model.WPComProductResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

private const val REMOTE_SITE_ID = 1L

@ExperimentalCoroutinesApi
class IAPPurchaseWPComPlanActionsTest {
    private val logWrapperMock: IAPLogWrapper = mock()
    private val mobilePayAPIMock: IAPMobilePayAPI = mock()
    private val billingClientMock: IAPBillingClientWrapper = mock()

    private val iapProduct: IAPProduct = IAPProduct.WPPremiumPlanTesting

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
            iapManager = buildIapManager(iapBillingClientStateHandler, purchasesUpdatedListener),
            REMOTE_SITE_ID,
            iapProduct = iapProduct,
        )
    }

    //region Tests
    @Test
    fun `given product in NON USD currency, when iap support checked, then success with false returned`() = runTest {
        // GIVEN
        val productDetails = buildProductDetails(
            productId = iapProduct.productId,
            name = "productName",
            price = 100L,
            currency = "AED"
        )
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
        val productDetails = buildProductDetails(
            productId = iapProduct.productId,
            name = "productName",
            price = 100L,
            currency = "USD"
        )
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
        val productDetails = buildProductDetails(
            productId = iapProduct.productId,
            name = "productName",
            price = 100L,
            currency = "USD"
        )
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

    @Test
    fun `given success and product purchased, when iap wp com plan purchase checked, then true returned`() = runTest {
        // GIVEN
        val responseCode = BillingClient.BillingResponseCode.OK
        val debugMessage = "debug message"

        setupPurchaseQuery(
            responseCode = responseCode,
            listOf(
                buildPurchase(
                    listOf(iapProduct.productId),
                    Purchase.PurchaseState.PURCHASED
                )
            )
        )

        val productDetails = buildProductDetails(
            productId = iapProduct.productId,
            name = "productName",
            price = 100L,
            currency = "USD"
        )
        whenever(billingClientMock.queryProductDetails(any())).thenReturn(
            ProductDetailsResult(
                buildBillingResult(responseCode, debugMessage),
                listOf(productDetails)
            )
        )

        // WHEN
        val result = sut.isWPComPlanPurchased()

        // THEN
        assertThat(result).isInstanceOf(WPComIsPurchasedResult.Success::class.java)
        assertThat((result as WPComIsPurchasedResult.Success).isPlanPurchased).isTrue()
    }

    @Test
    fun `given suc and other product purchased, when iap wp com plan purchase checked, then false returned`() =
        runTest {
            // GIVEN
            val responseCode = BillingClient.BillingResponseCode.OK
            val debugMessage = "debug message"

            setupPurchaseQuery(
                responseCode = responseCode,
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PURCHASED
                    )
                )
            )

            val productDetails = buildProductDetails(
                productId = "another_product_id",
                name = "productName",
                price = 100L,
                currency = "USD"
            )
            whenever(billingClientMock.queryProductDetails(any())).thenReturn(
                ProductDetailsResult(
                    buildBillingResult(responseCode, debugMessage),
                    listOf(productDetails)
                )
            )

            // WHEN
            val result = sut.isWPComPlanPurchased()

            // THEN
            assertThat(result).isInstanceOf(WPComIsPurchasedResult.Success::class.java)
            assertThat((result as WPComIsPurchasedResult.Success).isPlanPurchased).isFalse()
        }

    @Test
    fun `given suc and product not purchased, when iap wp com plan purchase checked, then false returned`() =
        runTest {
            // GIVEN
            val responseCode = BillingClient.BillingResponseCode.OK
            val debugMessage = "debug message"

            setupPurchaseQuery(
                responseCode = responseCode,
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PENDING
                    )
                )
            )

            val productDetails = buildProductDetails(
                productId = iapProduct.productId,
                name = "productName",
                price = 100L,
                currency = "USD"
            )
            whenever(billingClientMock.queryProductDetails(any())).thenReturn(
                ProductDetailsResult(
                    buildBillingResult(responseCode, debugMessage),
                    listOf(productDetails)
                )
            )

            // WHEN
            val result = sut.isWPComPlanPurchased()

            // THEN
            assertThat(result).isInstanceOf(WPComIsPurchasedResult.Success::class.java)
            assertThat((result as WPComIsPurchasedResult.Success).isPlanPurchased).isFalse()
        }

    @Test
    fun `given error, when iap wp com plan purchase checked, then error returned`() = runTest {
        // GIVEN
        val responseCode = BillingClient.BillingResponseCode.DEVELOPER_ERROR
        val debugMessage = "debug message"

        setupPurchaseQuery(
            responseCode = responseCode,
            emptyList(),
            debugMessage = debugMessage
        )

        // WHEN
        val result = sut.isWPComPlanPurchased()

        // THEN
        assertThat(result).isInstanceOf(WPComIsPurchasedResult.Error::class.java)
        assertThat((result as WPComIsPurchasedResult.Error).errorType).isInstanceOf(
            IAPError.Billing.DeveloperError::class.java
        )
        assertThat((result.errorType as IAPError.Billing).debugMessage).isEqualTo(debugMessage)
    }

    @Test
    fun `given product query success, when fetching wp com plan product, then returned product info`() = runTest {
        // GIVEN
        val responseCode = BillingClient.BillingResponseCode.OK

        val currency = "USD"
        val price = 100L
        val title = "wc_plan"
        val description = "best plan ever"
        val productDetails = buildProductDetails(
            productId = iapProduct.productId,
            name = "productName",
            price = price,
            currency = currency,
            title = title,
            description = description,
        )
        whenever(billingClientMock.queryProductDetails(any())).thenReturn(
            ProductDetailsResult(
                buildBillingResult(responseCode),
                listOf(productDetails)
            )
        )

        // WHEN
        val result = sut.fetchWPComPlanProduct()

        // THEN
        assertThat(result).isInstanceOf(WPComProductResult.Success::class.java)
        assertThat((result as WPComProductResult.Success).productInfo.currency).isEqualTo(currency)
        assertThat(result.productInfo.price).isEqualTo(price)
        assertThat(result.productInfo.localizedTitle).isEqualTo(title)
        assertThat(result.productInfo.localizedDescription).isEqualTo(description)
    }

    @Test
    fun `given product query error, when fetching wp com plan product, then error returned`() = runTest {
        // GIVEN
        val responseCode = BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED
        val debugMessage = "debug message"

        whenever(billingClientMock.queryProductDetails(any())).thenReturn(
            ProductDetailsResult(
                buildBillingResult(responseCode, debugMessage),
                emptyList()
            )
        )

        // WHEN
        val result = sut.fetchWPComPlanProduct()

        // THEN
        assertThat(result).isInstanceOf(WPComProductResult.Error::class.java)
        assertThat((result as WPComProductResult.Error).errorType).isInstanceOf(
            IAPError.Billing.FeatureNotSupported::class.java
        )
        assertThat((result.errorType as IAPError.Billing).debugMessage).isEqualTo(debugMessage)
    }

    private suspend fun setupPurchaseQuery(
        @BillingClient.BillingResponseCode responseCode: Int,
        purchases: List<Purchase>,
        debugMessage: String = "",
    ) {
        whenever(billingClientMock.queryPurchasesAsync(any())).thenReturn(
            PurchasesResult(
                buildBillingResult(responseCode, debugMessage),
                purchases
            )
        )
    }
    //endregion

    private fun setupBillingClientToBeConnected() {
        whenever(billingClientMock.startConnection(any())).thenAnswer {
            val listener = it.arguments[0] as BillingClientStateListener
            listener.onBillingSetupFinished(BillingResult())
            whenever(billingClientMock.isReady).thenReturn(true)
        }
    }

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
