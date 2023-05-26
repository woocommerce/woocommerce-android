package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_SEARCH_VIA_SKU_FAILURE
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_SEARCH_VIA_SKU_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_ADDED_VIA
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SCANNING_FAILURE_REASON
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SCANNING_SOURCE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Failed
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Succeeded
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WCProductStore
import java.math.BigDecimal
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
abstract class UnifiedOrderEditViewModelTest : BaseUnitTest() {
    protected lateinit var sut: OrderCreateEditViewModel
    protected lateinit var viewState: OrderCreateEditViewModel.ViewState
    protected lateinit var savedState: SavedStateHandle
    protected lateinit var mapItemToProductUIModel: MapItemToProductUiModel
    protected lateinit var createUpdateOrderUseCase: CreateUpdateOrder
    protected lateinit var autoSyncPriceModifier: AutoSyncPriceModifier
    protected lateinit var autoSyncOrder: AutoSyncOrder
    protected lateinit var createOrderItemUseCase: CreateOrderItem
    protected lateinit var orderCreateEditRepository: OrderCreateEditRepository
    protected lateinit var orderDetailRepository: OrderDetailRepository
    protected lateinit var parameterRepository: ParameterRepository
    private lateinit var determineMultipleLinesContext: DetermineMultipleLinesContext
    protected lateinit var tracker: AnalyticsTrackerWrapper
    private lateinit var codeScanner: CodeScanner
    lateinit var productListRepository: ProductListRepository

    protected val defaultOrderValue = Order.EMPTY.copy(id = 123)

    @Before
    fun setUp() {
        initMocks()
        createSut()
    }

    protected abstract val mode: OrderCreateEditViewModel.Mode
    protected abstract val sku: String

    private fun initMocks() {
        val defaultOrderItem = createOrderItem()
        val emptyOrder = Order.EMPTY
        viewState = OrderCreateEditViewModel.ViewState()
        savedState = spy(OrderCreateEditFormFragmentArgs(mode, sku).toSavedStateHandle()) {
            on { getLiveData(viewState.javaClass.name, viewState) } doReturn MutableLiveData(viewState)
            on { getLiveData(eq(Order.EMPTY.javaClass.name), any<Order>()) } doReturn MutableLiveData(emptyOrder)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(Order.EMPTY))
        }
        createOrderItemUseCase = mock {
            onBlocking { invoke(123, null) } doReturn defaultOrderItem
            onBlocking { invoke(456, null) } doReturn createOrderItem(456)
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
            onBlocking { placeOrder(defaultOrderValue) } doReturn Result.success(defaultOrderValue)
        }
        orderDetailRepository = mock {
            on { getOrderStatusOptions() } doReturn orderStatusList
        }
        mapItemToProductUIModel = mock {
            onBlocking { invoke(any()) } doReturn ProductUIModel(
                item = defaultOrderItem,
                imageUrl = "",
                isStockManaged = false,
                stockQuantity = 0.0,
                stockStatus = ProductStockStatus.InStock
            )
        }
        determineMultipleLinesContext = mock {
            on { invoke(any()) } doReturn OrderCreateEditViewModel.MultipleLinesContext.None
        }
        tracker = mock()
        codeScanner = mock()
        productListRepository = mock()
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
            ),
        )
    }

    @Test
    fun `when customer address edited, send tracks event`() {
        sut.onCustomerAddressEdited(0, Address.EMPTY, Address.EMPTY)

        verify(tracker).track(
            AnalyticsEvent.ORDER_CUSTOMER_ADD,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_HAS_DIFFERENT_SHIPPING_DETAILS to false,
            )
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
    fun `when customer note added or edited, send tracks event`() {
        sut.onCustomerNoteEdited("")

        verify(tracker).track(
            AnalyticsEvent.ORDER_NOTE_ADD,
            mapOf(
                AnalyticsTracker.KEY_PARENT_ID to 0L,
                AnalyticsTracker.KEY_STATUS to Order.Status.Pending,
                AnalyticsTracker.KEY_TYPE to AnalyticsTracker.Companion.OrderNoteType.CUSTOMER,
                AnalyticsTracker.KEY_FLOW to tracksFlow,
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
                AnalyticsTracker.KEY_FLOW to tracksFlow
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
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
        )
    }

    @Test
    fun `when product quantity decreased, send tracks event`() {
        val productId = 1L
        val products = OrderTestUtils.generateTestOrderItems(count = 1, productId = productId, quantity = 3F)
        val order = defaultOrderValue.copy(items = products)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onDecreaseProductsQuantity(productId)
        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_QUANTITY_CHANGE,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
        )
    }

    @Test
    fun `when product quantity decreased but quantity 1, don't send tracks event`() {
        val productId = 1L
        val products = OrderTestUtils.generateTestOrderItems(count = 1, productId = productId, quantity = 1F)
        val order = defaultOrderValue.copy(items = products)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onDecreaseProductsQuantity(productId)
        verify(tracker, never()).track(
            AnalyticsEvent.ORDER_PRODUCT_QUANTITY_CHANGE,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
        )
    }

    @Test
    fun `when product removed, send tracks event`() {
        val productId = 1L
        val products = OrderTestUtils.generateTestOrderItems(count = 1, productId = productId, quantity = 3F)
        val order = defaultOrderValue.copy(items = products)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onRemoveProduct(products.first())
        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_REMOVE,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
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
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
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
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
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
            properties = mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow),
            errorContext = sut::class.java.simpleName,
            errorType = wooError.type.name,
            errorDescription = wooError.message
        )
    }

    // region Scanned and Deliver
    @Test
    fun `when code scanner returns code, then set isUpdatingOrderDraft to true`() {
        createSut()
        whenever(codeScanner.startScan()).thenAnswer {
            flow<CodeScannerStatus> {
                emit(CodeScannerStatus.Success("12345"))
            }
        }
        var isUpdatingOrderDraft: Boolean? = null
        sut.viewStateData.observeForever { _, viewState ->
            isUpdatingOrderDraft = viewState.isUpdatingOrderDraft
        }

        sut.onScanClicked()

        assertTrue(isUpdatingOrderDraft!!)
    }

    @Test
    fun `when SKU search succeeds, then set isUpdatingOrderDraft to false`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()

            assertFalse(isUpdatingOrderDraft!!)
        }
    }

    @Test
    fun `when SKU search succeeds, then add the scanned product`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()

            assertThat(newOrder?.getProductIds()?.any { it == 10L }).isTrue()
        }
    }

    @Test
    fun `when SKU search succeeds for variable-subscription product, then add the scanned product`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()

            assertThat(newOrder?.getProductIds()?.any { it == 10L }).isTrue()
        }
    }

    @Test
    fun `when parent variable product is scanned, then trigger proper event`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()

            assertThat(sut.event.value).isInstanceOf(
                OnAddingProductViaScanningFailed::class.java
            )
        }
    }

    @Test
    fun `when parent variable product is scanned, then trigger event with proper message`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()

            assertThat(
                (sut.event.value as OnAddingProductViaScanningFailed).message
            ).isEqualTo(R.string.order_creation_barcode_scanning_unable_to_add_variable_product)
        }
    }

    @Test
    fun `when parent variable product is scanned, then do not track any product search via sku success event`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()

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
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()

            assertThat(newOrder?.getProductIds()?.any { it == 10L }).isTrue()
        }
    }

    @Test
    fun `when SKU search succeeds for variable parent product, then trigger proper failed event`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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
            sut.onScanClicked()

            assertThat(sut.event.value).isInstanceOf(OnAddingProductViaScanningFailed::class.java)
        }
    }

    @Test
    fun `when SKU search succeeds for variable parent product, then trigger failed event with proper message`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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
            sut.onScanClicked()

            assertThat(
                (sut.event.value as OnAddingProductViaScanningFailed).message
            ).isEqualTo(R.string.order_creation_barcode_scanning_unable_to_add_variable_product)
        }
    }

    @Test
    fun `when code scanner fails to recognize the barcode, then trigger proper event`() {
        createSut()
        whenever(codeScanner.startScan()).thenAnswer {
            flow<CodeScannerStatus> {
                emit(
                    CodeScannerStatus.Failure(
                        error = "Failed to recognize the barcode",
                        type = CodeScanningErrorType.NotFound
                    )
                )
            }
        }

        sut.onScanClicked()

        assertThat(sut.event.value).isInstanceOf(OnAddingProductViaScanningFailed::class.java)
    }

    @Test
    fun `when code scanner fails to recognize the barcode, then proper message is sent`() {
        createSut()
        whenever(codeScanner.startScan()).thenAnswer {
            flow<CodeScannerStatus> {
                emit(
                    CodeScannerStatus.Failure(
                        error = "Failed to recognize the barcode",
                        type = CodeScanningErrorType.NotFound
                    )
                )
            }
        }

        sut.onScanClicked()

        assertThat((sut.event.value as OnAddingProductViaScanningFailed).message).isEqualTo(
            R.string.order_creation_barcode_scanning_scanning_failed
        )
    }

    @Test
    fun `given code scanner fails to recognize the barcode, when retry clicked, then restart code scanning`() {
        createSut()
        whenever(codeScanner.startScan()).thenAnswer {
            flow<CodeScannerStatus> {
                emit(
                    CodeScannerStatus.Failure(
                        error = "Failed to recognize the barcode",
                        type = CodeScanningErrorType.NotFound
                    )
                )
            }
        }

        sut.onScanClicked()
        (sut.event.value as OnAddingProductViaScanningFailed).retry.onClick(any())

        verify(codeScanner).startScan()
    }

    @Test
    fun `when product search by SKU fails, then trigger proper event`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(null)

            sut.onScanClicked()

            assertThat(sut.event.value).isInstanceOf(OnAddingProductViaScanningFailed::class.java)
        }
    }

    @Test
    fun `when product search by SKU succeeds but has empty result, then trigger proper event`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            sut.onScanClicked()

            assertThat(sut.event.value).isInstanceOf(OnAddingProductViaScanningFailed::class.java)
        }
    }

    @Test
    fun `when product search by SKU fails, then proper message is displayed`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(null)

            sut.onScanClicked()

            assertThat(
                (sut.event.value as OnAddingProductViaScanningFailed).message
            ).isEqualTo(R.string.order_creation_barcode_scanning_unable_to_add_product)
        }
    }

    @Test
    fun `given product search by SKU fails, when retry clicked, then restart scanning`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(null)

            sut.onScanClicked()
            (sut.event.value as OnAddingProductViaScanningFailed).retry.onClick(any())

            verify(codeScanner).startScan()
        }
    }

    @Test
    fun `given that same variable subscription product scanned thrice, then increment the product quantity accordingly`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()
            sut.onScanClicked()
            sut.onScanClicked()

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
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()
            sut.onScanClicked()
            sut.onScanClicked()

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

        verify(tracker).track(AnalyticsEvent.ORDER_CREATION_PRODUCT_BARCODE_SCANNING_TAPPED)
    }

    @Test
    fun `when scan success, then track proper event`() {
        createSut()
        whenever(codeScanner.startScan()).thenAnswer {
            flow<CodeScannerStatus> {
                emit(CodeScannerStatus.Success("12345"))
            }
        }

        sut.onScanClicked()

        verify(tracker).track(
            eq(AnalyticsEvent.BARCODE_SCANNING_SUCCESS),
            any()
        )
    }

    @Test
    fun `when scan success, then track event with proper source`() {
        createSut()
        whenever(codeScanner.startScan()).thenAnswer {
            flow<CodeScannerStatus> {
                emit(CodeScannerStatus.Success("12345"))
            }
        }

        sut.onScanClicked()

        verify(tracker).track(
            AnalyticsEvent.BARCODE_SCANNING_SUCCESS,
            mapOf(
                KEY_SCANNING_SOURCE to "order_creation"
            )
        )
    }

    @Test
    fun `when scan failure, then track proper event`() {
        createSut()
        whenever(codeScanner.startScan()).thenAnswer {
            flow<CodeScannerStatus> {
                emit(
                    CodeScannerStatus.Failure(
                        error = "Failed to recognize the barcode",
                        type = CodeScanningErrorType.NotFound
                    )
                )
            }
        }

        sut.onScanClicked()

        verify(tracker).track(
            eq(AnalyticsEvent.BARCODE_SCANNING_FAILURE),
            any()
        )
    }

    @Test
    fun `when scan failure, then track event with proper source`() {
        createSut()
        whenever(codeScanner.startScan()).thenAnswer {
            flow<CodeScannerStatus> {
                emit(
                    CodeScannerStatus.Failure(
                        error = "Failed to recognize the barcode",
                        type = CodeScanningErrorType.NotFound
                    )
                )
            }
        }

        sut.onScanClicked()

        verify(tracker).track(
            AnalyticsEvent.BARCODE_SCANNING_FAILURE,
            mapOf(
                KEY_SCANNING_SOURCE to "order_creation",
                KEY_SCANNING_FAILURE_REASON to CodeScanningErrorType.NotFound.toString()
            )
        )
    }

    @Test
    fun `when scan failure, then track event with proper failure reason`() {
        createSut()
        whenever(codeScanner.startScan()).thenAnswer {
            flow<CodeScannerStatus> {
                emit(
                    CodeScannerStatus.Failure(
                        error = "Failed to recognize the barcode",
                        type = CodeScanningErrorType.NotFound
                    )
                )
            }
        }

        sut.onScanClicked()

        verify(tracker).track(
            AnalyticsEvent.BARCODE_SCANNING_FAILURE,
            mapOf(
                KEY_SCANNING_SOURCE to "order_creation",
                KEY_SCANNING_FAILURE_REASON to CodeScanningErrorType.NotFound.toString()
            )
        )
    }

    @Test
    fun `given product search via sku succeeds, then track proper event`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()

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
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()

            verify(tracker).track(
                PRODUCT_SEARCH_VIA_SKU_SUCCESS,
                mapOf(
                    KEY_SCANNING_SOURCE to "order_creation"
                )
            )
        }
    }

    @Test
    fun `given product search via sku fails, then track proper event`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(null)

            sut.onScanClicked()

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
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(null)

            sut.onScanClicked()

            verify(tracker).track(
                PRODUCT_SEARCH_VIA_SKU_FAILURE,
                mapOf(
                    KEY_SCANNING_SOURCE to "order_creation",
                    KEY_SCANNING_FAILURE_REASON to "Product search via SKU API call failed"
                )
            )
        }
    }

    @Test
    fun `given product search via sku fails, then track event with proper reason`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(null)

            sut.onScanClicked()

            verify(tracker).track(
                PRODUCT_SEARCH_VIA_SKU_FAILURE,
                mapOf(
                    KEY_SCANNING_SOURCE to "order_creation",
                    KEY_SCANNING_FAILURE_REASON to "Product search via SKU API call failed"
                )
            )
        }
    }

    @Test
    fun `given product search via sku fails when trying to add parent variable product, then track event with proper reason`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()

            verify(tracker).track(
                PRODUCT_SEARCH_VIA_SKU_FAILURE,
                mapOf(
                    KEY_SCANNING_SOURCE to "order_creation",
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
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            sut.onScanClicked()

            verify(tracker).track(
                PRODUCT_SEARCH_VIA_SKU_FAILURE,
                mapOf(
                    KEY_SCANNING_SOURCE to "order_creation",
                    KEY_SCANNING_FAILURE_REASON to "Empty data response (no product found for the SKU)"
                )
            )
        }
    }

    @Test
    fun `given variable product from order creation screen, when product added via scanning, then track correct source`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()

            verify(tracker).track(
                AnalyticsEvent.ORDER_PRODUCT_ADD,
                mapOf(
                    AnalyticsTracker.KEY_FLOW to tracksFlow,
                    AnalyticsTracker.KEY_PRODUCT_COUNT to 1,
                    KEY_SCANNING_SOURCE to ScanningSource.ORDER_CREATION.source,
                    KEY_PRODUCT_ADDED_VIA to ProductAddedVia.SCANNING.addedVia,
                )
            )
        }
    }

    @Test
    fun `given non-variable product from order creation screen, when product added via scanning, then track correct source`() {
        testBlocking {
            createSut()
            whenever(codeScanner.startScan()).thenAnswer {
                flow<CodeScannerStatus> {
                    emit(CodeScannerStatus.Success("12345"))
                }
            }
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

            sut.onScanClicked()

            verify(tracker).track(
                AnalyticsEvent.ORDER_PRODUCT_ADD,
                mapOf(
                    AnalyticsTracker.KEY_FLOW to tracksFlow,
                    AnalyticsTracker.KEY_PRODUCT_COUNT to 1,
                    KEY_SCANNING_SOURCE to ScanningSource.ORDER_CREATION.source,
                    KEY_PRODUCT_ADDED_VIA to ProductAddedVia.SCANNING.addedVia,
                )
            )
        }
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
            mapItemToProductUiModel = mapItemToProductUIModel,
            createOrderItem = createOrderItemUseCase,
            determineMultipleLinesContext = determineMultipleLinesContext,
            parameterRepository = parameterRepository,
            autoSyncOrder = autoSyncOrder,
            autoSyncPriceModifier = autoSyncPriceModifier,
            tracker = tracker,
            codeScanner = codeScanner,
            productRepository = productListRepository
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

    protected val orderStatusList = listOf(
        Order.OrderStatus("first key", "first status"),
        Order.OrderStatus("second key", "second status"),
        Order.OrderStatus("third key", "third status")
    )
}
