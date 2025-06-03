package com.woocommerce.android.ui.orders.wooshippinglabels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.HazmatState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.OpenLearnMoreScreen
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.OpenShippingLabelFile
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.OpenUrl
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState.DataAvailable
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.StartHazmatFormEdit
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.StartRefundRequest
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.WooShippingViewState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.WooShippingViewState.DataState
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressValidationHelper
import com.woocommerce.android.ui.orders.wooshippinglabels.address.ObserveShippingLabelNotice
import com.woocommerce.android.ui.orders.wooshippinglabels.address.destination.VerifyDestinationAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.address.origin.ObserveOriginAddresses
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeBannerUiState
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeType
import com.woocommerce.android.ui.orders.wooshippinglabels.components.WooShippingLabelPaperSize
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.ShouldRequireCustomsForm
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.ShouldRequireITN
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.printing.FetchShippingLabelFile
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel.Option
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.domain.GetShippingRates
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.CarrierUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateOptionUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingSortOption
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
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
    private val defaultShipments = listOf(ShipmentUIModel(id = "0", items = defaultShippableItems))
    private val defaultShippingLines = List(3) {
        Order.ShippingLine(
            methodTitle = "Shipping Line $it",
            total = BigDecimal(it),
            methodId = it.toString(),
            itemId = it.toLong(),
            totalTax = BigDecimal.ZERO,
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
    private val defaultStoreOptions = StoreOptionsModel(
        weightUnit = "kg",
        currencySymbol = "$",
        dimensionUnit = "cm",
        originCountry = "US"
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
        carrierId = "1",
        serviceName = "Default",
        deliveryDays = 1,
        price = BigDecimal(12),
        discount = BigDecimal.ZERO,
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
        title = defaultShippableItems[0].title,
        formatedPrice = "$ ${defaultShippableItems[0].price}",
        formattedFee = "",
        formattedEstimatedDays = "1 day",
        shippingRateOptions = emptyList(),
        option = Option.DEFAULT,
        rate = defaultShippingRate,
        feeDescription = "fee description",
        formattedOptionName = Option.DEFAULT.name
    )

    private val defaultShippingRates = mapOf(
        defaultCarrier to defaultShippableItems.map {
            ShippingRateUI(
                options = mapOf(Option.DEFAULT to defaultShippableItemUI),
                selectedOption = defaultShippableItemUI
            )
        }
    )

    private val orderDetailRepository: OrderDetailRepository = mock()
    private val getShipments: GetShipments = mock()
    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(any<BigDecimal>(), any(), any()) } doAnswer {
            val amount = it.getArgument(0) as BigDecimal
            "$ ${amount.toPlainString()}"
        }
    }
    private val savedState: SavedStateHandle =
        WooShippingLabelCreationFragmentArgs(orderId = orderId).toSavedStateHandle()

    private val shouldRequireCustomsForm: ShouldRequireCustomsForm = mock {
        on { invoke(any()) } doReturn true
    }

    private val addressValidationHelper: AddressValidationHelper = mock {
        on { canFetchShippingRates(any()) } doReturn true
    }

    private val observeOriginAddresses: ObserveOriginAddresses = mock()
    private val getShippingRates: GetShippingRates = mock()
    private val purchaseShippingLabel: PurchaseShippingLabel = mock()
    private val observeStoreOptions: ObserveStoreOptions = mock()
    private val verifyDestinationAddress: VerifyDestinationAddress = mock()
    private val observeShippingLabelNotice: ObserveShippingLabelNotice = mock()
    private val shouldRequireITN: ShouldRequireITN = mock {
        on { invoke(any(), any()) } doReturn false
    }
    private val fetchShippingLabelFile: FetchShippingLabelFile = mock()
    private val file: File = mock()

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
            observeStoreOptions = observeStoreOptions,
            fetchAccountSettings = mock(),
            addressValidationHelper = addressValidationHelper,
            verifyDestinationAddress = verifyDestinationAddress,
            observeShippingLabelNotice = observeShippingLabelNotice,
            shouldRequireCustoms = shouldRequireCustomsForm,
            shouldRequireITN = shouldRequireITN,
            fetchShippingLabelFile = fetchShippingLabelFile,
            observeShippingLabelStatus = mock(),
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
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

        createViewModel()

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assert(dataState.shippingLines.isEmpty())
    }

    @Test
    fun `when the order contains shipping lines, then shipping lines summary is displayed`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

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
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

        createViewModel()

        var exit: Exit? = null
        sut.event.observeForever { if (it is Exit) exit = it }

        assertNotNull(exit)
    }

    @Test
    fun `when there are no origin addresses, then show an error`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(observeOriginAddresses()) doReturn flowOf(emptyList())
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

        createViewModel()

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is WooShippingViewState.Error)
    }

    @Test
    fun `when there are origin addresses, then display the origin addresses`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

        createViewModel()

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertEquals(dataState.shippingAddresses.originAddresses.size, defaultOriginAddresses.size)
        val ids = dataState.shippingAddresses.originAddresses.map { it.id }
        assert(ids.containsAll(defaultOriginAddresses.map { it.id }))
    }

    @Test
    fun `when shipping rates succeed then display the shipping rates`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.success(defaultShippingRates)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

        createViewModel()

        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertIs<WooShippingLabelCreationViewModel.ShippingRatesState.DataState>(
            dataState.shipmentUIList[0].shippingRatesState
        )
    }

    @Test
    fun `when destination address is missing then display missing destination error`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(addressValidationHelper.canFetchShippingRates(any())) doReturn false
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false

        createViewModel()
        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertIs<WooShippingLabelCreationViewModel.ShippingRatesState.MissingInfo>(
            dataState.shipmentUIList[0].shippingRatesState
        )
    }

    @Test
    fun `when weight is zero then display no weight error`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments.map {
            it.copy(items = defaultShippableItems.map { it.copy(weight = 0f) })
        }
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false

        createViewModel()
        sut.onPackageSelected(defaultPackageData.copy(weight = "0"))

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertIs<WooShippingLabelCreationViewModel.ShippingRatesState.MissingInfo>(
            dataState.shipmentUIList[0].shippingRatesState
        )
    }

    @Test
    fun `when shipping rates fail then display an error`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.failure(Exception("Random error"))
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

        createViewModel()
        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertIs<WooShippingLabelCreationViewModel.ShippingRatesState.Error>(
            dataState.shipmentUIList[0].shippingRatesState
        )
    }

    @Test
    fun `when refresh rates is triggered then refresh shipping rates`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.success(defaultShippingRates)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

        createViewModel()

        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        sut.onRefreshShippingRates()

        advanceUntilIdle()

        verify(getShippingRates, times(2)).invoke(any(), any(), any(), any(), any(), any(), isNull(), isNull())
    }

    @Test
    fun `when rates sort order is changed then DON'T refresh shipping rates`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.success(defaultShippingRates)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

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
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn false
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.success(defaultShippingRates)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

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
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

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

        assertThat(dataState.shipmentUIList[0].packageSelectionState).isInstanceOf(DataAvailable::class.java)
        val dataAvailable = dataState.shipmentUIList[0].packageSelectionState as DataAvailable
        assertThat(dataAvailable.selectedPackage).isEqualTo(initialPackageData)
    }

    @Test
    fun `onPackageSelected updates state to DataAvailable when current state is DataAvailable`() = testBlocking {
        var currentViewState: WooShippingViewState? = null
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

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

        assertThat(dataState.shipmentUIList[0].packageSelectionState).isInstanceOf(DataAvailable::class.java)
        val dataAvailable = dataState.shipmentUIList[0].packageSelectionState as DataAvailable
        assertThat(dataAvailable.selectedPackage).isEqualTo(newPackageData)
    }

    @Test
    fun `CustomState is NotRequired when shouldRequireCustomsForm returns false`() = testBlocking {
        var currentViewState: WooShippingViewState? = null
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
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
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
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
            val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
                shippingLines = defaultShippingLines
            )
            whenever(orderDetailRepository.getOrderById(any())) doReturn order
            whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
            whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
            whenever(shouldRequireCustomsForm.invoke(any())) doReturn true
            whenever(shouldRequireITN.invoke(any(), any())) doReturn true
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
    fun `when onPurchaseShippingLabel fails then show a snackbar`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
        whenever(
            purchaseShippingLabel(any(), any(), any(), any(), any(), any(), any(), any(), isNull(), isNull())
        ) doReturn Result.failure(Exception("Random error"))

        createViewModel()

        val selectedRate = defaultShippingRates.values.first().first()

        sut.onPackageSelected(defaultPackageData)
        sut.onSelectedSippingRateChanged(selectedRate)

        advanceUntilIdle()

        sut.onPurchaseShippingLabel()

        assertThat(sut.snackbarData).matches { it?.message == R.string.woo_shipping_labels_purchase_error }
    }

    @Test
    fun `when the view model is created, then get store options from the local preferences and update settings on background`() =
        testBlocking {
            val order = OrderTestUtils.generateTestOrder(orderId = orderId)

            whenever(orderDetailRepository.getOrderById(any())) doReturn order
            whenever(getShipments(any())) doReturn defaultShipments
            whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
            whenever(observeStoreOptions()) doReturn flowOf(null, defaultStoreOptions)

            createViewModel()

            advanceUntilIdle()

            verify(observeStoreOptions).invoke()
        }

    @Test
    fun `when there is no cached store options and API request fails then display error`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(null)

        createViewModel()

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assertThat(currentViewState).isInstanceOf(WooShippingViewState.Error::class.java)
    }

    @Test
    fun `when address selection is collapsed then changes shipment details are allowed`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(null)

        createViewModel()

        advanceUntilIdle()
        // Collapse shipment details and select address
        var changeAccepted = sut.onShipmentDetailsExpandedChange(false)
        assertThat(changeAccepted).isTrue()
        changeAccepted = sut.onSelectAddressExpandedChange(false)
        assertThat(changeAccepted).isTrue()

        // Check all changes are accepted
        changeAccepted = sut.onShipmentDetailsExpandedChange(false)
        assertThat(changeAccepted).isTrue()
        changeAccepted = sut.onShipmentDetailsExpandedChange(true)
        assertThat(changeAccepted).isTrue()
    }

    @Test
    fun `when address selection is expanded then prevent any change on the shipment details`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(null)

        createViewModel()

        advanceUntilIdle()
        // Expand shipment details and select address
        var changeAccepted = sut.onShipmentDetailsExpandedChange(true)
        assertThat(changeAccepted).isTrue()

        changeAccepted = sut.onSelectAddressExpandedChange(true)
        assertThat(changeAccepted).isTrue()

        // Check no changes are accepted when select address is expanded
        changeAccepted = sut.onShipmentDetailsExpandedChange(false)
        assertThat(changeAccepted).isFalse()
        changeAccepted = sut.onShipmentDetailsExpandedChange(true)
        assertThat(changeAccepted).isFalse()
    }

    @Test
    fun `when a bottom sheet is expanded then the back gesture closes the sheet`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(null)

        createViewModel()

        advanceUntilIdle()
        // Expand shipment details and select address
        sut.onShipmentDetailsExpandedChange(true)
        sut.onSelectAddressExpandedChange(true)

        // Close address selection
        var shouldNavigateBack = sut.allowBackNavigation()
        assertThat(shouldNavigateBack).isFalse()

        // Close shipment details
        shouldNavigateBack = sut.allowBackNavigation()
        assertThat(shouldNavigateBack).isFalse()

        // Navigate back
        shouldNavigateBack = sut.allowBackNavigation()
        assertThat(shouldNavigateBack).isTrue()
    }

    @Test
    fun `when shipment details is expanded then the back gesture closes the sheet`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(null)

        createViewModel()

        advanceUntilIdle()
        sut.onShipmentDetailsExpandedChange(true)

        // Close shipment details
        var shouldNavigateBack = sut.allowBackNavigation()
        assertThat(shouldNavigateBack).isFalse()

        // Navigate back
        shouldNavigateBack = sut.allowBackNavigation()
        assertThat(shouldNavigateBack).isTrue()
    }

    @Test
    fun `when there is no bottom sheet expanded, then on back navigates to the previous screen`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(null)

        createViewModel()

        advanceUntilIdle()

        // Navigate back
        val shouldNavigateBack = sut.allowBackNavigation()
        assertThat(shouldNavigateBack).isTrue()
    }

    @Test
    fun `when there are notices then display the notices`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)
        val notice = NoticeBannerUiState(
            message = R.string.woo_shipping_address_notification_destination_missing,
            type = NoticeType.MISSING_DESTINATION_ADDRESS,
            error = true,
        )

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
        whenever(observeShippingLabelNotice(any(), any(), any(), any())) doReturn flowOf(notice)

        createViewModel()

        advanceUntilIdle()

        val dataState = sut.viewState.value as DataState
        assertThat(dataState.uiState.noticeBannerUiState?.message).isEqualTo(notice.message)
    }

    @Test
    fun `when there are no notices then do not display the notices`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)
        val notice = null

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
        whenever(observeShippingLabelNotice(any(), any(), any(), any())) doReturn flowOf(notice)

        createViewModel()

        advanceUntilIdle()

        val dataState = sut.viewState.value as DataState
        assertThat(dataState.uiState.noticeBannerUiState).isEqualTo(notice)
    }

    @Test
    fun `when the destination address is missing then verify endpoint should not be called`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
        whenever(addressValidationHelper.isMissingDestinationAddress(any())) doReturn true

        createViewModel()

        advanceUntilIdle()

        verifyNoInteractions(verifyDestinationAddress)
    }

    @Test
    fun `when the destination address exists then verify endpoint should be called`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
        whenever(addressValidationHelper.isMissingDestinationAddress(any())) doReturn false

        createViewModel()

        advanceUntilIdle()

        verify(verifyDestinationAddress).invoke(orderId)
    }

    @Test
    fun `HazmatState is NoSelection when no selection happens`() = testBlocking {
        var currentViewState: WooShippingViewState? = null
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
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
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
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
    fun `when StartHazmatFormEdit is triggered with a selected category, the event contains the expected category value`() =
        testBlocking {
            var event: MultiLiveEvent.Event? = null
            val order = OrderTestUtils.generateTestOrder(orderId = orderId)
            whenever(orderDetailRepository.getOrderById(any())) doReturn order
            whenever(getShipments(any())) doReturn defaultShipments
            whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
            whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

            createViewModel()

            sut.onHazmatCategorySelected(ShippingLabelHazmatCategory.CLASS_1)
            sut.onHazmatNoticeClick()

            sut.event.observeForever { event = it }

            assertThat(event).isEqualTo(StartHazmatFormEdit(ShippingLabelHazmatCategory.CLASS_1))
        }

    @Test
    fun `when initialized, show expected item quantity`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
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
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn listOf(
            ShipmentUIModel(id = "0", items = defaultShippableItems, labelId = 123)
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
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

        createViewModel()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertThat(dataState.uiState.paperSizeOption).isEqualTo(WooShippingLabelPaperSize.LABEL)
    }

    @Test
    fun `onLabelPaperSizeOptionSelected updates the ViewState as expected`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

        createViewModel()

        sut.onLabelPaperSizeOptionSelected(WooShippingLabelPaperSize.LETTER)

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertThat(dataState.uiState.paperSizeOption).isEqualTo(WooShippingLabelPaperSize.LETTER)
    }

    @Test
    fun `onTrackShipmentClicked triggers OpenUrl event`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn listOf(
            ShipmentUIModel(
                id = "0",
                items = defaultShippableItems,
                carrierId = "usps",
                trackingNumber = "123456"
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
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn listOf(
            ShipmentUIModel(id = "0", items = defaultShippableItems, carrierId = "usps")
        )

        createViewModel()

        var event: OpenUrl? = null
        sut.event.observeForever { if (it is OpenUrl) event = it }

        sut.onSchedulePickUpClicked()

        assertThat(event).isEqualTo(OpenUrl("https://tools.usps.com/schedule-pickup-steps.htm"))
    }

    @Test
    fun `onRefundClicked triggers StartRefundRequest event`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments

        createViewModel()

        var event: StartRefundRequest? = null
        sut.event.observeForever { if (it is StartRefundRequest) event = it }

        sut.onRefundClicked()

        assertThat(event).isEqualTo(StartRefundRequest)
    }

    @Test
    fun `onLearnMoreClicked triggers OpenLearnMoreScreen event`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines,
            customer = Order.Customer(
                billingAddress = defaultShipToAddress,
                shippingAddress = defaultShipToAddress
            )
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShipments(any())) doReturn defaultShipments

        createViewModel()

        var event: OpenLearnMoreScreen? = null
        sut.event.observeForever { if (it is OpenLearnMoreScreen) event = it }

        sut.onLearnMoreClicked()

        assertThat(event).isEqualTo(OpenLearnMoreScreen)
    }
}
