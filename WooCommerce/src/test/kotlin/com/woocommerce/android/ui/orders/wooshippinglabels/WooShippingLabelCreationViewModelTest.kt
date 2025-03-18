package com.woocommerce.android.ui.orders.wooshippinglabels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.LabelPurchased
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState.DataAvailable
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PurchaseState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.WooShippingViewState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.WooShippingViewState.DataState
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressNotification
import com.woocommerce.android.ui.orders.wooshippinglabels.address.GetAddressNotification
import com.woocommerce.android.ui.orders.wooshippinglabels.address.destination.VerifyDestinationAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.address.origin.ObserveOriginAddresses
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.ShouldRequireCustomsForm
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PurchasedLabelData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.PurchasedShippingLabelData
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date
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
            weight = it.toFloat(),
            currency = "USD",
            imageUrl = "https://example.com/image.jpg",
            width = it.toFloat(),
            height = it.toFloat(),
            length = it.toFloat()
        )
    }
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

    private val defaultShippingLabel = ShippingLabelModel(
        labelId = 12L,
        carrierId = WooShippingCarrier.UPS.name,
        tracking = "1234567890",
        refundableAmount = BigDecimal.ZERO,
        commercialInvoiceUrl = "",
        created = Date(),
        createdDate = Date(),
        currency = "USD",
        expiryDate = 9999999999L,
        isCommercialInvoiceSubmittedElectronically = false,
        isLetter = false,
        mainReceiptId = 1434234,
        packageName = "ups_express",
        productIds = listOf(123L, 456L),
        productNames = listOf("Product 1", "Product 2"),
        rate = BigDecimal.TEN,
        receiptItemId = 23324L,
        serviceName = "UPS Express",
        status = ShippingLabelStatus.PurchaseInProgress
    )

    private val defaultPackageName = "customPackage"

    private val defaultShipToAddress = Address.EMPTY.copy(
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

    private val defaultPurchasedLabelData = PurchasedLabelData(
        labels = listOf(defaultShippingLabel),
        origin = mapOf(defaultPackageName to defaultOriginAddresses.first()),
        destination = mapOf(defaultPackageName to Address.EMPTY),
        rates = mapOf(defaultPackageName to defaultShippingRate)
    )

    private val orderDetailRepository: OrderDetailRepository = mock()
    private val getShippableItems: GetShippableItems = mock()
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

    private val observeOriginAddresses: ObserveOriginAddresses = mock()
    private val getShippingRates: GetShippingRates = mock()
    private val purchaseShippingLabel: PurchaseShippingLabel = mock()
    private val observeStoreOptions: ObserveStoreOptions = mock()
    private val verifyDestinationAddress: VerifyDestinationAddress = mock()
    private val getAddressNotification: GetAddressNotification = mock()

    private lateinit var sut: WooShippingLabelCreationViewModel

    fun createViewModel() {
        sut = WooShippingLabelCreationViewModel(
            orderDetailRepository = orderDetailRepository,
            getShippableItems = getShippableItems,
            currencyFormatter = currencyFormatter,
            observeOriginAddresses = observeOriginAddresses,
            fetchOriginAddresses = mock(),
            getShippingRates = getShippingRates,
            purchaseShippingLabel = purchaseShippingLabel,
            observeStoreOptions = observeStoreOptions,
            fetchAccountSettings = mock(),
            shouldRequireCustoms = shouldRequireCustomsForm,
            addressValidationHelper = mock(),
            verifyDestinationAddress = verifyDestinationAddress,
            getAddressNotification = getAddressNotification,
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
        whenever(getShippableItems(any())) doReturn defaultShippableItems
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
        whenever(getShippableItems(any())) doReturn defaultShippableItems
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
        whenever(getShippableItems(any())) doReturn defaultShippableItems
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
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShippableItems(any())) doReturn defaultShippableItems
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any())
        ) doReturn Result.success(defaultShippingRates)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

        createViewModel()

        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertIs<WooShippingLabelCreationViewModel.ShippingRatesState.DataState>(dataState.shippingRates)
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
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShippableItems(any())) doReturn defaultShippableItems
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any())
        ) doReturn Result.failure(Exception("Random error"))
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)

        createViewModel()
        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assert(currentViewState is DataState)
        val dataState = currentViewState as DataState
        assertIs<WooShippingLabelCreationViewModel.ShippingRatesState.Error>(dataState.shippingRates)
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
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any())
        ) doReturn Result.success(defaultShippingRates)

        createViewModel()

        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        sut.onRefreshShippingRates()

        advanceUntilIdle()

        verify(getShippingRates, times(2)).invoke(any(), any(), any(), any(), any(), any())
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
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any())
        ) doReturn Result.success(defaultShippingRates)

        createViewModel()

        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        sut.onSelectedRateSortOrderChanged(ShippingSortOption.CHEAPEST)

        advanceUntilIdle()

        verify(getShippingRates, times(1)).invoke(any(), any(), any(), any(), any(), any())
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
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(
            getShippingRates(any(), any(), any(), any(), any(), any())
        ) doReturn Result.success(defaultShippingRates)

        createViewModel()

        sut.onPackageSelected(defaultPackageData)

        advanceUntilIdle()

        sut.onSelectedRateSortOrderChanged(ShippingSortOption.FASTEST)

        advanceUntilIdle()

        verify(getShippingRates, times(1)).invoke(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `onPackageSelected updates state to DataAvailable when current state is NotSelected`() = testBlocking {
        var currentViewState: WooShippingViewState? = null
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShippableItems(any())) doReturn defaultShippableItems
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

        assertThat(dataState.packageSelection).isInstanceOf(DataAvailable::class.java)
        val dataAvailable = dataState.packageSelection as DataAvailable
        assertThat(dataAvailable.selectedPackage).isEqualTo(initialPackageData)
    }

    @Test
    fun `onPackageSelected updates state to DataAvailable when current state is DataAvailable`() = testBlocking {
        var currentViewState: WooShippingViewState? = null
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShippableItems(any())) doReturn defaultShippableItems
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

        assertThat(dataState.packageSelection).isInstanceOf(DataAvailable::class.java)
        val dataAvailable = dataState.packageSelection as DataAvailable
        assertThat(dataAvailable.selectedPackage).isEqualTo(newPackageData)
    }

    @Test
    fun `CustomState is NotRequired when shouldRequireCustomsForm returns false`() = testBlocking {
        var currentViewState: WooShippingViewState? = null
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShippableItems(any())) doReturn defaultShippableItems
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

        assertThat(dataState.customsState).isEqualTo(CustomsState.NotRequired)
    }

    @Test
    fun `CustomState is Unavailable when shouldRequireCustomsForm returns true`() = testBlocking {
        var currentViewState: WooShippingViewState? = null
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShippableItems(any())) doReturn defaultShippableItems
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

        assertThat(dataState.customsState).isEqualTo(CustomsState.NotRequired)
    }

    @Test
    fun `CustomState is ItnMissing when shouldRequireCustomsForm returns true and ShippingLines exceeds the 2500 limit`() = testBlocking {
        var currentViewState: WooShippingViewState? = null
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn true
        whenever(getShippableItems(any())) doReturn defaultShippableItems.map { it.copy(price = BigDecimal(10000)) }

        createViewModel()

        advanceUntilIdle()

        sut.viewState.asLiveData().observeForever {
            currentViewState = it
        }

        assertThat(currentViewState).isInstanceOf(DataState::class.java)
        val dataState = currentViewState as DataState

        assertThat(dataState.customsState).isEqualTo(CustomsState.ItnMissing)
    }

    @Test
    fun `ItnMissing is dismissed when onDismissItnNotice is called`() = testBlocking {
        var dataState: DataState? = null
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
        whenever(shouldRequireCustomsForm.invoke(any())) doReturn true
        whenever(getShippableItems(any())) doReturn defaultShippableItems.map { it.copy(price = BigDecimal(10000)) }

        createViewModel()

        advanceUntilIdle()

        sut.viewState.asLiveData().observeForever {
            dataState = it as? DataState
        }

        assertThat(dataState?.customsState).isEqualTo(CustomsState.ItnMissing)

        sut.onDismissItnNotice()

        advanceUntilIdle()

        assertThat(dataState?.customsState).isEqualTo(CustomsState.Unavailable)
    }

    @Test
    fun `when onPurchaseShippingLabel succeed then return the label data`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId).copy(
            shippingLines = defaultShippingLines
        )
        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShippableItems(any())) doReturn defaultShippableItems
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
        whenever(
            purchaseShippingLabel(any(), any(), any(), any(), any(), any(), any(), any())
        ) doReturn Result.success(defaultPurchasedLabelData)

        val label = defaultPurchasedLabelData.labels.first()

        val expectedLabelData = PurchasedShippingLabelData(
            labelId = label.labelId,
            orderId = 1L,
            carrierId = label.carrierId,
            trackingNumber = label.tracking,
            items = mock(),
            rateSummary = mock(),
            shippingLines = mock(),
            addresses = mock()
        )

        createViewModel()

        val selectedRate = defaultShippingRates.values.first().first()

        sut.onPackageSelected(defaultPackageData)
        sut.onSelectedSippingRateChanged(selectedRate)

        advanceUntilIdle()

        var event: MultiLiveEvent.Event? = null
        sut.event.observeForever { latestEvent -> event = latestEvent }

        sut.onPurchaseShippingLabel()

        val currentViewState = sut.viewState.value
        assertThat(currentViewState).isInstanceOf(DataState::class.java)
        val dataState = currentViewState as DataState
        assertThat(dataState.purchaseState).isEqualTo(PurchaseState.Success)

        assertThat(event).isInstanceOf(LabelPurchased::class.java)
        val data = (event as LabelPurchased).purchaseData
        assertThat(data.labelId).isEqualTo(expectedLabelData.labelId)
        assertThat(data.trackingNumber).isEqualTo(expectedLabelData.trackingNumber)
        assertThat(data.carrierId).isEqualTo(expectedLabelData.carrierId)
        assertThat(data.orderId).isEqualTo(expectedLabelData.orderId)
    }

    @Test
    fun `when onPurchaseShippingLabel fails then show a snackbar`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShippableItems(any())) doReturn defaultShippableItems
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
        whenever(
            purchaseShippingLabel(any(), any(), any(), any(), any(), any(), any(), any())
        ) doReturn Result.failure(Exception("Random error"))

        createViewModel()

        val selectedRate = defaultShippingRates.values.first().first()

        sut.onPackageSelected(defaultPackageData)
        sut.onSelectedSippingRateChanged(selectedRate)

        advanceUntilIdle()

        sut.onPurchaseShippingLabel()

        assertThat(sut.actionSnackbar).matches { it?.message == R.string.woo_shipping_labels_purchase_error }
    }

    @Test
    fun `when the view model is created, then get store options from the local preferences and update settings on background`() =
        testBlocking {
            val order = OrderTestUtils.generateTestOrder(orderId = orderId)

            whenever(orderDetailRepository.getOrderById(any())) doReturn order
            whenever(getShippableItems(any())) doReturn defaultShippableItems
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
    fun `when there are address notifications then display the notification`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)
        val notification = AddressNotification(
            isSuccess = false,
            message = R.string.woo_shipping_address_notification_destination_missing,
            isDestinationNotification = false
        )

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShippableItems(any())) doReturn defaultShippableItems
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
        whenever(getAddressNotification(any(), anyOrNull())) doReturn notification

        createViewModel()

        advanceUntilIdle()

        val currentViewState = sut.viewState.value
        assertThat(currentViewState).isInstanceOf(DataState::class.java)
        val dataState = currentViewState as DataState
        assertThat(dataState.uiState.addressNotification).isEqualTo(notification)
    }

    @Test
    fun `when an address notifications is displayed then dismissAddressNotification should dismiss the notification`() = testBlocking {
        val order = OrderTestUtils.generateTestOrder(orderId = orderId)
        val notification = AddressNotification(
            isSuccess = false,
            message = R.string.woo_shipping_address_notification_destination_missing,
            isDestinationNotification = false
        )

        whenever(orderDetailRepository.getOrderById(any())) doReturn order
        whenever(getShippableItems(any())) doReturn defaultShippableItems
        whenever(observeOriginAddresses()) doReturn flowOf(defaultOriginAddresses)
        whenever(observeStoreOptions()) doReturn flowOf(defaultStoreOptions)
        whenever(getAddressNotification(any(), anyOrNull())) doReturn notification

        createViewModel()

        advanceUntilIdle()

        var currentViewState = sut.viewState.value
        assertThat(currentViewState).isInstanceOf(DataState::class.java)
        var dataState = currentViewState as DataState
        assertThat(dataState.uiState.addressNotification).isEqualTo(notification)

        sut.onDismissAddressNotification()

        currentViewState = sut.viewState.value
        assertThat(currentViewState).isInstanceOf(DataState::class.java)
        dataState = currentViewState as DataState
        assertThat(dataState.uiState.addressNotification).isNull()
    }
}
