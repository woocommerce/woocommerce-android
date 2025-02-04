package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.common.data.WooPosGetVariationById
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCOrderStore

class WooPosTotalsRepositoryTest {
    private val orderCreateEditRepository: OrderCreateEditRepository = mock()
    private val getProductById: WooPosGetProductById = mock()
    private val getVariationById: WooPosGetVariationById = mock()
    private val dateUtils: DateUtils = mock()
    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val orderMapper: OrderMapper = mock()
    private val resourceProvider: ResourceProvider = mock()

    private lateinit var repository: WooPosTotalsRepository

    private val product1 = ProductHelper.getDefaultNewProduct(
        productType = ProductType.SIMPLE,
        isVirtual = false
    ).copy(remoteId = 1L)

    @Test
    fun `given empty product list, when createOrderWithProducts called, then return error`() = runTest {
        // GIVEN
        repository = createRepository()
        val itemClickedData = emptyList<WooPosItemsViewModel.ItemClickedData>()

        // WHEN
        val result = runCatching { repository.createOrderWithProducts(itemClickedData) }

        // THEN
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `given product ids without duplicates, when createOrderWithProducts, then items all quantity one`() = runTest {
        // GIVEN
        repository = createRepository()
        val itemClickedData = listOf(
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 1L
            ),
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 2L
            ),
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 3L
            )
        )

        whenever(getProductById(1L)).thenReturn(product1)
        whenever(getProductById(2L)).thenReturn(product1)
        whenever(getProductById(3L)).thenReturn(product1)

        // WHEN
        repository.createOrderWithProducts(itemClickedData)

        // THEN
        val orderCapture = argumentCaptor<Order>()
        verify(orderCreateEditRepository).createOrUpdateOrder(
            orderCapture.capture(),
            eq("")
        )

        assertThat(orderCapture.lastValue.items.size).isEqualTo(3)
        assertThat(orderCapture.lastValue.items.map { it.quantity }).containsOnly(1f)
        assertThat(orderCapture.lastValue.items[0].name).isEqualTo(product1.name)
    }

    @Test
    fun `given product id, when createOrderWithProducts, then item name matches original product`() = runTest {
        // GIVEN
        repository = createRepository()
        val itemClickedData = listOf(
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 1L
            )
        )

        whenever(getProductById(1L)).thenReturn(product1)

        // WHEN
        repository.createOrderWithProducts(itemClickedData)

        // THEN
        val orderCapture = argumentCaptor<Order>()
        verify(orderCreateEditRepository).createOrUpdateOrder(
            orderCapture.capture(),
            eq("")
        )

        assertThat(orderCapture.lastValue.items.size).isEqualTo(1)
        assertThat(orderCapture.lastValue.items[0].name).isEqualTo(product1.name)
    }

    @Test
    fun `given product ids with duplicates, when createOrderWithProducts, then items quantity is correct`() = runTest {
        // GIVEN
        repository = createRepository()
        val itemClickedData = listOf(
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 1L
            ),
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 1L
            ),
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 2L
            ),
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 3L
            ),
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 3L
            ),
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 3L
            )
        )

        whenever(getProductById(1L)).thenReturn(product1)
        whenever(getProductById(2L)).thenReturn(product1)
        whenever(getProductById(3L)).thenReturn(product1)

        // WHEN
        repository.createOrderWithProducts(itemClickedData)

        // THEN
        val orderCapture = argumentCaptor<Order>()
        verify(orderCreateEditRepository).createOrUpdateOrder(
            orderCapture.capture(),
            eq("")
        )

        assertThat(orderCapture.lastValue.items.size).isEqualTo(3)
        assertThat(orderCapture.lastValue.items.map { it.quantity }).containsExactly(2f, 1f, 3f)
    }

    @Test
    fun `given product ids, when createOrder with some invalid ids, then return failure`() = runTest {
        // GIVEN
        repository = createRepository()
        val itemClickedData = listOf(
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 1L
            ),
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = -1L
            ),
            WooPosItemsViewModel.ItemClickedData.SimpleProduct(
                id = 3L
            )
        )
        val mockOrder: Order = mock()
        whenever(orderCreateEditRepository.createOrUpdateOrder(any(), eq(""))).thenReturn(Result.success(mockOrder))

        // WHEN
        val result = runCatching { repository.createOrderWithProducts(itemClickedData) }

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Invalid product ID: -1")
        verify(orderCreateEditRepository, never()).createOrUpdateOrder(any(), eq(""))
    }

    private fun createRepository() = WooPosTotalsRepository(
        orderCreateEditRepository,
        dateUtils,
        getProductById,
        getVariationById,
        orderStore,
        selectedSite,
        orderMapper,
        resourceProvider,
    )
}
