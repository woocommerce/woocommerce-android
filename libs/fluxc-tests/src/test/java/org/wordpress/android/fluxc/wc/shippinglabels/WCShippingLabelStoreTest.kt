package org.wordpress.android.fluxc.wc.shippinglabels

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.TestSiteSqlUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.Valid
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.CustomPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption.PredefinedPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCPaymentMethod
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingAccountSettings
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelCreationEligibility
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelMapper
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress.Type.DESTINATION
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress.Type.ORIGIN
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelPackageData
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingRatesResult
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingRatesResult.ShippingOption
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingRatesResult.ShippingPackage
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.LabelItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.SLCreationEligibilityApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient.ShippingRatesApiResponse.ShippingOption.Rate
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient.VerifyAddressResponse
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("LargeClass")
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCShippingLabelStoreTest {
    private val restClient = mock<ShippingLabelRestClient>()
    private val orderId = 25L
    private val refundShippingLabelId = 12L
    private val site = SiteModel().apply { id = 321 }
    private val errorSite = SiteModel().apply { id = 123 }
    private val mapper = WCShippingLabelMapper()
    private lateinit var store: WCShippingLabelStore

    private val sampleShippingLabelApiResponse = WCShippingLabelTestUtils.generateSampleShippingLabelApiResponse()
    private val error = WooError(INVALID_RESPONSE, NETWORK_ERROR, "Invalid site ID")

    private val printPaperSize = "label"
    private val samplePrintShippingLabelApiResponse =
            WCShippingLabelTestUtils.generateSamplePrintShippingLabelApiResponse()

    private val address = ShippingLabelAddress(country = "CA", address = "1370 Lewisham Dr.")
    private val successfulVerifyAddressApiResponse = VerifyAddressResponse(
            isSuccess = true,
            isTrivialNormalization = false,
            suggestedAddress = address,
            error = null
    )

    private val samplePackagesApiResponse = WCShippingLabelTestUtils.generateSampleGetPackagesApiResponse()

    private val sampleAccountSettingsApiResponse = WCShippingLabelTestUtils.generateSampleAccountSettingsApiResponse()

    private val sampleShippingRatesApiResponse = WCShippingLabelTestUtils.generateSampleGetShippingRatesApiResponse()

    private val samplePurchaseShippingLabelsResponse =
            WCShippingLabelTestUtils.generateSamplePurchaseShippingLabelsApiResponse()

    private val originAddress = ShippingLabelAddress(
            "Company",
            "Ondrej Ruttkay",
            "",
            "US",
            "NY",
            "42 Jewel St.",
            "",
            "Brooklyn",
            "11222"
    )

    private val destAddress = ShippingLabelAddress(
            "Company",
            "Ondrej Ruttkay",
            "",
            "US",
            "NY",
            "82 Jewel St.",
            "",
            "Brooklyn",
            "11222"
    )

    private val packages = listOf(
            ShippingLabelPackage(
                    "Krabka 1",
                    "medium_flat_box_top",
                    10f,
                    10f,
                    10f,
                    10f,
                    false
            ),
            ShippingLabelPackage(
                    "Krabka 2",
                    "medium_flat_box_side",
                    5f,
                    5f,
                    5f,
                    5f,
                    false
            )
    )

    private val purchaseLabelPackagesData = listOf(
            WCShippingLabelPackageData(
                    id = "id1",
                    boxId = "medium_flat_box_top",
                    isLetter = false,
                    height = 10f,
                    width = 10f,
                    length = 10f,
                    weight = 10f,
                    shipmentId = "shp_id",
                    rateId = "rate_id",
                    serviceId = "service-1",
                    serviceName = "USPS - Priority Mail International label",
                    carrierId = "usps",
                    products = listOf(10)
            )
    )

    private val sampleListOfOneCustomPackage = listOf(
            CustomPackage(
                    "Package 1",
                    false,
                    "10 x 10 x 10",
                    1.0f
            )
    )

    private val sampleListOfOnePredefinedPackage = listOf(
            PredefinedOption(
                    title = "USPS Priority Mail Flat Rate Boxes",
                    carrier = "usps",
                    predefinedPackages = listOf(
                            PredefinedPackage(
                                    id = "small_flat_box",
                                    title = "Small Flat Box",
                                    isLetter = false,
                                    dimensions = "10 x 10 x 10",
                                    boxWeight = 1.0f
                            )
                    )
            )
    )

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(
                        SiteModel::class.java,
                        WCShippingLabelModel::class.java,
                        WCShippingLabelCreationEligibility::class.java
                ),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        store = WCShippingLabelStore(
                restClient,
                initCoroutineEngine(),
                mapper
        )

        // Insert the site into the db so it's available later when testing shipping labels
        TestSiteSqlUtils.siteSqlUtils.insertOrUpdateSite(site)
    }

    @Test
    fun `fetch shipping labels for order`() = test {
        val result = fetchShippingLabelsForOrder()
        val shippingLabelModels = mapper.map(sampleShippingLabelApiResponse!!, site)

        assertThat(result.model?.size).isEqualTo(shippingLabelModels.size)
        assertThat(result.model?.first()?.remoteOrderId).isEqualTo(shippingLabelModels.first().remoteOrderId)
        assertThat(result.model?.first()?.localSiteId).isEqualTo(shippingLabelModels.first().localSiteId)
        assertThat(result.model?.first()?.remoteShippingLabelId)
                .isEqualTo(shippingLabelModels.first().remoteShippingLabelId)
        assertThat(result.model?.first()?.carrierId).isEqualTo(shippingLabelModels.first().carrierId)
        assertThat(result.model?.first()?.packageName).isEqualTo(shippingLabelModels.first().packageName)
        assertThat(result.model?.first()?.refundableAmount).isEqualTo(shippingLabelModels.first().refundableAmount)
        assertThat(result.model?.first()?.rate).isEqualTo(shippingLabelModels.first().rate)
        assertThat(result.model?.first()?.getProductNameList()?.size)
                .isEqualTo(shippingLabelModels.first().getProductNameList().size)
        assertThat(result.model?.first()?.getProductIdsList()?.size)
                .isEqualTo(shippingLabelModels.first().getProductIdsList().size)
        assertThat(result.model?.get(1)?.getProductIdsList()?.size).isEqualTo(0)
        assertNotNull(result.model?.first()?.refund)

        val invalidRequestResult = store.fetchShippingLabelsForOrder(errorSite, orderId)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `get stored shipping labels for order`() = test {
        fetchShippingLabelsForOrder()

        val storedShippingLabelsList = store.getShippingLabelsForOrder(site, orderId)

        val shippingLabelModels = mapper.map(sampleShippingLabelApiResponse!!, site)
        assertThat(storedShippingLabelsList.size).isEqualTo(shippingLabelModels.size)
        assertThat(storedShippingLabelsList.first().remoteOrderId).isEqualTo(shippingLabelModels.first().remoteOrderId)
        assertThat(storedShippingLabelsList.first().localSiteId).isEqualTo(shippingLabelModels.first().localSiteId)
        assertThat(storedShippingLabelsList.first().remoteShippingLabelId)
                .isEqualTo(shippingLabelModels.first().remoteShippingLabelId)
        assertThat(storedShippingLabelsList.first().carrierId).isEqualTo(shippingLabelModels.first().carrierId)
        assertThat(storedShippingLabelsList.first().packageName).isEqualTo(shippingLabelModels.first().packageName)
        assertThat(storedShippingLabelsList.first().refundableAmount)
                .isEqualTo(shippingLabelModels.first().refundableAmount)
        assertThat(storedShippingLabelsList.first().rate).isEqualTo(shippingLabelModels.first().rate)
        assertNotNull(storedShippingLabelsList.first().refund)
        assertThat(storedShippingLabelsList.first().getProductNameList().size)
                .isEqualTo(shippingLabelModels.first().getProductNameList().size)
        assertThat(storedShippingLabelsList.first().getProductIdsList().size)
                .isEqualTo(shippingLabelModels.first().getProductIdsList().size)
        assertThat(storedShippingLabelsList[1].getProductIdsList().size).isEqualTo(0)

        val invalidRequestResult = store.getShippingLabelsForOrder(errorSite, orderId)
        assertThat(invalidRequestResult.size).isEqualTo(0)
    }

    @Test
    fun `refund shipping label for order`() = test {
        val result = refundShippingLabelForOrder()
        assertTrue(result.model!!)

        val invalidRequestResult = store.refundShippingLabelForOrder(errorSite, orderId, refundShippingLabelId)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `print shipping label for order`() = test {
        val result = printShippingLabelForOrder()
        assertThat(result.model).isEqualTo(samplePrintShippingLabelApiResponse?.b64Content)

        val invalidRequestResult = store.printShippingLabel(errorSite, printPaperSize, refundShippingLabelId)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `verify shipping address`() = test {
        val result = verifyAddress(ORIGIN)
        assertThat(result.model).isEqualTo(Valid(address, successfulVerifyAddressApiResponse.isTrivialNormalization))

        val invalidRequestResult = verifyAddress(DESTINATION)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    @Suppress("LongMethod")
    fun `get shipping rates`() = test {
        val expectedRatesResult = WCShippingRatesResult(
            listOf(
                ShippingPackage(
                    "default_box",
                    listOf(
                        ShippingOption(
                            "default",
                            listOf(
                                Rate(
                                    title = "USPS - Media Mail",
                                    insurance = "100",
                                    rate = BigDecimal(3.5),
                                    rateId = "rate_cb976896a09c4171a93ace57ed66ce5b",
                                    serviceId = "MediaMail",
                                    carrierId = "usps",
                                    shipmentId = "shp_0a9b3ff983c6427eaf1e24cb344de36a",
                                    hasTracking = false,
                                    retailRate = BigDecimal(3.5),
                                    isSelected = false,
                                    isPickupFree = false,
                                    deliveryDays = 2,
                                    deliveryDateGuaranteed = false,
                                    deliveryDate = null
                                ),
                                Rate(
                                    title = "FedEx - Ground",
                                    insurance = "100",
                                    rate = BigDecimal(21.5),
                                    rateId = "rate_1b202bd43a8c4c929c73bb46989ef745",
                                    serviceId = "FEDEX_GROUND",
                                    carrierId = "fedex",
                                    shipmentId = "shp_0a9b3ff983c6427eaf1e24cb344de36a",
                                    hasTracking = false,
                                    retailRate = BigDecimal(21.5),
                                    isSelected = false,
                                    isPickupFree = false,
                                    deliveryDays = 1,
                                    deliveryDateGuaranteed = true,
                                    deliveryDate = null
                                )
                        )
                ),
                ShippingOption(
                        "with_signature",
                        listOf(
                                Rate(
                                    title = "USPS - Media Mail",
                                    insurance = "100",
                                    rate = BigDecimal(13.5),
                                    rateId = "rate_cb976896a09c4171a93ace57ed66ce5b",
                                    serviceId = "MediaMail",
                                    carrierId = "usps",
                                    shipmentId = "shp_0a9b3ff983c6427eaf1e24cb344de36a",
                                    hasTracking = true,
                                    retailRate = BigDecimal(13.5),
                                    isSelected = false,
                                    isPickupFree = true,
                                    deliveryDays = 2,
                                    deliveryDateGuaranteed = false,
                                    deliveryDate = null
                                ),
                                Rate(
                                    title = "FedEx - Ground",
                                    insurance = "100",
                                    rate = BigDecimal(121.5),
                                    rateId = "rate_1b202bd43a8c4c929c73bb46989ef745",
                                    serviceId = "FEDEX_GROUND",
                                    carrierId = "fedex",
                                    shipmentId = "shp_0a9b3ff983c6427eaf1e24cb344de36a",
                                    hasTracking = true,
                                    retailRate = BigDecimal(121.5),
                                    isSelected = false,
                                    isPickupFree = true,
                                    deliveryDays = 1,
                                    deliveryDateGuaranteed = true,
                                    deliveryDate = null
                                )
                            )
                        )
                    )
                )
            )
        )
        val result = getShippingRates()
        assertThat(result.model).isEqualTo(expectedRatesResult)
    }

    @Test
    fun `get packages`() = test {
        val expectedResult = WCPackagesResult(
                listOf(
                        CustomPackage("Krabica", false, "1 x 2 x 3", 1f),
                        CustomPackage("Obalka", true, "2 x 3 x 4", 5f),
                        CustomPackage("Flat Box", true, "5 x 6 x 4", 1f),
                        CustomPackage("Weird Box", false, "0 x 0 x 0", 0f)
                ),
                listOf(
                        PredefinedOption("USPS Priority Mail Flat Rate Boxes",
                                "usps",
                                listOf(
                                        PredefinedPackage(
                                                "small_flat_box",
                                                "Small Flat Rate Box",
                                                false,
                                                "21.91 x 13.65 x 4.13",
                                                0f
                                        ),
                                        PredefinedPackage(
                                                "medium_flat_box_top",
                                                "Medium Flat Rate Box 1, Top Loading",
                                                false,
                                                "28.57 x 22.22 x 15.24",
                                                0f
                                        )
                                )
                        ),
                        PredefinedOption(
                                "DHL Express",
                                "dhlexpress",
                                listOf(PredefinedPackage(
                                        "LargePaddedPouch",
                                        "Large Padded Pouch",
                                        true,
                                        "30.22 x 35.56 x 2.54",
                                        0f
                                ))
                        )
                )
        )
        val result = getPackages()
        assertThat(result.model).isEqualTo(expectedResult)

        val invalidRequestResult = getPackages(true)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `get account settings`() = test {
        val expectedPaymentMethodList = listOf(WCPaymentMethod(
                paymentMethodId = 4144354,
                name = "John Doe",
                cardType = "visa",
                cardDigits = "5454",
                expiry = "2023-12-31"
        ))
        val result = getAccountSettings()
        val model = result.model!!
        assertThat(model.canManagePayments).isTrue()
        assertThat(model.lastUsedBoxId).isEqualTo("small_flat_box")
        assertThat(model.selectedPaymentMethodId).isEqualTo(4144354)
        assertThat(model.paymentMethods).isEqualTo(expectedPaymentMethodList)

        val invalidRequestResult = getAccountSettings(true)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `purchase shipping label error`() = test {
        whenever(restClient.purchaseShippingLabels(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                any()
        ))
                .thenReturn(WooPayload(error))

        val invalidRequestResult = store.purchaseShippingLabels(
                site,
                orderId,
                originAddress,
                destAddress,
                purchaseLabelPackagesData,
                null
        )
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    @Suppress("LongMethod")
    fun `purchase shipping labels with polling`() = test {
        val response = WooPayload(samplePurchaseShippingLabelsResponse)
        whenever(restClient.purchaseShippingLabels(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                any()
        )).thenReturn(response)

        val statusIntermediateResponse = WCShippingLabelTestUtils.generateSampleShippingLabelsStatusApiResponse(false)
        val statusDoneReponse = WCShippingLabelTestUtils.generateSampleShippingLabelsStatusApiResponse(true)
        whenever(restClient.fetchShippingLabelsStatus(any(), any(), any()))
                .thenReturn(WooPayload(statusIntermediateResponse))
                .thenReturn(WooPayload(statusDoneReponse))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val result = store.purchaseShippingLabels(
                site,
                orderId,
                originAddress,
                destAddress,
                purchaseLabelPackagesData,
                null
        )
        val shippingLabelModels = mapper.map(
                samplePurchaseShippingLabelsResponse,
                orderId,
                originAddress,
                destAddress,
                site
        )

        verify(restClient).purchaseShippingLabels(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                any()
        )
        verify(restClient).fetchShippingLabelsStatus(
                site,
                orderId,
                samplePurchaseShippingLabelsResponse.labels!!.map { it.labelId!! })
        verify(restClient).fetchShippingLabelsStatus(
                site,
                orderId,
                statusIntermediateResponse.labels!!
                        .filter { it.status != LabelItem.STATUS_PURCHASED }
                        .map { it.labelId!! })

        assertThat(result.model!!.size).isEqualTo(shippingLabelModels.size)
        assertThat(result.model!!.first().remoteOrderId).isEqualTo(shippingLabelModels.first().remoteOrderId)
        assertThat(result.model!!.first().localSiteId).isEqualTo(shippingLabelModels.first().localSiteId)
        assertThat(result.model!!.first().remoteShippingLabelId)
                .isEqualTo(shippingLabelModels.first().remoteShippingLabelId)
        assertThat(result.model!!.first().carrierId).isEqualTo(shippingLabelModels.first().carrierId)
        assertThat(result.model!!.first().packageName).isEqualTo(shippingLabelModels.first().packageName)
        assertThat(result.model!!.first().refundableAmount).isEqualTo(shippingLabelModels.first().refundableAmount)
    }

    @Test
    fun `purchase shipping labels with polling fail after 3 retries`() = test {
        val response = WooPayload(samplePurchaseShippingLabelsResponse)
        whenever(restClient.purchaseShippingLabels(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                any()
        )).thenReturn(response)

        whenever(restClient.fetchShippingLabelsStatus(any(), any(), any())).thenReturn(WooPayload(error))

        val result = store.purchaseShippingLabels(
                site,
                orderId,
                originAddress,
                destAddress,
                purchaseLabelPackagesData,
                null
        )

        verify(restClient).purchaseShippingLabels(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                any()
        )
        verify(restClient, times(3)).fetchShippingLabelsStatus(
                site,
                orderId,
                samplePurchaseShippingLabelsResponse.labels!!.map { it.labelId!! })

        assertThat(result.error).isEqualTo(error)
    }

    @Test
    fun `purchase shipping labels with polling one label failed`() = test {
        val response = WooPayload(samplePurchaseShippingLabelsResponse)
        whenever(restClient.purchaseShippingLabels(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                any()
        )).thenReturn(response)

        val statusIntermediateResponse = WCShippingLabelTestUtils.generateSampleShippingLabelsStatusApiResponse(false)
        val statusErrorResponse = WCShippingLabelTestUtils.generateErrorShippingLabelsStatusApiResponse()
        whenever(restClient.fetchShippingLabelsStatus(any(), any(), any()))
                .thenReturn(WooPayload(statusIntermediateResponse))
                .thenReturn(WooPayload(statusErrorResponse))

        val result = store.purchaseShippingLabels(
                site,
                orderId,
                originAddress,
                destAddress,
                purchaseLabelPackagesData,
                null
        )

        assertThat(result.isError).isTrue()
        assertThat(result.error.message).isEqualTo(statusErrorResponse.labels!!.first().error)
    }

    @Test
    fun `fetch shipping label eligibility`() = test {
        val result = fetchSLCreationEligibility(
                canCreatePackage = false,
                canCreatePaymentMethod = false,
                canCreateCustomsForm = false,
                isEligible = true,
                isError = false
        )
        assertThat(result.model).isNotNull
        assertThat(result.model!!.isEligible).isTrue

        val failedResult = fetchSLCreationEligibility(
            canCreatePackage = false,
            canCreatePaymentMethod = false,
            canCreateCustomsForm = false,
            isError = true
        )
        assertThat(failedResult.model).isNull()
        assertThat(failedResult.error).isEqualTo(error)
    }

    @Test
    fun `get stored eligibility`() = test {
        fetchSLCreationEligibility(
                canCreatePackage = false,
                canCreatePaymentMethod = false,
                canCreateCustomsForm = false,
                isEligible = true,
                isError = false
        )

        val eligibility = store.isOrderEligibleForShippingLabelCreation(
                site = site,
                orderId = orderId,
                canCreatePackage = false,
                canCreatePaymentMethod = false,
                canCreateCustomsForm = false
        )
        assertThat(eligibility).isNotNull
        assertThat(eligibility!!.isEligible).isTrue
    }

    @Test
    fun `invalidate cached data if parameters are different`() = test {
        fetchSLCreationEligibility(
                canCreatePackage = false,
                canCreatePaymentMethod = false,
                canCreateCustomsForm = false,
                isEligible = true,
                isError = false
        )

        val eligibility = store.isOrderEligibleForShippingLabelCreation(
                site = site,
                orderId = orderId,
                canCreatePackage = true,
                canCreatePaymentMethod = false,
                canCreateCustomsForm = false
        )
        assertThat(eligibility).isNull()
    }

    @Test
    fun `creating packages returns true if the API call succeeds`() = test {
        val response = WooPayload(true)
        whenever(restClient.createPackages(site = any(), customPackages = any(), predefinedOptions = any()))
                .thenReturn(response)

        val expectedResult = WooResult(true)
        val successfulRequestResult = store.createPackages(
                site = site,
                customPackages = sampleListOfOneCustomPackage,
                predefinedPackages = emptyList()
        )
        assertEquals(successfulRequestResult, expectedResult)
    }

    @Test
    fun `creating packages returns error if the API call fails`() = test {
        // In practice, the API returns more specific error message(s) depending on the error case.
        // In `createPackages()` that error message isn't modified further, so here we use a mock message instead.
        val errorMessage = "error message"
        val response = WooPayload<Boolean>(WooError(GENERIC_ERROR, UNKNOWN, errorMessage))
        whenever(restClient.createPackages(site = any(), customPackages = any(), predefinedOptions = any()))
                .thenReturn(response)

        val expectedResult = WooResult<Boolean>(response.error)
        val errorRequestResult = store.createPackages(
                site = site,
                customPackages = emptyList(),
                predefinedPackages = sampleListOfOnePredefinedPackage
        )
        assertEquals(errorRequestResult, expectedResult)
        assertEquals(expectedResult.error.message, errorMessage)
    }

    private suspend fun fetchShippingLabelsForOrder(): WooResult<List<WCShippingLabelModel>> {
        val fetchShippingLabelsPayload = WooPayload(sampleShippingLabelApiResponse)
        whenever(restClient.fetchShippingLabelsForOrder(orderId, site)).thenReturn(fetchShippingLabelsPayload)
        whenever(restClient.fetchShippingLabelsForOrder(orderId, errorSite)).thenReturn(WooPayload(error))
        return store.fetchShippingLabelsForOrder(site, orderId)
    }

    private suspend fun refundShippingLabelForOrder(): WooResult<Boolean> {
        val refundShippingLabelPayload = WooPayload(sampleShippingLabelApiResponse)
        whenever(restClient.refundShippingLabelForOrder(
                site, orderId, refundShippingLabelId
        )).thenReturn(refundShippingLabelPayload)

        whenever(restClient.refundShippingLabelForOrder(
                errorSite, orderId, refundShippingLabelId
        )).thenReturn(WooPayload(error))

        return store.refundShippingLabelForOrder(site, orderId, refundShippingLabelId)
    }

    private suspend fun printShippingLabelForOrder(): WooResult<String> {
        val printShippingLabelPayload = WooPayload(samplePrintShippingLabelApiResponse)
        whenever(restClient.printShippingLabels(
                site, printPaperSize, listOf(refundShippingLabelId)
        )).thenReturn(printShippingLabelPayload)

        whenever(restClient.printShippingLabels(
                errorSite, printPaperSize, listOf(refundShippingLabelId)
        )).thenReturn(WooPayload(error))

        return store.printShippingLabel(site, printPaperSize, refundShippingLabelId)
    }

    private suspend fun verifyAddress(type: ShippingLabelAddress.Type): WooResult<WCAddressVerificationResult> {
        val verifyAddressPayload = WooPayload(successfulVerifyAddressApiResponse)
        whenever(restClient.verifyAddress(
                site,
                address,
                ORIGIN
        )).thenReturn(verifyAddressPayload)

        whenever(restClient.verifyAddress(
                site,
                address,
                DESTINATION
        )).thenReturn(WooPayload(error))

        return store.verifyAddress(site, address, type)
    }

    private suspend fun getShippingRates(): WooResult<WCShippingRatesResult> {
        val rates = WooPayload(sampleShippingRatesApiResponse)

        whenever(restClient.getShippingRates(any(), any(), any(), any(), any(), anyOrNull()))
                .thenReturn(rates)

        return store.getShippingRates(site, orderId, originAddress, destAddress, packages, null)
    }

    private suspend fun getPackages(isError: Boolean = false): WooResult<WCPackagesResult> {
        val getPackagesPayload = WooPayload(samplePackagesApiResponse)
        if (isError) {
            whenever(restClient.getPackageTypes(any())).thenReturn(WooPayload(error))
        } else {
            whenever(restClient.getPackageTypes(any())).thenReturn(getPackagesPayload)
        }
        return store.getPackageTypes(site)
    }

    private suspend fun getAccountSettings(isError: Boolean = false): WooResult<WCShippingAccountSettings> {
        if (isError) {
            whenever(restClient.getAccountSettings(any())).thenReturn(WooPayload(error))
        } else {
            val accountSettingsPayload = WooPayload(sampleAccountSettingsApiResponse)
            whenever(restClient.getAccountSettings(any())).thenReturn(accountSettingsPayload)
        }
        return store.getAccountSettings(site)
    }

    private suspend fun fetchSLCreationEligibility(
        canCreatePackage: Boolean,
        canCreatePaymentMethod: Boolean,
        canCreateCustomsForm: Boolean,
        isEligible: Boolean = false,
        isError: Boolean = false
    ): WooResult<WCShippingLabelCreationEligibility> {
        if (isError) {
            whenever(restClient.checkShippingLabelCreationEligibility(any(), any(), any(), any(), any()))
                    .thenReturn(WooPayload(error))
        } else {
            val result = SLCreationEligibilityApiResponse(
                    isEligible = isEligible,
                    reason = if (isEligible) null else "reason"
            )
            whenever(restClient.checkShippingLabelCreationEligibility(any(), any(), any(), any(), any()))
                    .thenReturn(WooPayload(result))
        }
        return store.fetchShippingLabelCreationEligibility(
                site = site,
                orderId = orderId,
                canCreatePackage = canCreatePackage,
                canCreatePaymentMethod = canCreatePaymentMethod,
                canCreateCustomsForm = canCreateCustomsForm
        )
    }
}
