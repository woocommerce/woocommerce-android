package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R.string
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.ui.products.ProductBackorderStatus.No
import com.woocommerce.android.ui.products.ProductBackorderStatus.Yes
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.ProductInventoryViewModel.ViewState
import com.woocommerce.android.ui.products.ProductStockStatus.InStock
import com.woocommerce.android.ui.products.ProductStockStatus.OutOfStock
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.TestDispatcher
import com.woocommerce.android.viewmodel.test
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@InternalCoroutinesApi
class ProductInventoryViewModelTest : BaseUnitTest() {
    private val productDetailRepository: ProductDetailRepository = mock()

    private val takenSku = "taken"

    private val initialData = InventoryData(
        "SKU123",
        isStockManaged = false,
        isSoldIndividually = false,
        stockStatus = InStock,
        stockQuantity = 10,
        backorderStatus = No
    )

    private val expectedData = InventoryData(
        "SKU321",
        isStockManaged = true,
        isSoldIndividually = true,
        stockStatus = OutOfStock,
        stockQuantity = 0,
        backorderStatus = Yes
    )

    private val coroutineDispatchers = CoroutineDispatchers(
        TestDispatcher,
        TestDispatcher,
        TestDispatcher
    )

    private lateinit var viewModel: ProductInventoryViewModel

    @Before
    fun setup() {
        viewModel = createViewModel(RequestCodes.PRODUCT_DETAIL_INVENTORY)
    }

    private fun createViewModel(requestCode: Int): ProductInventoryViewModel {
        val savedState = SavedStateWithArgs(
            SavedStateHandle(),
            null,
            ProductInventoryFragmentArgs(requestCode, initialData, initialData.sku!!)
        )
        return spy(
            ProductInventoryViewModel(
                savedState,
                coroutineDispatchers,
                productDetailRepository
            )
        )
    }

    @Test
    fun `Test that the initial data is displayed correctly`() = test {
        var actual: InventoryData? = null
        viewModel.viewStateData.observeForever { _, new ->
            actual = new.inventoryData
        }

        assertThat(actual).isEqualTo(initialData)
    }

    @Test
    fun `Test that when data is changed the view state is updated`() = test {
        var actual: InventoryData? = null
        viewModel.viewStateData.observeForever { _, new ->
            actual = new.inventoryData
        }

        viewModel.onDataChanged(
            expectedData.sku,
            expectedData.backorderStatus,
            expectedData.isSoldIndividually,
            expectedData.isStockManaged,
            expectedData.stockQuantity,
            expectedData.stockStatus
        )

        assertThat(actual).isEqualTo(expectedData)
    }

    @Test
    fun `Test that an error is shown and done button disabled if SKU is already taken`() = test {
        whenever(productDetailRepository.isSkuAvailableLocally(takenSku)).thenReturn(false)
        whenever(productDetailRepository.isSkuAvailableRemotely(expectedData.sku!!)).thenReturn(true)
        whenever(productDetailRepository.isSkuAvailableLocally(expectedData.sku!!)).thenReturn(true)

        var actual: ViewState? = null
        viewModel.viewStateData.observeForever { _, new ->
            actual = new
        }

        viewModel.onSkuChanged(takenSku)

        assertThat(actual?.inventoryData?.sku).isEqualTo(takenSku)
        assertThat(actual?.isDoneButtonVisible).isTrue()
        assertThat(actual?.isDoneButtonDisabled).isTrue()
        assertThat(actual?.skuErrorMessage).isEqualTo(string.product_inventory_update_sku_error)

        viewModel.onSkuChanged(expectedData.sku!!)

        assertThat(actual?.inventoryData?.sku).isEqualTo(expectedData.sku)
        assertThat(actual?.isDoneButtonVisible).isTrue()

        assertThat(actual?.isDoneButtonVisible).isTrue()
        assertThat(actual?.isDoneButtonDisabled).isFalse()
        assertThat(actual?.skuErrorMessage).isEqualTo(0)
    }

    @Test
    fun `Test that a discard dialog isn't shown if no data changed`() = test {
        val events = mutableListOf<Event>()
        viewModel.event.observeForever {
            events.add(it)
        }

        assertThat(events).isEmpty()

        viewModel.onExit()

        assertThat(events.singleOrNull { it is Exit }).isNotNull
        assertThat(events.any { it is ShowDialog }).isFalse()
        assertThat(events.any { it is ExitWithResult<*> }).isFalse()
    }

    @Test
    fun `Test that a discard dialog is shown if data changed`() = test {
        val events = mutableListOf<Event>()
        viewModel.event.observeForever {
            events.add(it)
        }

        viewModel.onDataChanged(expectedData.sku)

        viewModel.onExit()

        assertThat(events.singleOrNull { it is ShowDialog }).isNotNull
        assertThat(events.any { it is ExitWithResult<*> }).isFalse()
        assertThat(events.any { it is Exit }).isFalse()
    }

    @Test
    fun `Test that a the correct data is returned when Done button clicked`() = test {
        val events = mutableListOf<Event>()
        viewModel.event.observeForever {
            events.add(it)
        }

        viewModel.onDataChanged(
            expectedData.sku,
            expectedData.backorderStatus,
            expectedData.isSoldIndividually,
            expectedData.isStockManaged,
            expectedData.stockQuantity,
            expectedData.stockStatus
        )

        viewModel.onDoneButtonClicked()

        assertThat(events.any { it is ShowDialog }).isFalse()
        assertThat(events.any { it is Exit }).isFalse()

        @Suppress("UNCHECKED_CAST")
        val result = events.single { it is ExitWithResult<*> } as ExitWithResult<InventoryData>

        assertThat(result.data).isEqualTo(expectedData)
    }

    @Test
    fun `Test that the done button is only shown if data changed`() = test {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new ->
            viewState = new
        }

        assertThat(viewState?.isDoneButtonVisible).isFalse()

        viewModel.onDataChanged(backorderStatus = expectedData.backorderStatus)

        assertThat(viewState?.isDoneButtonVisible).isTrue()

        viewModel.onDataChanged(backorderStatus = initialData.backorderStatus)

        assertThat(viewState?.isDoneButtonVisible).isFalse()
    }

    @Test
    fun `Test that the individual sale switch is visible for products`() = test {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new ->
            viewState = new
        }

        assertThat(viewState?.isIndividualSaleSwitchVisible).isTrue()
    }

    @Test
    fun `Test that the individual sale switch is not visible for variations`() = test {
        viewModel = createViewModel(RequestCodes.VARIATION_DETAIL_INVENTORY)

        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new ->
            viewState = new
        }

        assertThat(viewState?.isIndividualSaleSwitchVisible).isFalse()
    }
}
