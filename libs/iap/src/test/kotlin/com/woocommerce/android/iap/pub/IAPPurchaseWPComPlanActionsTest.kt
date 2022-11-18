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
import com.woocommerce.android.iap.internal.core.IAPBillingFlowParamsBuilder
import com.woocommerce.android.iap.internal.core.IAPInMapper
import com.woocommerce.android.iap.internal.core.IAPManager
import com.woocommerce.android.iap.internal.core.IAPOutMapper
import com.woocommerce.android.iap.internal.core.IAPPeriodicPurchaseStatusChecker
import com.woocommerce.android.iap.internal.core.IAPPurchasesUpdatedListener
import com.woocommerce.android.iap.internal.core.isSuccess
import com.woocommerce.android.iap.internal.model.IAPProduct
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPI
import com.woocommerce.android.iap.internal.network.model.CreateAndConfirmOrderResponse
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWpComPlanHandler
import com.woocommerce.android.iap.pub.model.IAPError
import com.woocommerce.android.iap.pub.model.IAPSupportedResult
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

private const val REMOTE_SITE_ID = 1L

@ExperimentalCoroutinesApi
class IAPPurchaseWPComPlanActionsTest {
    private val logWrapperMock: IAPLogWrapper = mock()
    private val mobilePayAPIMock: IAPMobilePayAPI = mock()
    private val billingClientMock: IAPBillingClientWrapper = mock()
    private val billingFlowParamsBuilderMock: IAPBillingFlowParamsBuilder = mock {
        on { buildBillingFlowParams(any()) }.thenReturn(mock())
    }
    private val periodicPurchaseStatusCheckerMock: IAPPeriodicPurchaseStatusChecker = mock()
    private val activityWrapperMock: IAPActivityWrapper = mock {
        on { activity }.thenReturn(mock())
    }

    private val iapProduct: IAPProduct = IAPProduct.WPPremiumPlanTesting

    private lateinit var purchasesUpdatedListener: IAPPurchasesUpdatedListener
    private lateinit var sut: IAPPurchaseWPComPlanActionsImpl

    @Before
    fun setup() {
        setupSut(buildBillingResult(BillingClient.BillingResponseCode.OK))
    }

    //region Tests
    @Test
    fun `given product in NON USD currency, when iap support checked, then success with false returned`() = runTest {
        // GIVEN
        setupQueryProductDetails(
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
        setupQueryProductDetails(responseCode = BillingClient.BillingResponseCode.OK)

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

        setupQueryProductDetails(
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
        setupQueryProductDetails(responseCode = BillingClient.BillingResponseCode.OK, debugMessage = debugMessage)

        // WHEN
        val result = sut.isWPComPlanPurchased()

        // THEN
        assertThat(result).isInstanceOf(WPComIsPurchasedResult.Success::class.java)
        assertThat((result as WPComIsPurchasedResult.Success).isPlanPurchased).isTrue()
    }

    @Test
    fun `given connection to service failed, when iap wp com plan purchase checked, then true returned`() = runTest {
        // GIVEN
        val debugMessage = "debug message"
        setupSut(buildBillingResult(BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED, debugMessage))

        // WHEN
        val result = sut.isWPComPlanPurchased()

        // THEN
        assertThat(result).isInstanceOf(WPComIsPurchasedResult.Error::class.java)
        assertThat((result as WPComIsPurchasedResult.Error).errorType).isInstanceOf(
            IAPError.Billing.FeatureNotSupported::class.java
        )
        assertThat((result.errorType as IAPError.Billing).debugMessage).isEqualTo(debugMessage)
    }

    @Test
    fun `given suc and other product purchased, when iap wp com plan purchase checked, then false returned`() =
        runTest {
            // GIVEN
            val responseCode = BillingClient.BillingResponseCode.OK

            setupPurchaseQuery(
                responseCode = responseCode,
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PURCHASED
                    )
                )
            )

            setupQueryProductDetails(
                responseCode = responseCode,
                productId = "another_product_id"
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

            setupPurchaseQuery(
                responseCode = responseCode,
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PENDING
                    )
                )
            )

            setupQueryProductDetails(responseCode = responseCode)

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
        setupQueryProductDetails(
            responseCode = responseCode,
            priceMicroCents = price,
            title = title,
            description = description,
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
        val debugMessage = "debug message"
        setupQueryProductDetails(
            responseCode = BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
            debugMessage = debugMessage
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

    @Test
    fun `given connection to service failed, when fetching wp com plan product, then error returned`() = runTest {
        // GIVEN
        val debugMessage = "debug message"
        setupSut(buildBillingResult(BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED, debugMessage))

        // WHEN
        val result = sut.fetchWPComPlanProduct()

        // THEN
        assertThat(result).isInstanceOf(WPComProductResult.Error::class.java)
        assertThat((result as WPComProductResult.Error).errorType).isInstanceOf(
            IAPError.Billing.FeatureNotSupported::class.java
        )
        assertThat((result.errorType as IAPError.Billing).debugMessage).isEqualTo(debugMessage)
    }

    @Test
    fun `given product query and purchase success, when purchasing plan, then success returned`() = runTest {
        // GIVEN
        setupPurchaseQuery(
            responseCode = BillingClient.BillingResponseCode.OK,
            listOf(
                buildPurchase(
                    listOf(iapProduct.productId),
                    Purchase.PurchaseState.PURCHASED
                )
            )
        )

        val purchaseToken = "purchaseToken"
        val responseCode = BillingClient.BillingResponseCode.OK

        setupQueryProductDetails(responseCode)

        setupMobilePayAPIMock(
            purchaseToken = purchaseToken,
            result = CreateAndConfirmOrderResponse.Success(REMOTE_SITE_ID)
        )

        // WHEN
        sut.purchaseWPComPlan(activityWrapperMock)
        purchasesUpdatedListener.onPurchasesUpdated(
            buildBillingResult(responseCode),
            listOf(
                buildPurchase(
                    listOf(iapProduct.productId),
                    Purchase.PurchaseState.PURCHASED,
                    purchaseToken = purchaseToken
                )
            )
        )

        // THEN
        val result = sut.purchaseWpComPlanResult.firstOrNull()
        assertThat(result).isInstanceOf(WPComPurchaseResult.Success::class.java)
    }

    @Test
    fun `given connection to service failed, when purchasing plan, then error returned`() = runTest {
        // GIVEN
        val debugMessage = "debug message"
        setupSut(buildBillingResult(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, debugMessage))

        // WHEN
        sut.purchaseWPComPlan(activityWrapperMock)

        // THEN
        val result = sut.purchaseWpComPlanResult.firstOrNull()
        assertThat(result).isInstanceOf(WPComPurchaseResult.Error::class.java)
        assertThat((result as WPComPurchaseResult.Error).errorType).isInstanceOf(
            IAPError.Billing.ItemUnavailable::class.java
        )
        assertThat((result.errorType as IAPError.Billing).debugMessage).isEqualTo(debugMessage)
    }

    @Test
    fun `given product query error, when purchasing plan, then error returned`() = runTest {
        // GIVEN
        setupPurchaseQuery(
            responseCode = BillingClient.BillingResponseCode.OK,
            listOf(
                buildPurchase(
                    listOf(iapProduct.productId),
                    Purchase.PurchaseState.PURCHASED
                )
            )
        )

        val debugMessage = "debug message"
        setupQueryProductDetails(
            responseCode = BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            debugMessage = debugMessage
        )

        // WHEN
        sut.purchaseWPComPlan(activityWrapperMock)

        // THEN
        val result = sut.purchaseWpComPlanResult.firstOrNull()
        assertThat(result).isInstanceOf(WPComPurchaseResult.Error::class.java)
        assertThat((result as WPComPurchaseResult.Error).errorType).isInstanceOf(
            IAPError.Billing.ServiceDisconnected::class.java
        )
        assertThat((result.errorType as IAPError.Billing).debugMessage).isEqualTo(debugMessage)
    }

    @Test
    fun `given product query success and purchase error, when purchasing plan, then error returned`() = runTest {
        // GIVEN
        setupPurchaseQuery(
            responseCode = BillingClient.BillingResponseCode.OK,
            listOf()
        )

        setupQueryProductDetails(responseCode = BillingClient.BillingResponseCode.OK)

        val debugMessage = "debug message"

        // WHEN
        sut.purchaseWPComPlan(activityWrapperMock)
        purchasesUpdatedListener.onPurchasesUpdated(
            buildBillingResult(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, debugMessage),
            emptyList()
        )

        // THEN
        val result = sut.purchaseWpComPlanResult.firstOrNull()
        assertThat(result).isInstanceOf(WPComPurchaseResult.Error::class.java)
        assertThat((result as WPComPurchaseResult.Error).errorType).isInstanceOf(
            IAPError.Billing.ServiceDisconnected::class.java
        )
        assertThat((result.errorType as IAPError.Billing).debugMessage).isEqualTo(debugMessage)
    }

    @Test
    fun `given double invocation, when purchasing plan, then only one proceeds`() = runTest {
        // GIVEN
        setupPurchaseQuery(
            responseCode = BillingClient.BillingResponseCode.OK,
            listOf()
        )

        setupQueryProductDetails(responseCode = BillingClient.BillingResponseCode.OK)

        // WHEN
        awaitAll(
            async { sut.purchaseWPComPlan(activityWrapperMock) },
            async { sut.purchaseWPComPlan(activityWrapperMock) }
        )

        // THEN
        verify(billingClientMock, times(1)).queryPurchasesAsync(any())
    }

    @Test
    fun `given product query and purchase success and network err, when purchasing plan, then error returned`() =
        runTest {
            // GIVEN
            val purchaseToken = "purchaseToken"
            setupPurchaseQuery(
                responseCode = BillingClient.BillingResponseCode.OK,
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PURCHASED,
                        purchaseToken = purchaseToken,
                        isAcknowledged = false,
                    )
                )
            )

            val responseCode = BillingClient.BillingResponseCode.OK

            setupQueryProductDetails(responseCode)

            setupMobilePayAPIMock(
                purchaseToken = purchaseToken,
                result = CreateAndConfirmOrderResponse.Network
            )

            // WHEN
            sut.purchaseWPComPlan(activityWrapperMock)
            purchasesUpdatedListener.onPurchasesUpdated(
                buildBillingResult(responseCode),
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PURCHASED,
                        purchaseToken = purchaseToken
                    )
                )
            )

            // THEN
            val result = sut.purchaseWpComPlanResult.firstOrNull()
            assertThat(result).isInstanceOf(WPComPurchaseResult.Error::class.java)
            assertThat((result as WPComPurchaseResult.Error).errorType).isInstanceOf(
                IAPError.RemoteCommunication.Network::class.java
            )
        }

    @Test
    fun `given callback didnt respond and periodic task return suc, when purchasing plan, then suc return`() = runTest {
        // GIVEN
        setupPurchaseQuery(
            responseCode = BillingClient.BillingResponseCode.OK,
            listOf(
                buildPurchase(
                    listOf(iapProduct.productId),
                    Purchase.PurchaseState.PURCHASED
                )
            )
        )

        val purchaseToken = "purchaseToken"
        val responseCode = BillingClient.BillingResponseCode.OK

        setupQueryProductDetails(responseCode)

        setupMobilePayAPIMock(
            purchaseToken = purchaseToken,
            result = CreateAndConfirmOrderResponse.Success(REMOTE_SITE_ID)
        )

        val purchasesResult = PurchasesResult(
            buildBillingResult(responseCode),
            listOf(
                buildPurchase(
                    listOf(iapProduct.productId),
                    Purchase.PurchaseState.PURCHASED,
                    purchaseToken = purchaseToken
                )
            )
        )
        setupPeriodicJob(purchasesResult)

        // WHEN
        sut.purchaseWPComPlan(activityWrapperMock)

        // THEN
        val result = sut.purchaseWpComPlanResult.firstOrNull()
        assertThat(result).isInstanceOf(WPComPurchaseResult.Success::class.java)
    }

    @Test
    fun `given callback didnt respond and periodic task return err, when purchasing plan, then err return`() = runTest {
        // GIVEN
        setupPurchaseQuery(
            responseCode = BillingClient.BillingResponseCode.OK,
            listOf(
                buildPurchase(
                    listOf(),
                    Purchase.PurchaseState.PURCHASED
                )
            )
        )

        val purchaseToken = "purchaseToken"
        val responseCode = BillingClient.BillingResponseCode.OK

        setupQueryProductDetails(responseCode)

        setupMobilePayAPIMock(
            purchaseToken = purchaseToken,
            result = CreateAndConfirmOrderResponse.Success(REMOTE_SITE_ID)
        )

        val purchasesResult = PurchasesResult(
            buildBillingResult(BillingClient.BillingResponseCode.SERVICE_TIMEOUT),
            emptyList()
        )
        setupPeriodicJob(purchasesResult)

        // WHEN
        sut.purchaseWPComPlan(activityWrapperMock)

        // THEN
        val result = sut.purchaseWpComPlanResult.firstOrNull()
        assertThat(result).isInstanceOf(WPComPurchaseResult.Error::class.java)
        assertThat((result as WPComPurchaseResult.Error).errorType).isInstanceOf(
            IAPError.Billing.ServiceTimeout::class.java
        )
    }

    @Test
    fun `given having unacknowledged purchase and network error, when purchasing plan, then error returned`() =
        runTest {
            // GIVEN
            val purchaseToken = "purchaseToken"
            setupPurchaseQuery(
                responseCode = BillingClient.BillingResponseCode.OK,
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PURCHASED,
                        purchaseToken = purchaseToken,
                        isAcknowledged = false
                    )
                )
            )

            val responseCode = BillingClient.BillingResponseCode.OK

            setupQueryProductDetails(responseCode)

            setupMobilePayAPIMock(
                purchaseToken = purchaseToken,
                result = CreateAndConfirmOrderResponse.Network
            )

            // WHEN
            sut.purchaseWPComPlan(activityWrapperMock)
            purchasesUpdatedListener.onPurchasesUpdated(
                buildBillingResult(responseCode),
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PURCHASED,
                        purchaseToken = purchaseToken
                    )
                )
            )

            // THEN
            val result = sut.purchaseWpComPlanResult.firstOrNull()
            assertThat(result).isInstanceOf(WPComPurchaseResult.Error::class.java)
            assertThat((result as WPComPurchaseResult.Error).errorType).isInstanceOf(
                IAPError.RemoteCommunication.Network::class.java
            )
        }

    @Test
    fun `given having unacknowledged purchase and network success, when purchasing plan, then success returned`() =
        runTest {
            // GIVEN
            val purchaseToken = "purchaseToken"
            setupPurchaseQuery(
                responseCode = BillingClient.BillingResponseCode.OK,
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PURCHASED,
                        purchaseToken = purchaseToken,
                        isAcknowledged = false
                    )
                )
            )

            val responseCode = BillingClient.BillingResponseCode.OK

            setupQueryProductDetails(responseCode)

            setupMobilePayAPIMock(
                purchaseToken = purchaseToken,
                result = CreateAndConfirmOrderResponse.Success(REMOTE_SITE_ID)
            )

            // WHEN
            sut.purchaseWPComPlan(activityWrapperMock)
            purchasesUpdatedListener.onPurchasesUpdated(
                buildBillingResult(responseCode),
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PURCHASED,
                        purchaseToken = purchaseToken
                    )
                )
            )

            // THEN
            val result = sut.purchaseWpComPlanResult.firstOrNull()
            assertThat(result).isInstanceOf(WPComPurchaseResult.Success::class.java)
        }

    @Test
    fun `given error fetching purchases, when purchasing plan, then error returned`() =
        runTest {
            // GIVEN
            setupPurchaseQuery(
                responseCode = BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
                emptyList()
            )

            // WHEN
            sut.purchaseWPComPlan(activityWrapperMock)

            // THEN
            val result = sut.purchaseWpComPlanResult.firstOrNull()
            assertThat(result).isInstanceOf(WPComPurchaseResult.Error::class.java)
            assertThat((result as WPComPurchaseResult.Error).errorType).isInstanceOf(
                IAPError.Billing.ServiceTimeout::class.java
            )
        }

    @Test
    fun `given having acknowledged purchase, when purchasing plan, then success returned`() =
        runTest {
            // GIVEN
            val purchaseToken = "purchaseToken"
            setupPurchaseQuery(
                responseCode = BillingClient.BillingResponseCode.OK,
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PURCHASED,
                        purchaseToken = purchaseToken,
                        isAcknowledged = true
                    )
                )
            )

            val responseCode = BillingClient.BillingResponseCode.OK

            setupQueryProductDetails(responseCode)

            setupMobilePayAPIMock(
                purchaseToken = purchaseToken,
                result = CreateAndConfirmOrderResponse.Success(REMOTE_SITE_ID)
            )

            // WHEN
            sut.purchaseWPComPlan(activityWrapperMock)

            // THEN
            val result = sut.purchaseWpComPlanResult.firstOrNull()
            assertThat(result).isInstanceOf(WPComPurchaseResult.Success::class.java)
        }

    @Test
    fun `given purchase with not matched id, when purchasing plan, then backend is not called`() =
        runTest {
            // GIVEN
            val purchaseToken = "purchaseToken"
            setupPurchaseQuery(
                responseCode = BillingClient.BillingResponseCode.OK,
                listOf(
                    buildPurchase(
                        listOf("not_matched_id"),
                        Purchase.PurchaseState.PURCHASED,
                        purchaseToken = purchaseToken,
                    )
                )
            )

            val responseCode = BillingClient.BillingResponseCode.OK

            setupQueryProductDetails(responseCode)

            // WHEN
            sut.purchaseWPComPlan(activityWrapperMock)

            // THEN
            verifyNoInteractions(mobilePayAPIMock)
        }

    @Test
    fun `given purchase with matched id but not purchased, when purchasing plan, then backend is not called`() =
        runTest {
            // GIVEN
            val purchaseToken = "purchaseToken"
            setupPurchaseQuery(
                responseCode = BillingClient.BillingResponseCode.OK,
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PURCHASED,
                        purchaseToken = purchaseToken,
                    )
                )
            )

            val responseCode = BillingClient.BillingResponseCode.OK

            setupQueryProductDetails(responseCode)

            // WHEN
            sut.purchaseWPComPlan(activityWrapperMock)

            // THEN
            verifyNoInteractions(mobilePayAPIMock)
        }
//endregion

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

    private suspend fun setupQueryProductDetails(
        responseCode: Int,
        debugMessage: String = "",
        productId: String = iapProduct.productId,
        priceMicroCents: Long = 10_000L,
        currency: String = "USD",
        title: String = "title",
        description: String = "description"
    ) {
        val productDetails = buildProductDetails(
            productId = productId,
            name = "productName",
            price = priceMicroCents,
            currency = currency,
            title = title,
            description = description,
        )
        whenever(billingClientMock.queryProductDetails(any())).thenReturn(
            ProductDetailsResult(
                buildBillingResult(responseCode, debugMessage),
                listOf(productDetails)
            )
        )
    }

    private suspend fun setupMobilePayAPIMock(
        remoteSiteId: Long = REMOTE_SITE_ID,
        productIdentifier: String = iapProduct.productId,
        priceInCents: Int = 1,
        currency: String = "USD",
        purchaseToken: String,
        result: CreateAndConfirmOrderResponse,
    ) {
        whenever(
            mobilePayAPIMock.createAndConfirmOrder(
                remoteSiteId = remoteSiteId,
                productIdentifier = productIdentifier,
                priceInCents = priceInCents,
                currency = currency,
                purchaseToken = purchaseToken,
                appId = "com.woocommerce.android",
            )
        ).thenReturn(result)
    }

    private fun setupSut(connectionResult: BillingResult) {
        purchasesUpdatedListener = IAPPurchasesUpdatedListener(logWrapperMock)
        val billingClientProvider: IAPBillingClientProvider = mock {
            on { provideBillingClient() }.thenReturn(billingClientMock)
        }
        val outMapper = IAPOutMapper()
        setupBillingClient(connectionResult)
        val iapBillingClientStateHandler = IAPBillingClientStateHandler(
            billingClientProvider,
            outMapper,
            logWrapperMock,
        )
        val iapManager = buildIapManager(iapBillingClientStateHandler, purchasesUpdatedListener, outMapper)
        val purchaseWpComPlanHandler = IAPPurchaseWpComPlanHandler(
            iapMobilePayAPI = mobilePayAPIMock,
            iapManager = iapManager,
        )
        sut = IAPPurchaseWPComPlanActionsImpl(
            purchaseWpComPlanHandler = purchaseWpComPlanHandler,
            iapManager = buildIapManager(iapBillingClientStateHandler, purchasesUpdatedListener, outMapper),
            REMOTE_SITE_ID,
            iapProduct = iapProduct,
        )
    }

    private fun setupBillingClient(connectionResult: BillingResult) {
        whenever(billingClientMock.startConnection(any())).thenAnswer {
            val listener = it.arguments[0] as BillingClientStateListener
            listener.onBillingSetupFinished(connectionResult)
            whenever(billingClientMock.isReady).thenReturn(connectionResult.isSuccess)
        }
        whenever(billingClientMock.isReady).thenReturn(false)
    }

    private fun setupPeriodicJob(purchasesResult: PurchasesResult) {
        whenever(periodicPurchaseStatusCheckerMock.startPeriodicPurchasesCheckJob(any(), any(), any()))
            .thenAnswer {
                (it.arguments[2] as (PurchasesResult) -> Unit).invoke(purchasesResult)
                mock<Job>()
            }
    }

    private fun buildIapManager(
        iapBillingClientStateHandler: IAPBillingClientStateHandler,
        purchasesUpdatedListener: IAPPurchasesUpdatedListener,
        outMapper: IAPOutMapper
    ) = IAPManager(
        iapBillingClientStateHandler,
        outMapper,
        IAPInMapper(),
        purchasesUpdatedListener,
        billingFlowParamsBuilderMock,
        periodicPurchaseStatusCheckerMock,
        logWrapperMock,
    )
}
