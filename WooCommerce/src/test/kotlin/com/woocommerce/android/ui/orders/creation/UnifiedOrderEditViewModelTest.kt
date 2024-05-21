package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_SEARCH_VIA_SKU_FAILURE
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_SEARCH_VIA_SKU_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_ADDED_VIA
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SCANNING_BARCODE_FORMAT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SCANNING_FAILURE_REASON
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SCANNING_SOURCE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningTracker
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Failed
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Succeeded
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.ui.orders.creation.configuration.ConfigurationType
import com.woocommerce.android.ui.orders.creation.configuration.ProductConfiguration
import com.woocommerce.android.ui.orders.creation.configuration.ProductRules
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget
import com.woocommerce.android.ui.orders.creation.product.discount.CurrencySymbolFinder
import com.woocommerce.android.ui.orders.creation.shipping.ShippingUpdateResult
import com.woocommerce.android.ui.orders.creation.taxes.GetAddressFromTaxRate
import com.woocommerce.android.ui.orders.creation.taxes.GetTaxRatesInfoDialogViewState
import com.woocommerce.android.ui.orders.creation.taxes.rates.GetTaxRateLabel
import com.woocommerce.android.ui.orders.creation.taxes.rates.GetTaxRatePercentageValueText
import com.woocommerce.android.ui.orders.creation.taxes.rates.setting.GetAutoTaxRateSetting
import com.woocommerce.android.ui.orders.creation.totals.OrderCreateEditTotalsHelper
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.OrderCreationProductRestrictions
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WCProductStore
import java.math.BigDecimal
import java.util.Date
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
abstract class UnifiedOrderEditViewModelTest : BaseUnitTest() {
    protected lateinit var sut: OrderCreateEditViewModel
    protected lateinit var viewState: OrderCreateEditViewModel.ViewState
    protected lateinit var savedState: SavedStateHandle
    protected lateinit var orderCreationProductMapper: OrderCreationProductMapper
    protected lateinit var createUpdateOrderUseCase: CreateUpdateOrder
    private lateinit var autoSyncPriceModifier: AutoSyncPriceModifier
    private lateinit var autoSyncOrder: AutoSyncOrder
    protected lateinit var createOrderItemUseCase: CreateOrderItem
    protected lateinit var orderCreateEditRepository: OrderCreateEditRepository
    protected lateinit var orderDetailRepository: OrderDetailRepository
    protected lateinit var parameterRepository: ParameterRepository
    protected lateinit var tracker: AnalyticsTrackerWrapper
    protected lateinit var resourceProvider: ResourceProvider
    private lateinit var barcodeScanningTracker: BarcodeScanningTracker
    private lateinit var checkDigitRemoverFactory: CheckDigitRemoverFactory
    private lateinit var productRestrictions: OrderCreationProductRestrictions
    private lateinit var taxRateToAddress: GetAddressFromTaxRate
    private lateinit var getAutoTaxRateSetting: GetAutoTaxRateSetting
    private lateinit var getTaxRatePercentageValueText: GetTaxRatePercentageValueText
    private lateinit var getTaxRateLabel: GetTaxRateLabel
    private lateinit var prefs: AppPrefs
    lateinit var selectedSite: SelectedSite
    lateinit var productListRepository: ProductListRepository
    val currencySymbolFinder: CurrencySymbolFinder = mock()
    private lateinit var mapFeeLineToCustomAmountUiModel: MapFeeLineToCustomAmountUiModel
    protected lateinit var totalsHelper: OrderCreateEditTotalsHelper

    protected val defaultOrderValue = Order.getEmptyOrder(Date(), Date()).copy(id = 123)

    @Before
    fun setUp() {
        initMocks()
        createSut()
    }

    protected abstract val mode: OrderCreateEditViewModel.Mode
    protected abstract val sku: String
    protected abstract val barcodeFormat: BarcodeFormat

    @Suppress("LongMethod")
    private fun initMocks() {
        val defaultOrderItem = createOrderItem()
        val emptyOrder = Order.getEmptyOrder(Date(), Date())
        viewState = OrderCreateEditViewModel.ViewState()
        savedState = spy(OrderCreateEditFormFragmentArgs(mode, sku, barcodeFormat).toSavedStateHandle()) {
            on { getLiveData(viewState.javaClass.name, viewState) } doReturn MutableLiveData(viewState)
            on {
                getLiveData(
                    eq(Order.getEmptyOrder(Date(), Date()).javaClass.name),
                    any<Order>()
                )
            } doReturn MutableLiveData(emptyOrder)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(Order.getEmptyOrder(Date(), Date())))
        }
        createOrderItemUseCase = mock {
            onBlocking { invoke(123, null) } doReturn defaultOrderItem
            onBlocking { invoke(456, null) } doReturn createOrderItem(456)
            onBlocking { invoke(789, null) } doReturn createOrderItem(789)
            onBlocking { invoke(1, 2) } doReturn createOrderItem(1, 2)
            ProductSelectorViewModel.SelectedItem.ProductVariation(1, 2)
        }
        parameterRepository = mock {
            on { getParameters("parameters_key", savedState) } doReturn
                SiteParameters(
                    currencyCode = "",
                    currencySymbol = null,
                    currencyFormattingParameters = null,
                    weightUnit = null,
                    dimensionUnit = null,
                    gmtOffset = 0F
                )
        }
        orderCreateEditRepository = mock {
            onBlocking { createOrUpdateOrder(defaultOrderValue) } doReturn Result.success(defaultOrderValue)
        }
        orderDetailRepository = mock {
            on { getOrderStatusOptions() } doReturn orderStatusList
        }
        @Suppress("UNCHECKED_CAST")
        orderCreationProductMapper = mock {
            onBlocking { toOrderProducts(any(), eq(null)) } doAnswer { invocationOnMock ->
                val args = invocationOnMock.arguments
                (args.first() as? List<Order.Item>)?.let { list ->
                    if (list.isEmpty()) {
                        emptyList()
                    } else {
                        list.map { createProductItem(item = it) }
                    }
                } ?: emptyList()
            }
        }
        tracker = mock()
        barcodeScanningTracker = mock()
        checkDigitRemoverFactory = mock()
        productListRepository = mock()
        resourceProvider = mock {
            on {
                getString(R.string.order_creation_barcode_scanning_scanning_failed)
            } doReturn "Scanning failed. Please try again later"
            on { getString(R.string.order_creation_set_tax_rate) } doReturn "Set New Tax Rate"
        }
        productRestrictions = mock()
        selectedSite = mock()
        taxRateToAddress = mock()
        getAutoTaxRateSetting = mock {
            onBlocking { invoke() } doReturn null
        }
        getTaxRatePercentageValueText = mock()
        getTaxRateLabel = mock()
        prefs = mock()
        mapFeeLineToCustomAmountUiModel = mock()
        totalsHelper = mock()
    }

    protected abstract val tracksFlow: String

    protected abstract fun initMocksForAnalyticsWithOrder(order: Order)

    @Test
    fun `when product selected, send tracks event`() {
        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))

        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_ADD,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_PRODUCT_COUNT to 1,
                KEY_PRODUCT_ADDED_VIA to ProductAddedVia.MANUALLY.addedVia,
                AnalyticsTracker.KEY_HAS_BUNDLE_CONFIGURATION to false,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            ),
        )
    }

    @Test
    fun `when multiple products selected, send tracks event with correct property`() {
        val selectedItems = setOf(
            ProductSelectorViewModel.SelectedItem.Product(1),
            ProductSelectorViewModel.SelectedItem.Product(2),
            ProductSelectorViewModel.SelectedItem.Product(3),
            ProductSelectorViewModel.SelectedItem.Product(4),
        )
        sut.onProductsSelected(selectedItems)
        assertThat(selectedItems).hasSize(4)

        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_ADD,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_PRODUCT_COUNT to 4,
                KEY_PRODUCT_ADDED_VIA to ProductAddedVia.MANUALLY.addedVia,
                AnalyticsTracker.KEY_HAS_BUNDLE_CONFIGURATION to false,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            ),
        )
    }

    @Test
    fun `when customer address edited, send tracks event`() {
        sut.onCustomerEdited(
            Order.Customer(
                customerId = 0,
                billingAddress = Address.EMPTY,
                shippingAddress = Address.EMPTY
            )
        )

        verify(tracker).track(
            AnalyticsEvent.ORDER_CUSTOMER_ADD,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_HAS_DIFFERENT_SHIPPING_DETAILS to false,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    fun `when onEditCustomerClicked, then EditCustomer sent`() {
        sut.onEditCustomerClicked()

        assertThat(sut.event.value).isInstanceOf(OrderCreateEditNavigationTarget.EditCustomer::class.java)
    }

    @Test
    fun `when onAddCustomerClicked, then AddCustomer sent`() {
        sut.onAddCustomerClicked()

        assertThat(sut.event.value).isInstanceOf(OrderCreateEditNavigationTarget.AddCustomer::class.java)
    }

    @Test
    fun `when customer deleted, then order is update with null customer info`() {
        sut.onCustomerDeleted()

        val values = sut.orderDraft.captureValues()

        assertThat(values.last().customer).isNull()
    }

    @Test
    fun `when customer address deleted, then delete event tracked`() {
        sut.onCustomerDeleted()

        verify(tracker).track(
            AnalyticsEvent.ORDER_CUSTOMER_DELETE,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
        )
    }

    @Test
    fun `when fee edited, send tracks event`() {
        sut.onFeeEdited(BigDecimal.TEN)

        verify(tracker).track(
            AnalyticsEvent.ORDER_FEE_ADD,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow),
        )
    }

    @Test
    fun `when shipping added or edited, send tracks event`() {
        sut.onShippingEdited(BigDecimal.TEN, "")

        verify(tracker).track(
            AnalyticsEvent.ORDER_SHIPPING_METHOD_ADD,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow),
        )
    }

    @Test
    fun `when shipping line added or edited, send tracks event`() {
        val result = ShippingUpdateResult(
            id = 1L,
            amount = BigDecimal.TEN,
            name = "Other",
            methodId = "other"
        )
        sut.onUpdatedShipping(result)

        verify(tracker).track(
            AnalyticsEvent.ORDER_SHIPPING_METHOD_ADD,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_SHIPPING_METHOD to result.methodId
            )
        )
    }

    @Test
    fun `when customer note added or edited, send tracks event`() {
        sut.onCustomerNoteEdited("")

        verify(tracker).track(
            AnalyticsEvent.ORDER_NOTE_ADD,
            mapOf(
                AnalyticsTracker.KEY_PARENT_ID to 0L,
                AnalyticsTracker.KEY_STATUS to Order.Status.Pending,
                AnalyticsTracker.KEY_TYPE to AnalyticsTracker.Companion.OrderNoteType.CUSTOMER,
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact",
            )
        )
    }

    @Test
    fun `when status is edited, send tracks event`() {
        sut.onOrderStatusChanged(Order.Status.Cancelled)

        verify(tracker).track(
            AnalyticsEvent.ORDER_STATUS_CHANGE,
            mapOf(
                AnalyticsTracker.KEY_ID to 0L,
                AnalyticsTracker.KEY_FROM to Order.Status.Pending.value,
                AnalyticsTracker.KEY_TO to Order.Status.Cancelled.value,
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    open fun `when product quantity increased, send tracks event`() {
        val productId = 1L
        val products = OrderTestUtils.generateTestOrderItems(count = 1, productId = productId)
        val order = defaultOrderValue.copy(items = products)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onIncreaseProductsQuantity(productId)
        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_QUANTITY_CHANGE,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    fun `given product qt greater than 1, when product quantity decreased, send tracks event`() {
        val productId = 1L
        val products = OrderTestUtils.generateTestOrderItems(count = 1, productId = productId, quantity = 3F)
        val order = defaultOrderValue.copy(items = products)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onDecreaseProductsQuantity(productId)
        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_QUANTITY_CHANGE,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    fun `given product qt equals to 1, when product quantity decreased, send tracks event`() {
        val productId = 1L
        val products = OrderTestUtils.generateTestOrderItems(count = 1, productId = productId, quantity = 1F)
        val order = defaultOrderValue.copy(items = products)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onDecreaseProductsQuantity(productId)
        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_REMOVE,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    fun `when product removed, send tracks event`() {
        val productId = 1L
        val products = OrderTestUtils.generateTestOrderItems(count = 1, productId = productId, quantity = 3F)
        val order = defaultOrderValue.copy(items = products)

        var orderProducts: List<OrderCreationProduct>? = null
        sut.products.observeForever {
            orderProducts = it
        }

        initMocksForAnalyticsWithOrder(order)
        createSut()
        val productToRemove = orderProducts!!.first()
        sut.onRemoveProduct(productToRemove)
        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_REMOVE,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    fun `when fee removed, send tracks event`() {
        val feesLines = listOf(
            Order.FeeLine.EMPTY.copy(
                name = "order_custom_fee",
                total = BigDecimal(10)
            )
        )
        val order = defaultOrderValue.copy(feesLines = feesLines)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onFeeRemoved()
        verify(tracker).track(
            AnalyticsEvent.ORDER_FEE_REMOVE,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    fun `when shipping method removed, send tracks event`() {
        val shippingLines = listOf(
            Order.ShippingLine(
                methodId = "other",
                total = BigDecimal(10),
                methodTitle = "name"
            )
        )
        val order = defaultOrderValue.copy(shippingLines = shippingLines)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onShippingRemoved()
        verify(tracker).track(
            AnalyticsEvent.ORDER_SHIPPING_METHOD_REMOVE,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    fun `when order sync fails, send tracks event`() {
        val wooError = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = BaseRequest.GenericErrorType.TIMEOUT,
            message = "fail"
        )
        val throwable = WooException(error = wooError)
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Failed(throwable))
        }

        createSut()

        verify(tracker).track(
            stat = AnalyticsEvent.ORDER_SYNC_FAILED,
            properties = mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_USE_GIFT_CARD to false
            ),
            errorContext = sut::class.java.simpleName,
            errorType = wooError.type.name,
            errorDescription = wooError.message
        )
    }

    @Test
    fun `given a standard order creation, when add gift card is clicked, then track expected event`() = testBlocking {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onAddGiftCardButtonClicked()

        verify(tracker).track(
            AnalyticsEvent.ORDER_FORM_ADD_GIFT_CARD_CTA_TAPPED,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
        )
    }

    @Test
    fun `given a standard order creation, when a gift card code is set, then track expected event`() = testBlocking {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onGiftCardSelected("abc")

        verify(tracker).track(
            AnalyticsEvent.ORDER_FORM_GIFT_CARD_SET,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_IS_GIFT_CARD_REMOVED to false,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    fun `given a order creation with gift card already set, when a gift card is removed, then track expected event`() = testBlocking {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onGiftCardSelected("abc")

        verify(tracker).track(
            AnalyticsEvent.ORDER_FORM_GIFT_CARD_SET,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_IS_GIFT_CARD_REMOVED to false,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )

        sut.onGiftCardSelected("")

        verify(tracker).track(
            AnalyticsEvent.ORDER_FORM_GIFT_CARD_SET,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_IS_GIFT_CARD_REMOVED to true,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    // region Scanned and Deliver
    @Test
    fun `when scan succeeds, then set isUpdatingOrderDraft to true`() {
        createSut()
        val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
        var isUpdatingOrderDraft: Boolean? = null
        sut.viewStateData.observeForever { _, viewState ->
            isUpdatingOrderDraft = viewState.isUpdatingOrderDraft
        }

        sut.handleBarcodeScannedStatus(scannedStatus)

        assertTrue(isUpdatingOrderDraft!!)
    }

    @Test
    fun `when SKU search succeeds, then set isUpdatingOrderDraft to false`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                ProductTestUtils.generateProductList()
            )
            var isUpdatingOrderDraft: Boolean? = null
            sut.viewStateData.observeForever { _, viewState ->
                isUpdatingOrderDraft = viewState.isUpdatingOrderDraft
            }

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertFalse(isUpdatingOrderDraft!!)
        }
    }

    @Test
    fun `when SKU search succeeds, then add the scanned product`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 1L,
                        isVariable = true,
                    )
                )
            )
            whenever(createOrderItemUseCase.invoke(1L, 10L)).thenReturn(
                createOrderItem(10L)
            )
            var newOrder: Order? = null
            sut.orderDraft.observeForever { newOrderData ->
                newOrder = newOrderData
            }

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(newOrder?.getProductIds()?.any { it == 10L }).isTrue()
        }
    }

    @Test
    fun `when SKU search succeeds for variable-subscription product, then add the scanned product`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 1L,
                        productType = "variable-subscription",
                    )
                )
            )
            whenever(createOrderItemUseCase.invoke(1L, 10L)).thenReturn(
                createOrderItem(10L)
            )
            var newOrder: Order? = null
            sut.orderDraft.observeForever { newOrderData ->
                newOrder = newOrderData
            }

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(newOrder?.getProductIds()?.any { it == 10L }).isTrue()
        }
    }

    @Test
    fun `when SKU search succeeds for a not published product, then do not add that product`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            val product = ProductTestUtils.generateProduct(
                productId = 10L,
                customStatus = ProductStatus.PENDING.name
            )
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(listOf(product))
            whenever(createOrderItemUseCase.invoke(10L)).thenReturn(
                createOrderItem(10L)
            )
            whenever(productRestrictions.isProductRestricted(product)).thenReturn(true)
            var newOrder: Order? = null
            sut.orderDraft.observeForever { newOrderData ->
                newOrder = newOrderData
            }

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(newOrder?.getProductIds()?.any { it == 10L }).isFalse()
        }
    }

    @Test
    fun `when SKU search succeeds for a product with price 0, then add that product`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        customStatus = ProductStatus.PUBLISH.name,
                        amount = "0"
                    )
                )
            )
            whenever(createOrderItemUseCase.invoke(10L)).thenReturn(
                createOrderItem(10L)
            )
            var newOrder: Order? = null
            sut.orderDraft.observeForever { newOrderData ->
                newOrder = newOrderData
            }

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(newOrder?.getProductIds()?.any { it == 10L }).isTrue()
        }
    }

    @Test
    fun `when SKU search succeeds for a product with price null, then do not add that product`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            val product = ProductTestUtils.generateProduct(
                productId = 10L,
                customStatus = ProductStatus.PUBLISH.name,
                amount = ""
            )
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(listOf(product))
            whenever(createOrderItemUseCase.invoke(10L)).thenReturn(
                createOrderItem(10L)
            )
            whenever(productRestrictions.isProductRestricted(product)).thenReturn(true)
            var newOrder: Order? = null
            sut.orderDraft.observeForever { newOrderData ->
                newOrder = newOrderData
            }

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(newOrder?.getProductIds()?.any { it == 10L }).isFalse()
        }
    }

    @Test
    fun `when SKU search succeeds for a not published product, then trigger proper event`() {
        testBlocking {
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_draft_product
                )
            ).thenReturn(
                "You cannot add products that are not published"
            )
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            val product = ProductTestUtils.generateProduct(
                productId = 10L,
                customStatus = ProductStatus.PENDING.name
            )
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(listOf(product))
            whenever(productRestrictions.isProductRestricted(product)).thenReturn(true)

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(sut.event.value).isInstanceOf(
                OnAddingProductViaScanningFailed::class.java
            )
        }
    }

    @Test
    fun `when SKU search succeeds for a not published product, then trigger event with proper message`() {
        testBlocking {
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_draft_product
                )
            ).thenReturn("You cannot add products that are not published")
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            val product = ProductTestUtils.generateProduct(
                productId = 10L,
                customStatus = ProductStatus.PENDING.name
            )
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(listOf(product))
            whenever(productRestrictions.isProductRestricted(product)).thenReturn(true)

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(
                (sut.event.value as OnAddingProductViaScanningFailed).message
            ).isEqualTo(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_draft_product
                )
            )
        }
    }

    @Test
    fun `when SKU search succeeds for a not published product, then track proper event`() {
        testBlocking {
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_draft_product
                )
            ).thenReturn("You cannot add products that are not published")
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            val product = ProductTestUtils.generateProduct(
                productId = 10L,
                customStatus = ProductStatus.PENDING.name
            )
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(listOf(product))
            whenever(productRestrictions.isProductRestricted(product)).thenReturn(true)

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker).track(eq(PRODUCT_SEARCH_VIA_SKU_FAILURE), any())
        }
    }

    @Test
    fun `when SKU search succeeds for a not published product, then track event with proper properties`() {
        testBlocking {
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_draft_product
                )
            ).thenReturn("You cannot add products that are not published")
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            val product = ProductTestUtils.generateProduct(
                productId = 10L,
                customStatus = ProductStatus.PENDING.name
            )
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(listOf(product))
            whenever(productRestrictions.isProductRestricted(product)).thenReturn(true)

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker).track(
                PRODUCT_SEARCH_VIA_SKU_FAILURE,
                mapOf(
                    KEY_SCANNING_SOURCE to "order_creation",
                    KEY_SCANNING_BARCODE_FORMAT to BarcodeFormat.FormatUPCA.formatName,
                    KEY_SCANNING_FAILURE_REASON to "Failed to add a product that is not published"
                )
            )
        }
    }

    @Test
    fun `when SKU search succeeds for a published product whose price is not specified, then trigger event with proper message`() {
        testBlocking {
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_product_with_invalid_price
                )
            ).thenReturn("Unable to add product with invalid price")
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            val product = ProductTestUtils.generateProduct(
                productId = 10L,
                customStatus = ProductStatus.PUBLISH.name,
                amount = ""
            )
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(listOf(product))
            whenever(productRestrictions.isProductRestricted(product)).thenReturn(true)

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(
                (sut.event.value as OnAddingProductViaScanningFailed).message
            ).isEqualTo(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_product_with_invalid_price
                )
            )
        }
    }

    @Test
    fun `when SKU search succeeds for a published product whose price is not specified, then track proper event`() {
        testBlocking {
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_product_with_invalid_price
                )
            ).thenReturn("You cannot add products with no price specified")
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            val product = ProductTestUtils.generateProduct(
                productId = 10L,
                customStatus = ProductStatus.PUBLISH.name,
                amount = ""
            )
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(listOf(product))
            whenever(productRestrictions.isProductRestricted(product)).thenReturn(true)

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker).track(eq(PRODUCT_SEARCH_VIA_SKU_FAILURE), any())
        }
    }

    @Test
    fun `when SKU search succeeds for a published product whose price is not specified, then track event with proper properties`() {
        testBlocking {
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_product_with_invalid_price
                )
            ).thenReturn("You cannot add products with no price specified")
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            val product = ProductTestUtils.generateProduct(
                productId = 10L,
                customStatus = ProductStatus.PUBLISH.name,
                amount = ""
            )
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(listOf(product))
            whenever(productRestrictions.isProductRestricted(product)).thenReturn(true)

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker).track(
                PRODUCT_SEARCH_VIA_SKU_FAILURE,
                mapOf(
                    KEY_SCANNING_SOURCE to "order_creation",
                    KEY_SCANNING_BARCODE_FORMAT to BarcodeFormat.FormatUPCA.formatName,
                    KEY_SCANNING_FAILURE_REASON to "Failed to add a product whose price is not specified"
                )
            )
        }
    }

    @Test
    fun `when SKU search succeeds for a not published product, then do not track product search event`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            val product = ProductTestUtils.generateProduct(
                productId = 10L,
                customStatus = ProductStatus.PENDING.name,
            )
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(listOf(product))
            whenever(productRestrictions.isProductRestricted(product)).thenReturn(true)

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker, never()).track(
                eq(PRODUCT_SEARCH_VIA_SKU_SUCCESS),
                any()
            )
        }
    }

    @Test
    fun `when SKU search succeeds for a published product whose price is not specified, then do not track product search event`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            val product = ProductTestUtils.generateProduct(
                productId = 10L,
                customStatus = ProductStatus.PUBLISH.name,
                amount = ""
            )
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(listOf(product))
            whenever(productRestrictions.isProductRestricted(product)).thenReturn(true)

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker, never()).track(
                eq(PRODUCT_SEARCH_VIA_SKU_SUCCESS),
                any()
            )
        }
    }

    @Test
    fun `when parent variable product is scanned, then trigger proper event`() {
        testBlocking {
            whenever(
                resourceProvider.getString(R.string.order_creation_barcode_scanning_unable_to_add_variable_product)
            ).thenReturn(
                "You cannot add variable product directly. Please select a specific variation"
            )
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 0L,
                        isVariable = true
                    )
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(sut.event.value).isInstanceOf(
                OnAddingProductViaScanningFailed::class.java
            )
        }
    }

    @Test
    fun `when parent variable product is scanned, then trigger event with proper message`() {
        testBlocking {
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_variable_product
                )
            ).thenReturn(
                "You cannot add variable product directly. Please select a specific variation"
            )
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 0L,
                        isVariable = true
                    )
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(
                (sut.event.value as OnAddingProductViaScanningFailed).message
            ).isEqualTo(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_variable_product
                )
            )
        }
    }

    @Test
    fun `when parent variable product is scanned, then do not track any product search via sku success event`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 0L,
                        isVariable = true
                    )
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker, never()).track(
                PRODUCT_SEARCH_VIA_SKU_SUCCESS,
                mapOf(
                    KEY_SCANNING_SOURCE to ScanningSource.ORDER_CREATION.source
                )
            )
        }
    }

    @Test
    fun `when SKU search succeeds for variation product, then add the scanned product`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 1L,
                        productType = "variation",
                    )
                )
            )
            whenever(createOrderItemUseCase.invoke(1L, 10L)).thenReturn(
                createOrderItem(10L)
            )
            var newOrder: Order? = null
            sut.orderDraft.observeForever { newOrderData ->
                newOrder = newOrderData
            }

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(newOrder?.getProductIds()?.any { it == 10L }).isTrue()
        }
    }

    @Test
    fun `when SKU search succeeds for variable parent product, then trigger proper failed event`() {
        testBlocking {
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_variable_product
                )
            )
                .thenReturn("You cannot add variable product directly. Please select a specific variation")
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 0L,
                        isVariable = true,
                    )
                )
            )
            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(sut.event.value).isInstanceOf(OnAddingProductViaScanningFailed::class.java)
        }
    }

    @Test
    fun `when SKU search succeeds for variable parent product, then trigger failed event with proper message`() {
        testBlocking {
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_variable_product
                )
            )
                .thenReturn("You cannot add variable product directly. Please select a specific variation")
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 0L,
                        isVariable = true,
                    )
                )
            )
            R.string.order_creation_barcode_scanning_unable_to_add_variable_product.toString() //
            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(
                (sut.event.value as OnAddingProductViaScanningFailed).message
            ).isEqualTo("You cannot add variable product directly. Please select a specific variation")
        }
    }

    @Test
    fun `when code scanner fails to recognize the barcode, then trigger proper event`() {
        createSut()
        val scannedStatus = CodeScannerStatus.Failure(
            error = "Failed to recognize the barcode",
            type = CodeScanningErrorType.NotFound
        )

        sut.handleBarcodeScannedStatus(scannedStatus)

        assertThat(sut.event.value).isInstanceOf(OnAddingProductViaScanningFailed::class.java)
    }

    @Test
    fun `when code scanner fails to recognize the barcode, then proper message is sent`() {
        createSut()
        val scannedStatus = CodeScannerStatus.Failure(
            error = "Failed to recognize the barcode",
            type = CodeScanningErrorType.NotFound
        )

        sut.handleBarcodeScannedStatus(scannedStatus)

        assertThat((sut.event.value as OnAddingProductViaScanningFailed).message).isEqualTo(
            resourceProvider.getString(R.string.order_creation_barcode_scanning_scanning_failed)
        )
    }

    @Test
    fun `given code scanner fails to recognize the barcode, when retry clicked, then restart code scanning`() {
        createSut()
        val scannedStatus = CodeScannerStatus.Failure(
            error = "Failed to recognize the barcode",
            type = CodeScanningErrorType.NotFound
        )

        sut.handleBarcodeScannedStatus(scannedStatus)
        (sut.event.value as OnAddingProductViaScanningFailed).retry.onClick(mock())

        assertThat(sut.event.value).isInstanceOf(OpenBarcodeScanningFragment::class.java)
    }

    @Test
    fun `when product search by SKU succeeds but has empty result, then trigger proper event`() {
        testBlocking {
            val skuCode = "12345"
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_product,
                    skuCode
                )
            )
                .thenReturn("Product with SKU $skuCode not found. Unable to add to the order")
            createSut()
            val scannedStatus = CodeScannerStatus.Success(skuCode, BarcodeFormat.FormatQRCode)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(sut.event.value).isInstanceOf(OnAddingProductViaScanningFailed::class.java)
        }
    }

    @Test
    fun `when product search by SKU fails, then proper message is displayed`() {
        testBlocking {
            val skuCode = "12345"
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_product,
                    skuCode
                )
            )
                .thenReturn("Product with SKU $skuCode not found. Unable to add to the order")
            createSut()
            val scannedStatus = CodeScannerStatus.Success(skuCode, BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    skuCode,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(null)

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(
                (sut.event.value as OnAddingProductViaScanningFailed).message
            ).isEqualTo("Product with SKU $skuCode not found. Unable to add to the order")
        }
    }

    @Test
    fun `given product search by SKU fails, when retry clicked, then restart scanning`() {
        testBlocking {
            val skuCode = "12345"
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_product,
                    skuCode
                )
            )
                .thenReturn("Product with SKU $skuCode not found. Unable to add to the order")
            createSut()
            val scannedStatus = CodeScannerStatus.Success(skuCode, BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    skuCode,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(null)

            sut.handleBarcodeScannedStatus(scannedStatus)
            (sut.event.value as OnAddingProductViaScanningFailed).retry.onClick(mock())

            assertThat(sut.event.value).isInstanceOf(OpenBarcodeScanningFragment::class.java)
        }
    }

    @Test
    fun `given that same variable subscription product scanned thrice, then increment the product quantity accordingly`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 1L,
                        productType = "variable-subscription",
                    )
                )
            )
            whenever(createOrderItemUseCase.invoke(1L, 10L)).thenReturn(
                createOrderItem(1L, 10L)
            )
            var orderDraft: Order? = null
            sut.orderDraft.observeForever { order ->
                orderDraft = order
            }

            sut.handleBarcodeScannedStatus(scannedStatus)
            sut.handleBarcodeScannedStatus(scannedStatus)
            sut.handleBarcodeScannedStatus(scannedStatus)

            orderDraft?.items
                ?.takeIf { it.isNotEmpty() }
                ?.find { it.variationId == 10L }
                ?.let { assertThat(it.quantity).isEqualTo(3f) }
                ?: fail("Expected an item with variationId 10L with quantity as 3")
        }
    }

    @Test
    fun `given that same product scanned thrice, then increment the product quantity accordingly`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                    )
                )
            )
            whenever(createOrderItemUseCase.invoke(10L)).thenReturn(
                createOrderItem(10L)
            )
            var orderDraft: Order? = null
            sut.orderDraft.observeForever { order ->
                orderDraft = order
            }

            sut.handleBarcodeScannedStatus(scannedStatus)
            sut.handleBarcodeScannedStatus(scannedStatus)
            sut.handleBarcodeScannedStatus(scannedStatus)

            orderDraft?.items
                ?.takeIf { it.isNotEmpty() }
                ?.find { it.productId == 10L }
                ?.let { assertThat(it.quantity).isEqualTo(3f) }
                ?: fail("Expected an item with productId 10L with quantity as 3")
        }
    }

    @Test
    fun `when scan clicked, then track proper event`() {
        createSut()

        sut.onScanClicked()

        verify(tracker).track(
            AnalyticsEvent.ORDER_CREATION_PRODUCT_BARCODE_SCANNING_TAPPED,
            mapOf(AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact")
        )
    }

    @Test
    fun `when scan clicked, then trigger proper event`() {
        createSut()

        sut.onScanClicked()

        assertThat(sut.event.value).isInstanceOf(OpenBarcodeScanningFragment::class.java)
    }

    @Test
    fun `when scan success, then track proper event`() {
        createSut()
        val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)

        sut.handleBarcodeScannedStatus(scannedStatus)

        verify(barcodeScanningTracker).trackSuccess(any())
    }

    @Test
    fun `when scan success, then track proper event with proper source`() {
        createSut()
        val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)

        sut.handleBarcodeScannedStatus(scannedStatus)

        verify(barcodeScanningTracker).trackSuccess(ScanningSource.ORDER_CREATION)
    }

    @Test
    fun `when scan failure, then track proper event`() {
        createSut()
        val scannedStatus = CodeScannerStatus.Failure(
            error = "Failed to recognize the barcode",
            type = CodeScanningErrorType.NotFound
        )

        sut.handleBarcodeScannedStatus(scannedStatus)

        verify(barcodeScanningTracker).trackScanFailure(any(), any())
    }

    @Test
    fun `when scan failure, then track proper event with proper source`() {
        createSut()
        val scannedStatus = CodeScannerStatus.Failure(
            error = "Failed to recognize the barcode",
            type = CodeScanningErrorType.NotFound
        )

        sut.handleBarcodeScannedStatus(scannedStatus)

        verify(barcodeScanningTracker).trackScanFailure(
            eq(ScanningSource.ORDER_CREATION),
            any()
        )
    }

    @Test
    fun `when scan failure, then track proper event with proper error type`() {
        createSut()
        val scannedStatus = CodeScannerStatus.Failure(
            error = "Failed to recognize the barcode",
            type = CodeScanningErrorType.NotFound
        )

        sut.handleBarcodeScannedStatus(scannedStatus)

        verify(barcodeScanningTracker).trackScanFailure(
            any(),
            eq(CodeScanningErrorType.NotFound)
        )
    }

    @Test
    fun `given product search via sku succeeds, then track proper event`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 1L,
                        productType = "variable-subscription",
                    )
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker).track(
                eq(PRODUCT_SEARCH_VIA_SKU_SUCCESS),
                any()
            )
        }
    }

    @Test
    fun `given product search via sku succeeds, then track event with proper source`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 1L,
                        productType = "variable-subscription",
                    )
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker).track(
                PRODUCT_SEARCH_VIA_SKU_SUCCESS,
                mapOf(
                    KEY_SCANNING_SOURCE to "order_creation",
                    AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
                )
            )
        }
    }

    @Test
    fun `given product search via sku fails, then track proper event`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(null)

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker).track(
                eq(PRODUCT_SEARCH_VIA_SKU_FAILURE),
                any()
            )
        }
    }

    @Test
    fun `given product search via sku fails, then track event with proper source`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(null)

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker).track(
                PRODUCT_SEARCH_VIA_SKU_FAILURE,
                mapOf(
                    KEY_SCANNING_SOURCE to "order_creation",
                    KEY_SCANNING_BARCODE_FORMAT to BarcodeFormat.FormatUPCA.formatName,
                    KEY_SCANNING_FAILURE_REASON to "Product search via SKU API call failed"
                )
            )
        }
    }

    @Test
    fun `given product search via sku fails, then track event with proper reason`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(null)

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker).track(
                PRODUCT_SEARCH_VIA_SKU_FAILURE,
                mapOf(
                    KEY_SCANNING_SOURCE to "order_creation",
                    KEY_SCANNING_BARCODE_FORMAT to BarcodeFormat.FormatUPCA.formatName,
                    KEY_SCANNING_FAILURE_REASON to "Product search via SKU API call failed"
                )
            )
        }
    }

    @Test
    fun `given product search via sku fails when trying to add parent variable product, then track event with proper reason`() {
        testBlocking {
            whenever(
                resourceProvider.getString(
                    R.string.order_creation_barcode_scanning_unable_to_add_variable_product
                )
            ).thenReturn("You cannot add variable product directly. Please select a specific variation")
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 0L,
                        isVariable = true
                    )
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker).track(
                PRODUCT_SEARCH_VIA_SKU_FAILURE,
                mapOf(
                    KEY_SCANNING_SOURCE to "order_creation",
                    KEY_SCANNING_BARCODE_FORMAT to BarcodeFormat.FormatUPCA.formatName,
                    KEY_SCANNING_FAILURE_REASON to
                        "Instead of specific variations, user tried to add parent variable product."
                )
            )
        }
    }

    @Test
    fun `given product search via sku succeeds but contains no product, then track event with proper source`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatQRCode)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker).track(
                PRODUCT_SEARCH_VIA_SKU_FAILURE,
                mapOf(
                    KEY_SCANNING_SOURCE to "order_creation",
                    KEY_SCANNING_BARCODE_FORMAT to BarcodeFormat.FormatQRCode.formatName,
                    KEY_SCANNING_FAILURE_REASON to "Empty data response (no product found for the SKU)"
                )
            )
        }
    }

    @Test
    fun `given variable product from order creation screen, when product added via scanning, then track correct source`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 1L,
                        isVariable = true
                    )
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker).track(
                AnalyticsEvent.ORDER_PRODUCT_ADD,
                mapOf(
                    AnalyticsTracker.KEY_FLOW to tracksFlow,
                    AnalyticsTracker.KEY_PRODUCT_COUNT to 1,
                    KEY_SCANNING_SOURCE to ScanningSource.ORDER_CREATION.source,
                    KEY_PRODUCT_ADDED_VIA to ProductAddedVia.SCANNING.addedVia,
                    AnalyticsTracker.KEY_HAS_BUNDLE_CONFIGURATION to false,
                    AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
                )
            )
        }
    }

    @Test
    fun `given non-variable product from order creation screen, when product added via scanning, then track correct source`() {
        testBlocking {
            createSut()
            val scannedStatus = CodeScannerStatus.Success("12345", BarcodeFormat.FormatUPCA)
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                    )
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker).track(
                AnalyticsEvent.ORDER_PRODUCT_ADD,
                mapOf(
                    AnalyticsTracker.KEY_FLOW to tracksFlow,
                    AnalyticsTracker.KEY_PRODUCT_COUNT to 1,
                    KEY_SCANNING_SOURCE to ScanningSource.ORDER_CREATION.source,
                    KEY_PRODUCT_ADDED_VIA to ProductAddedVia.SCANNING.addedVia,
                    AnalyticsTracker.KEY_HAS_BUNDLE_CONFIGURATION to false,
                    AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
                )
            )
        }
    }

    @Test
    fun `given UPC SKU with check digit, when product search fails, then retry product search call by removing the check digit`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            val mockUPCCheckDigitRemover = mock<UPCCheckDigitRemover> {
                on { getSKUWithoutCheckDigit(sku) }.thenReturn(skuWithCheckDigitRemoved)
            }
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatUPCA)
            whenever(
                checkDigitRemoverFactory.getCheckDigitRemoverFor(any())
            ).thenReturn(
                mockUPCCheckDigitRemover
            )
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(productListRepository).searchProductList(
                skuWithCheckDigitRemoved,
                WCProductStore.SkuSearchOptions.ExactSearch
            )
        }
    }

    @Test
    fun `given EAN-13 SKU with check digit, when product search fails, then retry product search call by removing the check digit`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            val mockEAN13CheckDigitRemover = mock<EAN13CheckDigitRemover> {
                on { getSKUWithoutCheckDigit(sku) }.thenReturn(skuWithCheckDigitRemoved)
            }
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatEAN13)
            whenever(
                checkDigitRemoverFactory.getCheckDigitRemoverFor(BarcodeFormat.FormatEAN13)
            ).thenReturn(
                mockEAN13CheckDigitRemover
            )
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(productListRepository).searchProductList(
                skuWithCheckDigitRemoved,
                WCProductStore.SkuSearchOptions.ExactSearch
            )
        }
    }

    @Test
    fun `given EAN-8 SKU with check digit, when product search fails, then retry product search call by removing the check digit`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            val mockEAN8CheckDigitRemover = mock<EAN8CheckDigitRemover> {
                on { getSKUWithoutCheckDigit(sku) }.thenReturn(skuWithCheckDigitRemoved)
            }
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatEAN8)
            whenever(
                checkDigitRemoverFactory.getCheckDigitRemoverFor(BarcodeFormat.FormatEAN8)
            ).thenReturn(
                mockEAN8CheckDigitRemover
            )
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(productListRepository).searchProductList(
                skuWithCheckDigitRemoved,
                WCProductStore.SkuSearchOptions.ExactSearch
            )
        }
    }

    @Test
    fun `given product search fails for UPC barcode format, when retrying, then show a loading indicator`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            val mockUPCCheckDigitRemover = mock<UPCCheckDigitRemover> {
                on { getSKUWithoutCheckDigit(sku) }.thenReturn(skuWithCheckDigitRemoved)
            }
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatUPCA)
            whenever(
                checkDigitRemoverFactory.getCheckDigitRemoverFor(any())
            ).thenReturn(
                mockUPCCheckDigitRemover
            )
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())
            var isUpdatingOrderDraft: Boolean? = null
            sut.viewStateData.observeForever { _, viewState ->
                isUpdatingOrderDraft = viewState.isUpdatingOrderDraft
            }

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertTrue(isUpdatingOrderDraft!!)
        }
    }

    @Test
    fun `given product search fails for UPC barcode format, when retrying, then do not handle the check digit on failing to fetch product information second time`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            val mockUPCCheckDigitRemover = mock<UPCCheckDigitRemover> {
                on { getSKUWithoutCheckDigit(sku) }.thenReturn(skuWithCheckDigitRemoved)
            }
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatUPCA)
            whenever(
                checkDigitRemoverFactory.getCheckDigitRemoverFor(any())
            ).thenReturn(
                mockUPCCheckDigitRemover
            )
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())
            whenever(
                productListRepository.searchProductList(
                    skuWithCheckDigitRemoved,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(checkDigitRemoverFactory, times(1)).getCheckDigitRemoverFor(any())
            verify(productListRepository, times(1)).searchProductList(
                skuWithCheckDigitRemoved,
                WCProductStore.SkuSearchOptions.ExactSearch
            )
        }
    }

    @Test
    fun `given product search fails for EAN-13 barcode format, when retrying, then do not handle the check digit on failing to fetch product information second time`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            val mockEAN13CheckDigitRemover = mock<EAN13CheckDigitRemover> {
                on { getSKUWithoutCheckDigit(sku) }.thenReturn(skuWithCheckDigitRemoved)
            }
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatEAN13)
            whenever(
                checkDigitRemoverFactory.getCheckDigitRemoverFor(BarcodeFormat.FormatEAN13)
            ).thenReturn(
                mockEAN13CheckDigitRemover
            )
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())
            whenever(
                productListRepository.searchProductList(
                    skuWithCheckDigitRemoved,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(checkDigitRemoverFactory, times(1)).getCheckDigitRemoverFor(BarcodeFormat.FormatEAN13)
            verify(productListRepository, times(1)).searchProductList(
                skuWithCheckDigitRemoved,
                WCProductStore.SkuSearchOptions.ExactSearch
            )
        }
    }

    @Test
    fun `given product search fails for EAN-8 barcode format, when retrying, then do not handle the check digit on failing to fetch product information second time`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            val mockEAN8CheckDigitRemover = mock<EAN8CheckDigitRemover> {
                on { getSKUWithoutCheckDigit(sku) }.thenReturn(skuWithCheckDigitRemoved)
            }
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatEAN8)
            whenever(
                checkDigitRemoverFactory.getCheckDigitRemoverFor(BarcodeFormat.FormatEAN8)
            ).thenReturn(
                mockEAN8CheckDigitRemover
            )
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())
            whenever(
                productListRepository.searchProductList(
                    skuWithCheckDigitRemoved,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(checkDigitRemoverFactory, times(1)).getCheckDigitRemoverFor(BarcodeFormat.FormatEAN8)
            verify(productListRepository, times(1)).searchProductList(
                skuWithCheckDigitRemoved,
                WCProductStore.SkuSearchOptions.ExactSearch
            )
        }
    }

    @Test
    fun `given product search fails for UPC barcode format, when retrying, then do not track any failure event`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            val mockUPCCheckDigitRemover = mock<UPCCheckDigitRemover> {
                on { getSKUWithoutCheckDigit(sku) }.thenReturn(skuWithCheckDigitRemoved)
            }
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatUPCA)
            whenever(
                checkDigitRemoverFactory.getCheckDigitRemoverFor(any())
            ).thenReturn(
                mockUPCCheckDigitRemover
            )
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            whenever(
                productListRepository.searchProductList(
                    skuWithCheckDigitRemoved,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(1L)
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker, never()).track(
                eq(PRODUCT_SEARCH_VIA_SKU_FAILURE),
                any()
            )
        }
    }

    @Test
    fun `given product search fails for EAN-13 barcode format, when retrying, then do not track any failure event`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            val mockEAN13CheckDigitRemover = mock<EAN13CheckDigitRemover> {
                on { getSKUWithoutCheckDigit(sku) }.thenReturn(skuWithCheckDigitRemoved)
            }
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatEAN13)
            whenever(
                checkDigitRemoverFactory.getCheckDigitRemoverFor(BarcodeFormat.FormatEAN13)
            ).thenReturn(
                mockEAN13CheckDigitRemover
            )
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            whenever(
                productListRepository.searchProductList(
                    skuWithCheckDigitRemoved,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(1L)
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker, never()).track(
                eq(PRODUCT_SEARCH_VIA_SKU_FAILURE),
                any()
            )
        }
    }

    @Test
    fun `given product search fails for EAN-8 barcode format, when retrying, then do not track any failure event`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            val mockEAN8CheckDigitRemover = mock<EAN8CheckDigitRemover> {
                on { getSKUWithoutCheckDigit(sku) }.thenReturn(skuWithCheckDigitRemoved)
            }
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatEAN8)
            whenever(
                checkDigitRemoverFactory.getCheckDigitRemoverFor(BarcodeFormat.FormatEAN8)
            ).thenReturn(
                mockEAN8CheckDigitRemover
            )
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            whenever(
                productListRepository.searchProductList(
                    skuWithCheckDigitRemoved,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(1L)
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(tracker, never()).track(
                eq(PRODUCT_SEARCH_VIA_SKU_FAILURE),
                any()
            )
        }
    }

    @Test
    fun `given product search fails for UPC barcode format, when retrying, then do not trigger failure event`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            val mockUPCCheckDigitRemover = mock<UPCCheckDigitRemover> {
                on { getSKUWithoutCheckDigit(sku) }.thenReturn(skuWithCheckDigitRemoved)
            }
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatUPCA)
            whenever(
                checkDigitRemoverFactory.getCheckDigitRemoverFor(any())
            ).thenReturn(
                mockUPCCheckDigitRemover
            )
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            whenever(
                productListRepository.searchProductList(
                    skuWithCheckDigitRemoved,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(1L)
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(sut.event.value).isNull()
        }
    }

    @Test
    fun `given product search fails for EAN-13 barcode format, when retrying, then do not trigger failure event`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            val mockEAN13CheckDigitRemover = mock<EAN13CheckDigitRemover> {
                on { getSKUWithoutCheckDigit(sku) }.thenReturn(skuWithCheckDigitRemoved)
            }
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatEAN13)
            whenever(
                checkDigitRemoverFactory.getCheckDigitRemoverFor(BarcodeFormat.FormatEAN13)
            ).thenReturn(
                mockEAN13CheckDigitRemover
            )
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            whenever(
                productListRepository.searchProductList(
                    skuWithCheckDigitRemoved,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(1L)
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(sut.event.value).isNull()
        }
    }

    @Test
    fun `given product search fails for EAN-8 barcode format, when retrying, then do not trigger failure event`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            val mockEAN8CheckDigitRemover = mock<EAN8CheckDigitRemover> {
                on { getSKUWithoutCheckDigit(sku) }.thenReturn(skuWithCheckDigitRemoved)
            }
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatEAN8)
            whenever(
                checkDigitRemoverFactory.getCheckDigitRemoverFor(BarcodeFormat.FormatEAN8)
            ).thenReturn(
                mockEAN8CheckDigitRemover
            )
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            whenever(
                productListRepository.searchProductList(
                    skuWithCheckDigitRemoved,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(1L)
                )
            )

            sut.handleBarcodeScannedStatus(scannedStatus)

            assertThat(sut.event.value).isNull()
        }
    }

    @Test
    fun `given product search fails for non UPC barcode format, then do not do any checksum operation`() {
        testBlocking {
            val sku = "12345678901"
            val skuWithCheckDigitRemoved = "1234567890"
            createSut()
            val scannedStatus = CodeScannerStatus.Success(sku, BarcodeFormat.FormatQRCode)
            whenever(
                productListRepository.searchProductList(
                    sku,
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            sut.handleBarcodeScannedStatus(scannedStatus)

            verify(checkDigitRemoverFactory, never()).getCheckDigitRemoverFor(any())
            verify(productListRepository, never()).searchProductList(
                skuWithCheckDigitRemoved,
                WCProductStore.SkuSearchOptions.ExactSearch
            )
        }
    }

    @Test
    fun `check that products are ordered by product id to prevent bundle items from switching positions`() {
        var productsIds: List<Long>? = null
        val expectedOrder = listOf(123L, 456L, 789L)
        sut.products.observeForever { products ->
            productsIds = products.map { it.item.productId }
        }
        val selectedItems = setOf(
            ProductSelectorViewModel.SelectedItem.Product(456),
            ProductSelectorViewModel.SelectedItem.Product(789),
            ProductSelectorViewModel.SelectedItem.Product(123),
        )
        sut.onProductsSelected(selectedItems)

        assertThat(productsIds).isEqualTo(expectedOrder)
    }

    @Test
    fun `given configurable item expanded, then track configuration CTA shown`() {
        testBlocking {
            createSut()
            val product = OrderCreationProduct.ProductItem(
                item = Order.Item.EMPTY,
                productInfo = ProductInfo(
                    imageUrl = "",
                    isStockManaged = true,
                    stockQuantity = 5.0,
                    stockStatus = ProductStockStatus.InStock,
                    productType = ProductType.BUNDLE,
                    isConfigurable = true,
                    pricePreDiscount = "5",
                    priceTotal = "5",
                    priceSubtotal = "5",
                    discountAmount = "0",
                    priceAfterDiscount = "5",
                    hasDiscount = false,
                )
            )

            sut.onProductExpanded(true, product)

            verify(tracker).track(
                AnalyticsEvent.ORDER_FORM_BUNDLE_PRODUCT_CONFIGURE_CTA_SHOWN,
                mapOf(
                    AnalyticsTracker.KEY_FLOW to tracksFlow,
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_CARD,
                    AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
                )
            )
        }
    }

    @Test
    fun `given configurable item NOT expanded, then DON'T track configuration CTA shown`() {
        testBlocking {
            createSut()
            val product = OrderCreationProduct.ProductItem(
                item = Order.Item.EMPTY,
                productInfo = ProductInfo(
                    imageUrl = "",
                    isStockManaged = true,
                    stockQuantity = 5.0,
                    stockStatus = ProductStockStatus.InStock,
                    productType = ProductType.BUNDLE,
                    isConfigurable = true,
                    pricePreDiscount = "5",
                    priceTotal = "5",
                    priceSubtotal = "5",
                    discountAmount = "0",
                    priceAfterDiscount = "5",
                    hasDiscount = false,
                )
            )

            sut.onProductExpanded(false, product)

            verify(tracker, never()).track(
                AnalyticsEvent.ORDER_FORM_BUNDLE_PRODUCT_CONFIGURE_CTA_SHOWN,
                mapOf(
                    AnalyticsTracker.KEY_FLOW to tracksFlow,
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_CARD
                )
            )
        }
    }

    @Test
    fun `on edit configuration, then track configuration CTA tapped`() {
        testBlocking {
            createSut()
            val rules: ProductRules = mock()
            val item: Order.Item = mock()
            val productInfo: ProductInfo = mock()
            val configuration: ProductConfiguration = mock()

            whenever(productInfo.productType) doReturn ProductType.BUNDLE

            val product = OrderCreationProduct.GroupedProductItemWithRules(
                item = item,
                productInfo = productInfo,
                configuration = configuration,
                rules = rules,
                children = emptyList()
            )

            sut.onEditConfiguration(product)

            verify(tracker).track(
                AnalyticsEvent.ORDER_FORM_BUNDLE_PRODUCT_CONFIGURE_CTA_TAPPED,
                mapOf(
                    AnalyticsTracker.KEY_FLOW to tracksFlow,
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_CARD
                )
            )
        }
    }

    @Test
    fun `on edit configuration, then DON'T  track configuration CTA tapped if product is not BUNDLE`() {
        testBlocking {
            createSut()
            val rules: ProductRules = mock()
            val item: Order.Item = mock()
            val productInfo: ProductInfo = mock()
            val configuration: ProductConfiguration = mock()

            whenever(productInfo.productType) doReturn ProductType.COMPOSITE

            val product = OrderCreationProduct.GroupedProductItemWithRules(
                item = item,
                productInfo = productInfo,
                configuration = configuration,
                rules = rules,
                children = emptyList()
            )

            sut.onEditConfiguration(product)

            verify(tracker, never()).track(
                AnalyticsEvent.ORDER_FORM_BUNDLE_PRODUCT_CONFIGURE_CTA_TAPPED,
                mapOf(
                    AnalyticsTracker.KEY_FLOW to tracksFlow,
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_CARD
                )
            )
        }
    }

    @Test
    fun `check that when products selected contains a bundle configuration hasBundleConfiguration is true`() {
        val configuration: ProductConfiguration = mock()
        whenever(configuration.configurationType) doReturn ConfigurationType.BUNDLE
        val selectedItems = setOf(
            ProductSelectorViewModel.SelectedItem.Product(456),
            ProductSelectorViewModel.SelectedItem.Product(789),
            ProductSelectorViewModel.SelectedItem.Product(123),
            ProductSelectorViewModel.SelectedItem.ConfigurableProduct(183, configuration)
        )
        sut.onProductsSelected(selectedItems)

        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_ADD,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_PRODUCT_COUNT to selectedItems.size,
                KEY_PRODUCT_ADDED_VIA to ProductAddedVia.MANUALLY.addedVia,
                AnalyticsTracker.KEY_HAS_BUNDLE_CONFIGURATION to true,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    fun `check that when products selected DON'T contains a bundle configuration hasBundleConfiguration is false`() {
        val configuration: ProductConfiguration = mock()
        whenever(configuration.configurationType) doReturn ConfigurationType.UNKNOWN
        val selectedItems = setOf(
            ProductSelectorViewModel.SelectedItem.Product(456),
            ProductSelectorViewModel.SelectedItem.Product(789),
            ProductSelectorViewModel.SelectedItem.Product(123),
            ProductSelectorViewModel.SelectedItem.ConfigurableProduct(183, configuration)
        )
        sut.onProductsSelected(selectedItems)

        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_ADD,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_PRODUCT_COUNT to selectedItems.size,
                KEY_PRODUCT_ADDED_VIA to ProductAddedVia.MANUALLY.addedVia,
                AnalyticsTracker.KEY_HAS_BUNDLE_CONFIGURATION to false,
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    fun `when shipping button tapped, send tracks event`() {
        sut.onAddOrEditShipping()

        verify(tracker).track(AnalyticsEvent.ORDER_ADD_SHIPPING_TAPPED)
    }
    //endregion

    protected fun createSut(savedStateHandle: SavedStateHandle = savedState) {
        autoSyncPriceModifier = AutoSyncPriceModifier(createUpdateOrderUseCase)
        autoSyncOrder = AutoSyncOrder(createUpdateOrderUseCase)
        sut = OrderCreateEditViewModel(
            savedState = savedStateHandle,
            dispatchers = coroutinesTestRule.testDispatchers,
            orderDetailRepository = orderDetailRepository,
            orderCreateEditRepository = orderCreateEditRepository,
            createOrderItem = createOrderItemUseCase,
            parameterRepository = parameterRepository,
            autoSyncOrder = autoSyncOrder,
            autoSyncPriceModifier = autoSyncPriceModifier,
            tracker = tracker,
            barcodeScanningTracker = barcodeScanningTracker,
            productRepository = productListRepository,
            checkDigitRemoverFactory = checkDigitRemoverFactory,
            resourceProvider = resourceProvider,
            productRestrictions = productRestrictions,
            getTaxRatesInfoDialogState = GetTaxRatesInfoDialogViewState(
                orderCreateEditRepository,
                selectedSite,
                resourceProvider
            ),
            getAddressFromTaxRate = taxRateToAddress,
            getAutoTaxRateSetting = getAutoTaxRateSetting,
            getTaxRatePercentageValueText = getTaxRatePercentageValueText,
            getTaxRateLabel = getTaxRateLabel,
            prefs = prefs,
            orderCreationProductMapper = orderCreationProductMapper,
            adjustProductQuantity = AdjustProductQuantity(),
            mapFeeLineToCustomAmountUiModel = mapFeeLineToCustomAmountUiModel,
            currencySymbolFinder = currencySymbolFinder,
            totalsHelper = totalsHelper,
            dateUtils = mock(),
            getShippingMethodsWithOtherValue = mock()
        )
    }

    protected fun createOrderItem(withProductId: Long = 123, withVariationId: Long? = null) =
        if (withVariationId != null) {
            Order.Item.EMPTY.copy(
                productId = withProductId,
                itemId = (1L..1000000000L).random(),
                variationId = withVariationId,
                quantity = 1F,
            )
        } else {
            Order.Item.EMPTY.copy(
                productId = withProductId,
                itemId = (1L..1000000000L).random(),
                quantity = 1F,
            )
        }

    protected fun createProductItem(item: Order.Item? = null): OrderCreationProduct {
        val orderItem = item ?: createOrderItem()
        val productInfo = ProductInfo(
            imageUrl = "",
            isStockManaged = false,
            stockQuantity = 0.0,
            stockStatus = ProductStockStatus.InStock,
            productType = ProductType.SIMPLE,
            isConfigurable = false,
            pricePreDiscount = "$10",
            priceTotal = "$30",
            priceSubtotal = "$30",
            discountAmount = "$5",
            priceAfterDiscount = "$25",
            hasDiscount = true
        )
        return OrderCreationProduct.ProductItem(
            item = orderItem,
            productInfo = productInfo
        )
    }

    protected val orderStatusList = listOf(
        Order.OrderStatus("first key", "first status"),
        Order.OrderStatus("second key", "second status"),
        Order.OrderStatus("third key", "third status")
    )
}
