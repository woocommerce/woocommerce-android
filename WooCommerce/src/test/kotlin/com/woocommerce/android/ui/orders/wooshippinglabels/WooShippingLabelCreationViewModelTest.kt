package com.woocommerce.android.ui.orders.wooshippinglabels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.HazmatState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.NavigateToHazmatFormEdit
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.NavigateToRefundRequest
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.NavigateToUPSDAPTermsOfService
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.OpenLearnMoreScreen
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.OpenShippingLabelFile
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.OpenUrl
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.ShippingRatesState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.WooShippingViewState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.WooShippingViewState.DataState
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressValidationHelper
import com.woocommerce.android.ui.orders.wooshippinglabels.address.ObserveShippingLabelNotice
import com.woocommerce.android.ui.orders.wooshippinglabels.address.destination.VerifyDestinationAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.address.origin.ObserveOriginAddresses
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeBannerUiState
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.CreateDefaultCustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.ShouldRequireCustomsForm
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.WooShippingCustomsValidator
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AccountSettingsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodOptions
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PurchasedLabelData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingLabelPaperSize
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.ObserveShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.printing.FetchShippingLabelFile
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel.Option
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.domain.GetShippingRates
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.CarrierUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateOption
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateOptionUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingSortOption
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import java.io.File
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingLabelCreationViewModelTest : BaseUnitTest() {
    private val orderId = 1L
    private val defaultShippableItems = List(3) {
        ShippableItemModel(
            itemId = it.toLong(),
            productId = it.toLong(),
            title = "Product $it",
            price = BigDecimal(it),
            quantity = it.toFloat(),
            weight = it + 0.01f,
            currency = "USD",
            imageUrl = "https://example.com/image.jpg",
            width = it.toFloat(),
            height = it.toFloat(),
            length = it.toFloat()
        )
    }
    private val defaultShipments = listOf(ShipmentUIModel(localId = "0", items = defaultShippableItems))
    private val defaultShippingLines = List(3) {
        Order.ShippingLine(
            methodTitle = "Shipping Line $it",
            total = BigDecimal(it),
            methodId = it.toString(),
            itemId = it.toLong(),
            totalTax = BigDecimal.ZERO,
            taxes = emptyList()
        )
    }
    private val defaultOriginAddresses = listOf(
        OriginShippingAddress(
            firstName = "first name",
            lastName = "last name",
            company = "Company",
            phone = "",
            address1 = "A huge address that should be truncated",
            address2 = "",
            city = "San Francisco",
            postcode = "",
            email = "email",
            country = "USA",
            state = "California",
            id = "id_1",
            isDefault = false,
            isVerified = true
        )
    )

    private val defaultShipToAddress = Address.EMPTY.copy(
        firstName = "first name",
        lastName = "last name",
        country = Location("US", "US"),
        state = AmbiguousLocation.Raw("AA"),
        city = "city",
        postcode = "postcode",
        address1 = "1278 24st Perito AVE"
    )
    private val defaultAccountSettings = AccountSettingsModel(
        storeOptions = StoreOptionsModel(
            weightUnit = "kg",
            currencySymbol = "$",
            dimensionUnit = "cm",
            originCountry = "US"
        ),
        paymentMethodOptions = PaymentMethodOptions(
            selectedPaymentId = null,
            paymentMethods = emptyList(),
            addPaymentMethodUrl = "https://example.com/add-payment-method",
            emailReceipts = false
        ),
        canManagePayments = false,
        canEditSettings = true,
        storeOwnerName = "",
        storeOwnerUsername = "",
        paperSize = WooShippingLabelPaperSize.LABEL,
        lastOrderCompleted = false
    )

    private val defaultPackageData = PackageData(
        id = "1",
        name = "Package 1",
        dimensions = "10 x 10 x 10",
        weight = "10",
        isSelected = false,
        isLetter = false
    )

    private val defaultCarrier = CarrierUI(
        carrier = WooShippingCarrier.UPS,
        name = "UPS",
    )

    private val defaultShippingRate = WooShippingRateModel(
        packageId = "1",
        shipmentId = "1",
        rateId = "1",
        serviceId = "1",
        carrierId = WooShippingCarrier.UPS.carrierIds.first(),
        serviceName = "Default",
        deliveryDays = 1,
        price = BigDecimal(12),

        option = Option.DEFAULT,
        carrier = defaultCarrier.carrier,
        hasFreePickup = true,
        isTrackingEnabled = true,
        insurance = null,
        deliveryDate = null,
        isDeliveryDateGuaranteed = false,
        isSelected = false,
        listRate = BigDecimal.TEN,
        retailRate = BigDecimal.TEN
    )

    private val defaultShippableItemUI = ShippingRateOptionUI(
        option = ShippingRateOption.DEFAULT,
        optionName = "",
        fee = BigDecimal.ZERO,
        formattedFee = "",
        rate = defaultShippingRate,
        feeDescription = "fee description"
    )

    private val shippingLabelModel = ShippingLabelModel(
        labelId = 1,
        tracking = "",
        refundableAmount = BigDecimal.ZERO,
        status = ShippingLabelStatus.UNKNOWN,
        created = null,
        carrierId = "",
        serviceName = "",
        commercialInvoiceUrl = "",
        isCommercialInvoiceSubmittedElectronically = false,
        packageName = "",
        isLetter = false,
        productNames = emptyList(),
        productIds = emptyList(),
        shipmentId = "0",
        receiptItemId = 0L,
        createdDate = null,
        mainReceiptId = 0L,
        rate = BigDecimal.ZERO,
        currency = "",
        expiryDate = 0L,
        usedDate = 0L,
        refund = null,
    )

    private val defaultShippingRates = mapOf(
        defaultCarrier to defaultShippableItems.map {
            ShippingRateUI(
                title = defaultCarrier.name,
                formattedBasePrice = "10",
                shippingRateIncludedOptions = emptyList(),
                formattedEstimatedDays = "1 day",
                options = mapOf(ShippingRateOption.DEFAULT to defaultShippableItemUI),
                selectedOption = ShippingRateOption.DEFAULT,
                additionalSelectedOptions = emptyList()
            )
        }
    )

    private val orderDetailRepository: OrderDetailRepository = mock {
        onBlocking { getOrderById(any()) } doReturn OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
    }
    private val getShipments: GetShipments = mock {
        onBlocking { invoke(any()) } doReturn defaultShipments
    }
    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(any<BigDecimal>(), any(), any()) } doAnswer {
            val amount = it.getArgument(0) as BigDecimal
            "$ ${amount.toPlainString()}"
        }
    }
    private val savedState: SavedStateHandle =
        WooShippingLabelCreationFragmentArgs(orderId = orderId, shipmentId = 0).toSavedStateHandle()

    private val shouldRequireCustomsForm: ShouldRequireCustomsForm = mock {
        on { invoke(any()) } doReturn false
    }

    private val addressValidationHelper: AddressValidationHelper = mock {
        on { canFetchShippingRates(any()) } doReturn true
    }

    private val observeOriginAddresses: ObserveOriginAddresses = mock {
        on { invoke() } doReturn flowOf(defaultOriginAddresses)
    }
    private val getShippingRates: GetShippingRates = mock {
        onBlocking {
            invoke(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        } doReturn Result.success(defaultShippingRates)
    }
    private val purchaseShippingLabel: PurchaseShippingLabel = mock {
        onBlocking {
            invoke(any(), any(), any(), any(), any(), any(), any(), any(), any(), isNull(), isNull())
        } doReturn Result.success(
            PurchasedLabelData(
                labels = listOf(shippingLabelModel.copy(status = ShippingLabelStatus.PURCHASED)),
                origin = emptyMap(),
                destination = emptyMap(),
                rates = emptyMap()
            )
        )
    }
    private val markOrderAsComplete: MarkOrderAsComplete = mock()
    private val observeAccountSettings: ObserveAccountSettings = mock {
        on { invoke() } doReturn flowOf(defaultAccountSettings)
    }
    private val verifyDestinationAddress: VerifyDestinationAddress = mock {
        onBlocking { invoke(orderId) } doReturn Result.success(DestinationShippingAddress(defaultShipToAddress, true))
    }
    private val observeShippingLabelNotice: ObserveShippingLabelNotice = mock {
        on { invoke(any(), any(), any(), any()) } doReturn flowOf(null)
    }
    private val customsValidator: WooShippingCustomsValidator = mock()
    private val fetchShippingLabelFile: FetchShippingLabelFile = mock()
    private val observeShippingLabelStatus: ObserveShippingLabelStatus = mock()
    private val file: File = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val createDefaultCustomsData = CreateDefaultCustomsData(mock())

    private lateinit var sut: WooShippingLabelCreationViewModel

    fun createViewModel() {
        sut = WooShippingLabelCreationViewModel(
            orderDetailRepository = orderDetailRepository,
            getShipments = getShipments,
            currencyFormatter = currencyFormatter,
            observeOriginAddresses = observeOriginAddresses,
            fetchOriginAddresses = mock(),
            getShippingRates = getShippingRates,
            purchaseShippingLabel = purchaseShippingLabel,
            markOrderAsComplete = markOrderAsComplete,
            observeAccountSettings = observeAccountSettings,
            fetchAccountSettings = mock(),
            addressValidationHelper = addressValidationHelper,
            verifyDestinationAddress = verifyDestinationAddress,
            observeShippingLabelNotice = observeShippingLabelNotice,
            createDefaultCustomsData = createDefaultCustomsData,
            shouldRequireCustoms = shouldRequireCustomsForm,
            customsValidator = customsValidator,
            fetchShippingLabelFile = fetchShippingLabelFile,
            observeShippingLabelStatus = observeShippingLabelStatus,
            downloadAndPrintInvoiceUseCase = mock(),
            analyticsTracker = analyticsTracker,
            savedState = savedState
        )
    }

    @Test
    fun `when the order NO contains shipping lines, then NO shipping lines summary is displayed`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = emptyList(),
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order

        createViewModel()

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assert(dataState.shippingLines.isEmpty())
    }

    @Test
    fun `when the order contains shipping lines, then shipping lines summary is displayed`() = testBlocking {
        createViewModel()

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assert(dataState.shippingLines.isNotEmpty())
        assertEquals(dataState.shippingLines.size, defaultShippingLines.size)
    }

    @Test
    fun `when the order is not found, then exit`() = testBlocking {
        val order: Order? = null
        whenever(orderDetailRepository.getOrderById(any())) doReturn order

        createViewModel()

        var exit: Exit? = null
        sut.event.observeForever { if (it is Exit) exit = it }

        assertNotNull(exit)
    }

    @Test
    fun `when there are origin addresses, then display the origin addresses`() = testBlocking {
        createViewModel()

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertEquals(dataState.shippingAddresses.first().originAddresses.size, defaultOriginAddresses.size)
        val ids = dataState.shippingAddresses.first().originAddresses.map { it.id }
        assert(ids.containsAll(defaultOriginAddresses.map { it.id }))
    }

    @Test
    fun `when shipping rates succeed then display the shipping rates`() = testBlocking {
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.success(defaultShippingRates)

        createViewModel()

        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertIs<ShippingRatesState.DataState>(
            dataState.shipmentUIList[0].shippingRatesState
        )
    }

    @Test
    fun `when destination address is missing then display missing destination error`() = testBlocking {
        whenever(addressValidationHelper.canFetchShippingRates(any())) doReturn false

        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false

        createViewModel()
        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertIs<ShippingRatesState.MissingInfo>(
            dataState.shipmentUIList[0].shippingRatesState
        )
    }

    @Test
    fun `when weight is zero then display no weight error`() = testBlocking {
        whenever(getShipments(any())) doReturn defaultShipments.map {
            it.copy(items = defaultShippableItems.map { it.copy(weight = 0f) })
        }

        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false

        createViewModel()
        sut.onPackageSelected(defaultPackageData.copy(weight = "0"))

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertIs<ShippingRatesState.MissingInfo>(
            dataState.shipmentUIList[0].shippingRatesState
        )
    }

    @Test
    fun `when shipping rates fail then display an error`() = testBlocking {
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.failure(Exception("Random error"))

        createViewModel()
        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertIs<ShippingRatesState.Error>(
            dataState.shipmentUIList[0].shippingRatesState
        )
    }

    @Test
    fun `when refresh rates is triggered then refresh shipping rates`() = testBlocking {
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.success(defaultShippingRates)

        createViewModel()

        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        sut.onRefreshShippingRates()

        advanceUntilIdle()

        verify(getShippingRates, times(2)).invoke(any(), any(), any(), any(), any(), any(), isNull(), isNull())
    }

    @Test
    fun `when rates sort order is changed then DON'T refresh shipping rates`() = testBlocking {
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.success(defaultShippingRates)

        createViewModel()

        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        sut.onSelectedRateSortOrderChanged(ShippingSortOption.CHEAPEST)

        advanceUntilIdle()

        verify(getShippingRates, times(1))
            .invoke(any(), any(), any(), any(), any(), any(), isNull(), isNull())
    }

    @Test
    fun `when rates sort order is NOT changed then DON'T refresh shipping rates`() = testBlocking {
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.success(defaultShippingRates)

        createViewModel()

        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        sut.onSelectedRateSortOrderChanged(ShippingSortOption.FASTEST)

        advanceUntilIdle()

        verify(getShippingRates, times(1))
            .invoke(any(), any(), any(), any(), any(), any(), isNull(), isNull())
    }

    @Test
    fun `onPackageSelected updates state to DataAvailable when current state is NotSelected`() = testBlocking {
        var currentViewState: WooShippingViewState? = null

        createViewModel()

        advanceUntilIdle()

        sut.viewState.asLiveData().observeForever {
            currentViewState = it
        }

        val initialPackageData = PackageData(
            id = "1",
            name = "Initial Package",
            dimensions = "5 x 5 x 5",
            weight = "0.5",
            isSelected = true,
            isLetter = false
        )

        sut.onPackageSelected(initialPackageData)

        advanceUntilIdle()

        assertThat(currentViewState).isInstanceOf(DataState::class.java)
        val dataState = currentViewState as DataState

        assertThat(dataState.shipmentUIList[0].packageSelectionState)
            .isInstanceOf(PackageSelectionState.DataAvailable::class.java)
        val dataAvailable = dataState.shipmentUIList[0].packageSelectionState as PackageSelectionState.DataAvailable
        assertThat(dataAvailable.selectedPackage).isEqualTo(initialPackageData)
    }

    @Test
    fun `onPackageSelected updates state to DataAvailable when current state is DataAvailable`() = testBlocking {
        var currentViewState: WooShippingViewState? = null

        createViewModel()

        advanceUntilIdle()

        sut.viewState.asLiveData().observeForever {
            currentViewState = it
        }

        val initialPackageData = PackageData(
            id = "1",
            name = "Initial Package",
            dimensions = "5 x 5 x 5",
            weight = "0.5",
            isSelected = true,
            isLetter = false
        )

        sut.onPackageSelected(initialPackageData)

        advanceUntilIdle()

        val newPackageData = PackageData(
            id = "2",
            name = "New Package",
            dimensions = "10 x 10 x 10",
            weight = "1.5",
            isSelected = true,
            isLetter = false
        )

        sut.onPackageSelected(newPackageData)

        advanceUntilIdle()

        assertThat(currentViewState).isInstanceOf(DataState::class.java)
        val dataState = currentViewState as DataState

        assertThat(dataState.shipmentUIList[0].packageSelectionState)
            .isInstanceOf(PackageSelectionState.DataAvailable::class.java)
        val dataAvailable = dataState.shipmentUIList[0].packageSelectionState as PackageSelectionState.DataAvailable
        assertThat(dataAvailable.selectedPackage).isEqualTo(newPackageData)
    }

    @Test
    fun `CustomState is NotRequired when shouldRequireCustomsForm returns false`() = testBlocking {
        var currentViewState: WooShippingViewState? = null

        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false

        createViewModel()

        advanceUntilIdle()

        sut.viewState.asLiveData().observeForever {
            currentViewState = it
        }

        assertThat(currentViewState).isInstanceOf(DataState::class.java)
        val dataState = currentViewState as DataState

        assertThat(dataState.shipmentUIList[0].customsState).isEqualTo(CustomsState.NotRequired)
    }

    @Test
    fun `CustomState is Unavailable when shouldRequireCustomsForm returns true`() = testBlocking {
        var currentViewState: WooShippingViewState? = null

        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false

        createViewModel()

        advanceUntilIdle()

        sut.viewState.asLiveData().observeForever {
            currentViewState = it
        }

        assertThat(currentViewState).isInstanceOf(DataState::class.java)
        val dataState = currentViewState as DataState

        assertThat(dataState.shipmentUIList[0].customsState).isEqualTo(CustomsState.NotRequired)
    }

    @Test
    fun `CustomState is ItnMissing when shouldRequireCustomsForm returns true and ShippingLines exceeds the 2500 limit`() =
        testBlocking {
            var currentViewState: WooShippingViewState? = null

            whenever(shouldRequireCustomsForm.invoke(any())) doReturn true
            whenever(customsValidator.validate(any(), any())) doReturn
                WooShippingCustomsValidator.FormValidationResult.ItnMissing
            whenever(getShipments(any())) doReturn defaultShipments.map {
                it.copy(items = defaultShippableItems.map { it.copy(price = BigDecimal(10000)) })
            }

            createViewModel()

            advanceUntilIdle()

            sut.viewState.asLiveData().observeForever {
                currentViewState = it
            }

            assertThat(currentViewState).isInstanceOf(DataState::class.java)
            val dataState = currentViewState as DataState

            assertThat(dataState.shipmentUIList[0].customsState).isEqualTo(CustomsState.ItnMissing)
        }

    @Test
    fun `when shipping rates loaded, then track loading_success event`() = testBlocking {
        createViewModel()

        sut.viewState.runAndCaptureValues {
            sut.onPackageSelected(defaultPackageData)
            advanceUntilIdle()
        }

        advanceUntilIdle()

        verify(analyticsTracker).track(AnalyticsEvent.WCS_RATE_SELECTION_STEP, mapOf(KEY_STATE to "loading_success"))
    }

    @Test
    fun `when shipping rates fail, then track loading_failed event`() = testBlocking {
        val error = "Random error"
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.failure(Exception(error))

        createViewModel()

        sut.viewState.runAndCaptureValues {
            sut.onPackageSelected(defaultPackageData)
            advanceUntilIdle()
        }

        advanceUntilIdle()

        verify(analyticsTracker).track(
            AnalyticsEvent.WCS_RATE_SELECTION_STEP,
            mapOf(KEY_STATE to "loading_failed", KEY_ERROR to error)
        )
    }

    @Test
    fun `when shipping rate selected, then track selected event`() = testBlocking {
        createViewModel()

        val selectedRate = defaultShippingRates.values.first().first()

        val ratesState = sut.viewState.runAndCaptureValues {
            sut.onPackageSelected(defaultPackageData)
            advanceUntilIdle()
        }.last().let { viewState ->
            (viewState as DataState).shipmentUIList.first().shippingRatesState as ShippingRatesState.DataState
        }

        ratesState.onSelectedShippingRateChanged(selectedRate)

        advanceUntilIdle()

        verify(analyticsTracker).track(AnalyticsEvent.WCS_RATE_SELECTION_STEP, mapOf(KEY_STATE to "selected"))
    }

    @Test
    fun `when onPurchaseShippingLabel fails then show a snackbar`() = testBlocking {
        whenever(
            purchaseShippingLabel(any(), any(), any(), any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.failure(Exception("Random error"))

        createViewModel()

        val selectedRate = defaultShippingRates.values.first().first()

        val ratesState = sut.viewState.runAndCaptureValues {
            sut.onPackageSelected(defaultPackageData)
            advanceUntilIdle()
        }.last().let { viewState ->
            (viewState as DataState).shipmentUIList.first().shippingRatesState as ShippingRatesState.DataState
        }

        ratesState.onSelectedShippingRateChanged(selectedRate)

        advanceUntilIdle()

        sut.onPurchaseShippingLabel()

        assertThat(sut.snackbarData).matches { it?.message == R.string.woo_shipping_labels_purchase_error }
    }

    @Test
    fun `when onPurchaseShippingLabel succeed then track purchase_success`() = testBlocking {
        createViewModel()

        val selectedRate = defaultShippingRates.values.first().first()

        val ratesState = sut.viewState.runAndCaptureValues {
            sut.onPackageSelected(defaultPackageData)
            advanceUntilIdle()
        }.last().let { viewState ->
            (viewState as DataState).shipmentUIList.first().shippingRatesState as ShippingRatesState.DataState
        }

        ratesState.onSelectedShippingRateChanged(selectedRate)

        advanceUntilIdle()

        sut.onPurchaseShippingLabel()

        verify(analyticsTracker).track(AnalyticsEvent.WCS_PURCHASE_STEP, mapOf(KEY_STATE to "purchase_success"))
    }

    @Test
    fun `when onPurchaseShippingLabel fails then track purchase_failed`() = testBlocking {
        val error = "Random error"
        whenever(
            purchaseShippingLabel(any(), any(), any(), any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.failure(Exception(error))

        createViewModel()

        val selectedRate = defaultShippingRates.values.first().first()

        val ratesState = sut.viewState.runAndCaptureValues {
            sut.onPackageSelected(defaultPackageData)
            advanceUntilIdle()
        }.last().let { viewState ->
            (viewState as DataState).shipmentUIList.first().shippingRatesState as ShippingRatesState.DataState
        }

        ratesState.onSelectedShippingRateChanged(selectedRate)

        advanceUntilIdle()

        sut.onPurchaseShippingLabel()

        verify(analyticsTracker).track(
            AnalyticsEvent.WCS_PURCHASE_STEP,
            mapOf(KEY_STATE to "purchase_failed", KEY_ERROR to error)
        )
    }

    @Test
    fun `when shipping label is purchased and markOrderComplete is true, then markOrderAsComplete is called`() =
        testBlocking {
            whenever(markOrderAsComplete(orderId)) doReturn Result.success(Unit)
            whenever(observeShippingLabelStatus(eq(orderId), any())) doReturn flowOf(
                ObserveShippingLabelStatus.ObserveShippingLabelStatusResult(
                    status = ShippingLabelStatus.PURCHASED,
                    shippingLabelModel = shippingLabelModel.copy(status = ShippingLabelStatus.PURCHASED)
                )
            )

            createViewModel()

            sut.onMarkOrderCompleteChange(true)
            val selectedRate = defaultShippingRates.values.first().first()
            val ratesState = sut.viewState.runAndCaptureValues {
                sut.onPackageSelected(defaultPackageData)
                advanceUntilIdle()
            }.last().let { viewState ->
                (viewState as DataState).shipmentUIList.first().shippingRatesState as ShippingRatesState.DataState
            }
            ratesState.onSelectedShippingRateChanged(selectedRate)
            advanceUntilIdle()

            sut.onPurchaseShippingLabel()
            advanceUntilIdle()

            verify(markOrderAsComplete).invoke(orderId)
            verify(analyticsTracker).track(
                stat = AnalyticsEvent.SHIPPING_LABEL_ORDER_FULFILL_SUCCEEDED,
                properties = mapOf(AnalyticsTracker.KEY_IS_REVAMPED_FLOW to true)
            )
        }

    @Test
    fun `when markOrderAsComplete fails, then show a snackbar with error message`() = testBlocking {
        whenever(markOrderAsComplete(orderId)) doReturn Result.failure(Exception("Error marking order as complete"))
        whenever(observeShippingLabelStatus(eq(orderId), any())) doReturn flowOf(
            ObserveShippingLabelStatus.ObserveShippingLabelStatusResult(
                status = ShippingLabelStatus.PURCHASED,
                shippingLabelModel = shippingLabelModel.copy(status = ShippingLabelStatus.PURCHASED)
            )
        )

        createViewModel()

        sut.onMarkOrderCompleteChange(true)

        val selectedRate = defaultShippingRates.values.first().first()
        val ratesState = sut.viewState.runAndCaptureValues {
            sut.onPackageSelected(defaultPackageData)
            advanceUntilIdle()
        }.last().let { viewState ->
            (viewState as DataState).shipmentUIList.first().shippingRatesState as ShippingRatesState.DataState
        }
        ratesState.onSelectedShippingRateChanged(selectedRate)
        advanceUntilIdle()

        sut.onPurchaseShippingLabel()
        advanceUntilIdle()

        verify(markOrderAsComplete).invoke(orderId)

        val showSnackbarEvent = sut.event.captureValues().filterIsInstance<Event.ShowSnackbar>().last()
        assertThat(showSnackbarEvent.message).isEqualTo(R.string.woo_shipping_labels_order_completion_error)

        verify(analyticsTracker).track(
            stat = eq(AnalyticsEvent.SHIPPING_LABEL_ORDER_FULFILL_FAILED),
            properties = eq(mapOf(AnalyticsTracker.KEY_IS_REVAMPED_FLOW to true)),
            errorType = anyOrNull(),
            errorContext = anyOrNull(),
            errorDescription = anyOrNull()
        )
    }

    @Test
    fun `when the view model is created, then get store options from the local preferences and update settings on background`() =
        testBlocking {
            whenever(observeAccountSettings()) doReturn flowOf(null, defaultAccountSettings)

            createViewModel()

            advanceUntilIdle()

            verify(observeAccountSettings).invoke()
        }

    @Test
    fun `when there is no cached store options and API request fails then display error`() = testBlocking {
        whenever(observeAccountSettings()) doReturn flowOf(null)

        createViewModel()

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assertThat(currentViewState).isInstanceOf(WooShippingViewState.Error::class.java)
    }

    @Test
    fun `when there are notices then display the notices`() = testBlocking {
        val notice = NoticeBannerUiState(
            message = R.string.woo_shipping_address_notification_destination_missing,
            type = NoticeType.MISSING_DESTINATION_ADDRESS,
            error = true,
        )

        whenever(observeShippingLabelNotice(any(), any(), any(), any())) doReturn flowOf(notice)

        createViewModel()

        advanceUntilIdle()

        val dataState = sut.viewState.value as DataState
        assertThat(dataState.uiState.noticeBannerUiState?.message).isEqualTo(notice.message)
    }

    @Test
    fun `when there are no notices then do not display the notices`() = testBlocking {
        val notice = null
        whenever(observeShippingLabelNotice(any(), any(), any(), any())) doReturn flowOf(notice)

        createViewModel()

        advanceUntilIdle()

        val dataState = sut.viewState.value as DataState
        assertThat(dataState.uiState.noticeBannerUiState).isEqualTo(notice)
    }

    @Test
    fun `when current label is purchased, then do not display notices`() = testBlocking {
        whenever(getShipments(any())) doReturn listOf(
            ShipmentUIModel(
                localId = "0",
                items = defaultShippableItems,
                label = shippingLabelModel.copy(
                    status = ShippingLabelStatus.PURCHASED
                )
            )
        )

        createViewModel()

        advanceUntilIdle()

        val dataState = sut.viewState.value as DataState
        assertThat(dataState.uiState.noticeBannerUiState).isNull()
    }

    @Test
    fun `when current label is purchased, then show its price`() = testBlocking {
        val label = shippingLabelModel.copy(
            serviceName = "Test Service",
            rate = BigDecimal.TEN,
            status = ShippingLabelStatus.PURCHASED
        )
        whenever(getShipments(any())) doReturn listOf(
            ShipmentUIModel(
                localId = "0",
                items = defaultShippableItems,
                label = label
            )
        )

        createViewModel()

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertThat(dataState.shipmentUIList[0].shipmentCostUI?.formattedBasePrice)
            .isEqualTo(currencyFormatter.formatCurrency(label.rate, label.currency))
        assertThat(dataState.shipmentUIList[0].shipmentCostUI?.formattedTotalPrice)
            .isEqualTo(currencyFormatter.formatCurrency(label.rate, label.currency))
        assertThat(dataState.shipmentUIList[0].shipmentCostUI?.serviceName).isEqualTo(label.serviceName)
    }

    @Test
    fun `when the destination address is missing then verify endpoint should not be called`() = testBlocking {
        whenever(addressValidationHelper.isMissingDestinationAddress(any())) doReturn true

        createViewModel()

        advanceUntilIdle()

        verifyNoInteractions(verifyDestinationAddress)
    }

    @Test
    fun `when the destination address exists then verify endpoint should be called`() = testBlocking {
        whenever(addressValidationHelper.isMissingDestinationAddress(any())) doReturn false

        createViewModel()

        advanceUntilIdle()

        verify(verifyDestinationAddress).invoke(orderId)
    }

    @Test
    fun `HazmatState is NoSelection when no selection happens`() = testBlocking {
        var currentViewState: WooShippingViewState? = null

        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false

        createViewModel()

        advanceUntilIdle()

        sut.viewState.asLiveData().observeForever {
            currentViewState = it
        }

        assertThat(currentViewState).isInstanceOf(DataState::class.java)
        val dataState = currentViewState as DataState

        assertThat(dataState.shipmentUIList[0].hazmatState).isEqualTo(HazmatState.NoSelection)
    }

    @Test
    fun `HazmatState is Declared when onHazmatCategorySelected is called`() = testBlocking {
        var currentViewState: WooShippingViewState? = null

        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false

        createViewModel()
        sut.onHazmatCategorySelected(ShippingLabelHazmatCategory.CLASS_1)

        sut.viewState.asLiveData().observeForever {
            currentViewState = it
        }

        assertThat(currentViewState).isInstanceOf(DataState::class.java)
        val dataState = currentViewState as DataState

        assertThat(dataState.shipmentUIList[0].hazmatState)
            .isEqualTo(HazmatState.Declared(ShippingLabelHazmatCategory.CLASS_1))
    }

    @Test
    fun `when NavigateToHazmatFormEdit is triggered with a selected category, the event contains the expected category value`() =
        testBlocking {
            var event: MultiLiveEvent.Event? = null

            createViewModel()

            sut.onHazmatCategorySelected(ShippingLabelHazmatCategory.CLASS_1)
            sut.onHazmatNoticeClick()

            sut.event.observeForever { event = it }

            assertThat(event).isEqualTo(NavigateToHazmatFormEdit(ShippingLabelHazmatCategory.CLASS_1))
        }

    @Test
    fun `when initialized, show expected item quantity`() = testBlocking {
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false

        val expectedItemQuantity = defaultShippableItems.size

        createViewModel()

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertThat(dataState.totalItems).isEqualTo(expectedItemQuantity)
    }

    @Test
    fun `onPrintShippingLabelClicked triggers OpenShippingLabelFile event`() = testBlocking {
        whenever(getShipments(any())) doReturn listOf(
            ShipmentUIModel(
                localId = "0",
                items = defaultShippableItems,
                label = shippingLabelModel.copy(labelId = 123L)
            )
        )
        whenever(fetchShippingLabelFile(eq(listOf(123)), any())).thenReturn(file)

        createViewModel()

        sut.onPrintShippingLabelClicked()

        verify(fetchShippingLabelFile).invoke(eq(listOf(123)), any())

        var event: OpenShippingLabelFile? = null
        sut.event.observeForever { if (it is OpenShippingLabelFile) event = it }
        assertThat(event).isEqualTo(OpenShippingLabelFile(file))
    }

    @Test
    fun `ViewState starts with LABEL paper size as default`() = testBlocking {
        createViewModel()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertThat(dataState.uiState.paperSizeOption).isEqualTo(WooShippingLabelPaperSize.LABEL)
    }

    @Test
    fun `onLabelPaperSizeOptionSelected updates the ViewState as expected`() = testBlocking {
        createViewModel()

        sut.onLabelPaperSizeOptionSelected(WooShippingLabelPaperSize.LETTER)

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertThat(dataState.uiState.paperSizeOption).isEqualTo(WooShippingLabelPaperSize.LETTER)
    }

    @Test
    fun `onTrackShipmentClicked triggers OpenUrl event`() = testBlocking {
        whenever(getShipments(any())) doReturn listOf(
            ShipmentUIModel(
                localId = "0",
                items = defaultShippableItems,
                label = shippingLabelModel.copy(carrierId = "usps", tracking = "123456"),
            )
        )

        createViewModel()

        var event: OpenUrl? = null
        sut.event.observeForever { if (it is OpenUrl) event = it }

        sut.onTrackShipmentClicked()

        assertThat(event).isEqualTo(OpenUrl("https://tools.usps.com/go/TrackConfirmAction.action?tLabels=123456"))
    }

    @Test
    fun `onSchedulePickUpClicked triggers OpenUrl event`() = testBlocking {
        whenever(getShipments(any())) doReturn listOf(
            ShipmentUIModel(
                localId = "0",
                items = defaultShippableItems,
                label = shippingLabelModel.copy(carrierId = "usps")
            )
        )

        createViewModel()

        var event: OpenUrl? = null
        sut.event.observeForever { if (it is OpenUrl) event = it }

        sut.onSchedulePickUpClicked()

        assertThat(event).isEqualTo(OpenUrl("https://tools.usps.com/schedule-pickup-steps.htm"))
    }

    @Test
    fun `when viewmodel initializes, then default paper size is set`() = testBlocking {
        val expectedPaperSize = WooShippingLabelPaperSize.A4
        given(observeAccountSettings()).willReturn(
            flowOf(
                defaultAccountSettings.copy(paperSize = expectedPaperSize)
            )
        )

        createViewModel()
        advanceUntilIdle()

        val dataState = sut.viewState.value as DataState
        assertThat(dataState.uiState.paperSizeOption).isEqualTo(expectedPaperSize)
    }

    @Test
    fun `onRefundClicked triggers NavigateToRefundRequest event`() = testBlocking {
        val labelId = 123L
        whenever(getShipments(any())) doReturn defaultShipments.toMutableList().apply {
            set(0, defaultShipments.first().copy(label = shippingLabelModel.copy(labelId = labelId)))
        }

        createViewModel()

        var event: NavigateToRefundRequest? = null
        sut.event.observeForever { if (it is NavigateToRefundRequest) event = it }

        sut.onRefundClicked()

        assertThat(event).isEqualTo(NavigateToRefundRequest(orderId, labelId))
    }

    @Test
    fun `when refunded, resets UI state`() = testBlocking {
        val labelId = 123L
        whenever(getShipments(any())) doReturn defaultShipments.toMutableList().apply {
            set(0, defaultShipments.first().copy(label = shippingLabelModel.copy(labelId = labelId)))
        }

        createViewModel()

        // Set up initial state with selected package and rates
        sut.onPackageSelected(defaultPackageData)
        advanceUntilIdle()

        val initialViewState = sut.viewState.value as DataState
        val initialShipment = initialViewState.shipmentUIList[0]

        // Select shipping rate to have something to reset
        val ratesState = initialShipment.shippingRatesState as ShippingRatesState.DataState
        val selectedRate = defaultShippingRates.values.first().first()
        ratesState.onSelectedShippingRateChanged(selectedRate)

        sut.onShippingLabelRefunded(labelId)
        advanceUntilIdle()

        // Verify UI state is reset
        val finalViewState = sut.viewState.value as DataState
        val finalShipment = finalViewState.shipmentUIList[0]

        // Verify package selection is reset
        assertThat(finalShipment.packageSelectionState).isEqualTo(PackageSelectionState.NotSelected)

        // Verify shipping rates are reset
        assertThat(finalShipment.shippingRatesState).isEqualTo(ShippingRatesState.NoAvailable)
    }

    @Test
    fun `onLearnMoreClicked triggers OpenLearnMoreScreen event`() = testBlocking {
        createViewModel()

        var event: OpenLearnMoreScreen? = null
        sut.event.observeForever { if (it is OpenLearnMoreScreen) event = it }

        sut.onLearnMoreClicked()

        assertThat(event).isEqualTo(OpenLearnMoreScreen)
    }

    @Test
    fun `when viewmodel initializes, then tracks form shown event`() = testBlocking {
        createViewModel()
        advanceUntilIdle()
        verify(analyticsTracker).track(eq(AnalyticsEvent.WCS_CREATE_SHIPPING_LABEL_FORM_SHOWN), any())
    }

    @Test
    fun `after splitting a shipment, then retain the state of previous shipments`() = testBlocking {
        createViewModel()
        advanceUntilIdle()

        sut.onPackageSelected(defaultPackageData)
        sut.onShipmentSplit(
            listOf(
                ShipmentUIModel("0", items = defaultShippableItems.take(2)),
                ShipmentUIModel("1", items = defaultShippableItems.takeLast(1)),
            )
        )

        val afterSplitState = sut.viewState.value as DataState
        assertThat(afterSplitState.shipmentUIList.first().packageSelectionState)
            .matches { it is PackageSelectionState.DataAvailable && it.selectedPackage == defaultPackageData }
        assertThat(afterSplitState.shipmentUIList.last().packageSelectionState)
            .isEqualTo(PackageSelectionState.NotSelected)
    }

    @Test
    fun `after merging shipments, then reset state to default values`() = testBlocking {
        whenever(getShipments(any())) doReturn listOf(
            ShipmentUIModel("0", items = defaultShippableItems.take(2)),
            ShipmentUIModel("1", items = defaultShippableItems.takeLast(1)),
        )

        createViewModel()
        advanceUntilIdle()

        sut.onPackageSelected(defaultPackageData)
        sut.onShipmentSplit(defaultShipments)

        val afterSplitState = sut.viewState.value as DataState
        assertThat(afterSplitState.shipmentUIList.first().packageSelectionState)
            .isEqualTo(PackageSelectionState.NotSelected)
    }

    @Test
    fun `given selected payment method, when the screen is loaded, then show the payment method`() = testBlocking {
        val accountSettings = defaultAccountSettings.copy(
            paymentMethodOptions = PaymentMethodOptions(
                selectedPaymentId = 1,
                paymentMethods = listOf(
                    PaymentMethodModel(
                        paymentMethodId = 1,
                        name = "Visa",
                        cardType = "VISA",
                        cardDigits = "1234",
                        expiry = "12/25"
                    )
                ),
                addPaymentMethodUrl = "https://example.com/add-payment-method",
                emailReceipts = false
            )
        )
        given(observeAccountSettings()).willReturn(flowOf(accountSettings))

        createViewModel()
        advanceUntilIdle()

        val viewState = sut.viewState.value as DataState
        assertThat(viewState.paymentsSectionUI.selectedPaymentMethod).isNotNull
        assertThat(viewState.paymentsSectionUI.selectedPaymentMethod)
            .isEqualTo(accountSettings.paymentMethodOptions.selectedPaymentMethod)
    }

    @Test
    fun `given no selected payment method, when the screen is loaded, then show no payment method`() = testBlocking {
        val accountSettings = defaultAccountSettings.copy(
            paymentMethodOptions = PaymentMethodOptions(
                selectedPaymentId = null,
                paymentMethods = emptyList(),
                addPaymentMethodUrl = "https://example.com/add-payment-method",
                emailReceipts = false
            )
        )
        given(observeAccountSettings()).willReturn(flowOf(accountSettings))

        createViewModel()
        advanceUntilIdle()

        val viewState = sut.viewState.value as DataState
        assertThat(viewState.paymentsSectionUI.selectedPaymentMethod).isNull()
    }

    @Test
    fun `when purchase shipping label fails with UPSDAP_MISSING_TOS_ERROR_CODE, then navigate to UPS DAP terms of service`() =
        testBlocking {
            val wooError = WooError(
                type = WooErrorType.API_ERROR,
                original = GenericErrorType.UNKNOWN,
                apiErrorCode = WooShippingLabelCreationViewModel.UPSDAP_MISSING_TOS_ERROR_CODE,
                message = "Missing UPS DAP terms of service acceptance"
            )
            val wooException = WooException(wooError)
            whenever(
                purchaseShippingLabel(any(), any(), any(), any(), any(), any(), any(), any(), any(), isNull(), isNull())
            ) doReturn Result.failure(wooException)

            createViewModel()

            val selectedRate = defaultShippingRates.values.first().first()

            val viewState = sut.viewState.runAndCaptureValues {
                sut.onPackageSelected(defaultPackageData)
                advanceUntilIdle()
            }.last() as DataState
            val ratesState = viewState.shipmentUIList.first().shippingRatesState as ShippingRatesState.DataState

            ratesState.onSelectedShippingRateChanged(selectedRate)

            advanceUntilIdle()

            var event: NavigateToUPSDAPTermsOfService? = null
            sut.event.observeForever { if (it is NavigateToUPSDAPTermsOfService) event = it }

            sut.onPurchaseShippingLabel()

            assertThat(event).isNotNull
            assertThat(event?.originAddress).isEqualTo(defaultOriginAddresses.first())
        }

    @Test
    fun `when UPS terms accepted, then fetch new rates and reselect same rate as before`() = testBlocking {
        val firstRatesFetchResult = mapOf(
            defaultCarrier to defaultShippingRates[defaultCarrier]!!.map {
                it.copy(
                    options = mapOf(
                        ShippingRateOption.DEFAULT to it.options.getValue(ShippingRateOption.DEFAULT),
                        ShippingRateOption.ADDITIONAL_HANDLING to it.options.getValue(ShippingRateOption.DEFAULT)
                    )
                )
            }
        )
        val rateToBeSelected = defaultShippableItemUI.copy(
            rate = defaultShippingRate.copy(rateId = "2")
        )
        val secondRatesFetchResult = mapOf(
            defaultCarrier to defaultShippingRates[defaultCarrier]!!.map {
                it.copy(
                    options = mapOf(
                        ShippingRateOption.DEFAULT to rateToBeSelected,
                        ShippingRateOption.ADDITIONAL_HANDLING to rateToBeSelected
                    )
                )
            }
        )
        whenever(getShippingRates(any(), any(), any(), any(), any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.success(firstRatesFetchResult))
            .thenReturn(Result.success(secondRatesFetchResult))

        createViewModel()
        advanceUntilIdle()

        val firstViewState = sut.viewState.runAndCaptureValues {
            // Select a package
            sut.onPackageSelected(defaultPackageData)
            advanceUntilIdle()
        }.last() as DataState

        val shippingRateState = firstViewState.shipmentUIList.first().shippingRatesState as ShippingRatesState.DataState

        val viewState = sut.viewState.runAndCaptureValues {
            // Select a shipping rate
            val selectedRate = firstRatesFetchResult.values.first().first()
            shippingRateState.onSelectedShippingRateChanged(selectedRate)
            shippingRateState.onSelectedRateOptionChanged(ShippingRateOption.ADDITIONAL_HANDLING, true)
            advanceUntilIdle()

            // Simulate UPS terms acceptance
            sut.onUPSTermsAccepted()
            advanceUntilIdle()
        }.last() as DataState

        // The rates should be fetched again and the same rate should be selected
        verify(getShippingRates, times(2)).invoke(any(), any(), any(), any(), any(), any(), anyOrNull(), anyOrNull())
        val shipment = viewState.shipmentUIList[0]
        assertThat(shipment.shippingRatesState)
            .isInstanceOf(ShippingRatesState.DataState::class.java)
        val ratesState = shipment.shippingRatesState as ShippingRatesState.DataState
        assertThat(ratesState.selectedRate!!.selectedRateOption).isEqualTo(rateToBeSelected)
        assertThat(ratesState.selectedRate!!.additionalSelectedOptions)
            .isEqualTo(listOf(ShippingRateOption.ADDITIONAL_HANDLING))
    }

    @Test
    fun `when UPS terms are accepted, then continue purchase`() = testBlocking {
        whenever(getShippingRates(any(), any(), any(), any(), any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.success(defaultShippingRates))

        createViewModel()
        advanceUntilIdle()

        val firstViewState = sut.viewState.runAndCaptureValues {
            // Select a package
            sut.onPackageSelected(defaultPackageData)
            advanceUntilIdle()
        }.last() as DataState

        val shippingRateState = firstViewState.shipmentUIList.first().shippingRatesState as ShippingRatesState.DataState

        val viewState = sut.viewState.runAndCaptureValues {
            val selectedRate = defaultShippingRates.values.first().first()
            shippingRateState.onSelectedShippingRateChanged(selectedRate)
            advanceUntilIdle()

            sut.onUPSTermsAccepted()
            advanceUntilIdle()
        }.last() as DataState

        viewState.shipmentUIList.first().let {
            assertThat(it.isReadOnly).isTrue()
        }
    }

    @Test
    fun `when order status is Completed, then calculate isOrderAlreadyCompleted correctly`() = testBlocking {
        val completedOrder = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            status = Order.Status.Completed,
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn completedOrder

        createViewModel()
        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertThat(dataState.purchaseSectionUI.isOrderAlreadyCompleted).isTrue()
    }

    @Test
    fun `when order status is not Completed, then calculate isOrderAlreadyCompleted correctly`() = testBlocking {
        val processingOrder = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            status = Order.Status.Processing,
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn processingOrder

        createViewModel()
        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertThat(dataState.purchaseSectionUI.isOrderAlreadyCompleted).isFalse()
    }
}
