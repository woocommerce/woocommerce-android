package com.woocommerce.android.iap.pub

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
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
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWpComPlanHandler
import com.woocommerce.android.iap.pub.model.IAPError
import com.woocommerce.android.iap.pub.model.PurchaseStatus
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult
import com.woocommerce.android.iap.pub.network.IAPMobilePayAPI
import com.woocommerce.android.iap.pub.network.model.CreateAndConfirmOrderResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
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

    private val testPreparationHelper = IAPTestPreparationHelper(billingClientMock)

    private lateinit var purchasesUpdatedListener: IAPPurchasesUpdatedListener
    private lateinit var sut: IAPPurchaseWPComPlanActionsImpl

    @Before
    fun setup() {
        setupSut(buildBillingResult(BillingClient.BillingResponseCode.OK))
    }

    //region Tests
    @Test
    fun `given success and product purchased, when iap wp com plan purchase checked, then purchased returned`() =
        runTest {
            // GIVEN
            val responseCode = BillingClient.BillingResponseCode.OK
            val debugMessage = "debug message"

            setupPurchaseQuery(
                responseCode = responseCode,
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PURCHASED,
                        isAcknowledged = false,
                    )
                )
            )
            testPreparationHelper.setupQueryProductDetails(
                responseCode = BillingClient.BillingResponseCode.OK,
                debugMessage = debugMessage
            )

            // WHEN
            val result = sut.isWPComPlanPurchased()

            // THEN
            assertThat(result).isInstanceOf(WPComIsPurchasedResult.Success::class.java)
            assertThat((result as WPComIsPurchasedResult.Success).purchaseStatus).isEqualTo(
                PurchaseStatus.PURCHASED
            )
        }

    @Test
    fun `given success and product acknowledged, when iap wp com plan purchase checked, then acknowledged returned`() =
        runTest {
            // GIVEN
            val responseCode = BillingClient.BillingResponseCode.OK
            val debugMessage = "debug message"

            setupPurchaseQuery(
                responseCode = responseCode,
                listOf(
                    buildPurchase(
                        listOf(iapProduct.productId),
                        Purchase.PurchaseState.PURCHASED,
                        isAcknowledged = true
                    )
                )
            )
            testPreparationHelper.setupQueryProductDetails(
                responseCode = BillingClient.BillingResponseCode.OK,
                debugMessage = debugMessage
            )

            // WHEN
            val result = sut.isWPComPlanPurchased()

            // THEN
            assertThat(result).isInstanceOf(WPComIsPurchasedResult.Success::class.java)
            assertThat((result as WPComIsPurchasedResult.Success).purchaseStatus).isEqualTo(
                PurchaseStatus.PURCHASED_AND_ACKNOWLEDGED
            )
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
    fun `given suc and other product purchased, when iap wp com plan purchase checked, then not purchased returned`() =
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

            testPreparationHelper.setupQueryProductDetails(
                responseCode = responseCode,
                productId = "another_product_id"
            )

            // WHEN
            val result = sut.isWPComPlanPurchased()

            // THEN
            assertThat(result).isInstanceOf(WPComIsPurchasedResult.Success::class.java)
            assertThat((result as WPComIsPurchasedResult.Success).purchaseStatus).isEqualTo(
                PurchaseStatus.NOT_PURCHASED
            )
        }

    @Test
    fun `given suc and product not purchased, when iap wp com plan purchase checked, then not purchased returned`() =
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

            testPreparationHelper.setupQueryProductDetails(responseCode = responseCode)

            // WHEN
            val result = sut.isWPComPlanPurchased()

            // THEN
            assertThat(result).isInstanceOf(WPComIsPurchasedResult.Success::class.java)
            assertThat((result as WPComIsPurchasedResult.Success).purchaseStatus).isEqualTo(
                PurchaseStatus.NOT_PURCHASED
            )
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
        val price = 1_000_000L
        val title = "wc_plan"
        val description = "best plan ever"
        testPreparationHelper.setupQueryProductDetails(
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
        assertThat(result.productInfo.price).isEqualTo(1.0)
        assertThat(result.productInfo.localizedTitle).isEqualTo(title)
        assertThat(result.productInfo.localizedDescription).isEqualTo(description)
    }

    @Test
    fun `given product query error, when fetching wp com plan product, then error returned`() = runTest {
        // GIVEN
        val debugMessage = "debug message"
        testPreparationHelper.setupQueryProductDetails(
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

        testPreparationHelper.setupQueryProductDetails(responseCode)

        setupMobilePayAPIMock(
            purchaseToken = purchaseToken,
            result = CreateAndConfirmOrderResponse.Success(REMOTE_SITE_ID)
        )

        // WHEN
        sut.purchaseWPComPlan(activityWrapperMock, REMOTE_SITE_ID)
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
        val result = sut.getPurchaseWpComPlanResult(REMOTE_SITE_ID).firstOrNull()
        assertThat(result).isInstanceOf(WPComPurchaseResult.Success::class.java)
    }

    @Test
    fun `given connection to service failed, when purchasing plan, then error returned`() = runTest {
        // GIVEN
        val debugMessage = "debug message"
        setupSut(buildBillingResult(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, debugMessage))

        // WHEN
        sut.purchaseWPComPlan(activityWrapperMock, REMOTE_SITE_ID)

        // THEN
        val result = sut.getPurchaseWpComPlanResult(REMOTE_SITE_ID).firstOrNull()
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
        testPreparationHelper.setupQueryProductDetails(
            responseCode = BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            debugMessage = debugMessage
        )

        // WHEN
        sut.purchaseWPComPlan(activityWrapperMock, REMOTE_SITE_ID)

        // THEN
        val result = sut.getPurchaseWpComPlanResult(REMOTE_SITE_ID).firstOrNull()
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

        testPreparationHelper.setupQueryProductDetails(responseCode = BillingClient.BillingResponseCode.OK)

        val debugMessage = "debug message"

        // WHEN
        sut.purchaseWPComPlan(activityWrapperMock, REMOTE_SITE_ID)
        purchasesUpdatedListener.onPurchasesUpdated(
            buildBillingResult(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, debugMessage),
            emptyList()
        )

        // THEN
        val result = sut.getPurchaseWpComPlanResult(REMOTE_SITE_ID).firstOrNull()
        assertThat(result).isInstanceOf(WPComPurchaseResult.Error::class.java)
        assertThat((result as WPComPurchaseResult.Error).errorType).isInstanceOf(
            IAPError.Billing.ServiceDisconnected::class.java
        )
        assertThat((result.errorType as IAPError.Billing).debugMessage).isEqualTo(debugMessage)
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

            testPreparationHelper.setupQueryProductDetails(responseCode)

            setupMobilePayAPIMock(
                purchaseToken = purchaseToken,
                result = CreateAndConfirmOrderResponse.Network
            )

            // WHEN
            sut.purchaseWPComPlan(activityWrapperMock, REMOTE_SITE_ID)
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
            val result = sut.getPurchaseWpComPlanResult(REMOTE_SITE_ID).firstOrNull()
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

        testPreparationHelper.setupQueryProductDetails(responseCode)

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
        sut.purchaseWPComPlan(activityWrapperMock, REMOTE_SITE_ID)

        // THEN
        val result = sut.getPurchaseWpComPlanResult(REMOTE_SITE_ID).firstOrNull()
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

        testPreparationHelper.setupQueryProductDetails(responseCode)

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
        sut.purchaseWPComPlan(activityWrapperMock, REMOTE_SITE_ID)

        // THEN
        val result = sut.getPurchaseWpComPlanResult(REMOTE_SITE_ID).firstOrNull()
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

            testPreparationHelper.setupQueryProductDetails(responseCode)

            setupMobilePayAPIMock(
                purchaseToken = purchaseToken,
                result = CreateAndConfirmOrderResponse.Network
            )

            // WHEN
            sut.purchaseWPComPlan(activityWrapperMock, REMOTE_SITE_ID)
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
            val result = sut.getPurchaseWpComPlanResult(REMOTE_SITE_ID).firstOrNull()
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

            testPreparationHelper.setupQueryProductDetails(responseCode)

            setupMobilePayAPIMock(
                purchaseToken = purchaseToken,
                result = CreateAndConfirmOrderResponse.Success(REMOTE_SITE_ID)
            )

            // WHEN
            sut.purchaseWPComPlan(activityWrapperMock, REMOTE_SITE_ID)
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
            val result = sut.getPurchaseWpComPlanResult(REMOTE_SITE_ID).firstOrNull()
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
            sut.purchaseWPComPlan(activityWrapperMock, REMOTE_SITE_ID)

            // THEN
            val result = sut.getPurchaseWpComPlanResult(REMOTE_SITE_ID).firstOrNull()
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

            testPreparationHelper.setupQueryProductDetails(responseCode)

            setupMobilePayAPIMock(
                purchaseToken = purchaseToken,
                result = CreateAndConfirmOrderResponse.Success(REMOTE_SITE_ID)
            )

            // WHEN
            sut.purchaseWPComPlan(activityWrapperMock, REMOTE_SITE_ID)

            // THEN
            val result = sut.getPurchaseWpComPlanResult(REMOTE_SITE_ID).firstOrNull()
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

            testPreparationHelper.setupQueryProductDetails(responseCode)

            // WHEN
            sut.purchaseWPComPlan(activityWrapperMock, REMOTE_SITE_ID)

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

            testPreparationHelper.setupQueryProductDetails(responseCode)

            // WHEN
            sut.purchaseWPComPlan(activityWrapperMock, REMOTE_SITE_ID)

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
        testPreparationHelper.setupBillingClient(connectionResult)
        val iapBillingClientStateHandler = IAPBillingClientStateHandler(
            billingClientProvider,
            outMapper,
            logWrapperMock,
        )
        val iapManager = IAPManager(
            iapBillingClientStateHandler,
            outMapper,
            IAPInMapper(),
            purchasesUpdatedListener,
            billingFlowParamsBuilderMock,
            periodicPurchaseStatusCheckerMock,
            logWrapperMock,
        )
        val purchaseWpComPlanHandler = IAPPurchaseWpComPlanHandler(
            iapMobilePayAPI = mobilePayAPIMock,
            iapManager = iapManager,
        )
        sut = IAPPurchaseWPComPlanActionsImpl(
            purchaseWpComPlanHandler = purchaseWpComPlanHandler,
            iapManager = iapManager,
            iapProduct = iapProduct,
        )
    }

    private fun setupPeriodicJob(purchasesResult: PurchasesResult) {
        whenever(periodicPurchaseStatusCheckerMock.startPeriodicPurchasesCheckJob(any(), any(), any()))
            .thenAnswer {
                @Suppress("UNCHECKED_CAST")
                (it.arguments[2] as (PurchasesResult) -> Unit).invoke(purchasesResult)
                mock<Job>()
            }
    }
}
