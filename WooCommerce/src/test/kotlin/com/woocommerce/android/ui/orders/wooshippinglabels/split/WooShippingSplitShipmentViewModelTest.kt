package com.woocommerce.android.ui.orders.wooshippinglabels.split

import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.SplitShipmentArgs
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import java.math.BigDecimal
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingSplitShipmentViewModelTest : BaseUnitTest() {

    private val currencyFormatter: CurrencyFormatter = org.mockito.kotlin.mock {
        on { formatCurrency(amount = any(), any(), any()) }.doAnswer { it.getArgument<BigDecimal>(0).toString() }
    }
    private val getSplitMovements: GetSplitMovements = mock()
    lateinit var sut: WooShippingSplitShipmentViewModel

    private fun createViewModel(
        shipmentArgs: SplitShipmentArgs
    ) {
        val savedState = WooShippingSplitShipmentFragmentArgs(shipmentArgs).toSavedStateHandle()
        sut = WooShippingSplitShipmentViewModel(
            savedState,
            currencyFormatter,
            getSplitMovements
        )
    }

    @Test
    fun `when split shipments is opened, then display the correct number of expandable products`() = testBlocking {
        val shipmentArgs = SplitShipmentArgs(
            orderId = 1L,
            storeOptions = StoreOptionsModel.EMPTY,
            shipments = defaultShipment
        )

        createViewModel(shipmentArgs)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val selectableItems = state.selectableItems.getValue(1)
        val expandableProducts = selectableItems.shippableItems
            .filterIsInstance<SelectableShippableItemUI.ExpandableSelectableShippableItemUI>()
        assertThat(expandableProducts.size).isEqualTo(2)
    }

    @Test
    fun `when split shipments is opened, then display the correct number of single selectable products`() =
        testBlocking {
            val shipmentArgs = SplitShipmentArgs(
                orderId = 1L,
                storeOptions = StoreOptionsModel.EMPTY,
                shipments = defaultShipment
            )

            createViewModel(shipmentArgs)

            sut.viewState.observeForTesting { }

            val state = sut.viewState.value!!

            val selectableItems = state.selectableItems.getValue(1)
            val expandableProducts = selectableItems.shippableItems
                .filterIsInstance<SelectableShippableItemUI.SingleSelectableShippableItemUI>()
            assertThat(expandableProducts.size).isEqualTo(1)
        }

    @Test
    fun `when an expandable product inner selection changes, then the product is updated`() = testBlocking {
        val shippableItemIndex = 2
        val shipmentArgs = SplitShipmentArgs(
            orderId = 1L,
            storeOptions = StoreOptionsModel.EMPTY,
            shipments = defaultShipment
        )

        createViewModel(shipmentArgs)

        sut.onUpdateSelection(1, shippableItemIndex, List(3) { it }.toSet())

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val selectableItems = state.selectableItems.getValue(1)
        val expandableProducts = selectableItems.shippableItems.filter {
            it is SelectableShippableItemUI.ExpandableSelectableShippableItemUI &&
                it.isSelected
        }
        assertThat(expandableProducts.size).isEqualTo(1)
    }

    @Test
    fun `when an expandable product selection changes, then the product is updated`() = testBlocking {
        val shippableItemIndex = 2
        val shipmentArgs = SplitShipmentArgs(
            orderId = 1L,
            storeOptions = StoreOptionsModel.EMPTY,
            shipments = defaultShipment
        )

        createViewModel(shipmentArgs)

        sut.onUpdateSelection(1, shippableItemIndex, null)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val selectableItems = state.selectableItems.getValue(1)
        val expandableProducts = selectableItems.shippableItems.filter {
            it is SelectableShippableItemUI.ExpandableSelectableShippableItemUI &&
                it.isSelected
        }
        assertThat(expandableProducts.size).isEqualTo(1)
    }

    @Test
    fun `when a single product selection changes, then the product is updated`() = testBlocking {
        val shippableItemIndex = 0
        val shipmentArgs = SplitShipmentArgs(
            orderId = 1L,
            storeOptions = StoreOptionsModel.EMPTY,
            shipments = defaultShipment
        )

        createViewModel(shipmentArgs)

        sut.onUpdateSelection(1, shippableItemIndex, null)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val selectableItems = state.selectableItems.getValue(1)
        val expandableProducts = selectableItems.shippableItems.filter {
            it is SelectableShippableItemUI.SingleSelectableShippableItemUI &&
                it.isSelected
        }
        assertThat(expandableProducts.size).isEqualTo(1)
    }

    @Test
    fun `when moving a shippable item to a new shipment, then a new shipment is created`() = testBlocking {
        val shipmentArgs = SplitShipmentArgs(
            orderId = 1L,
            storeOptions = StoreOptionsModel.EMPTY,
            shipments = defaultShipment
        )
        val updatedCurrentShipmentItems = defaultShipment.getValue(1).subList(fromIndex = 1, toIndex = 3)
        val updatedShipmentItems = defaultShipment.getValue(1).subList(fromIndex = 0, toIndex = 1)

        val movement = SplitMovement(
            currentShipment = 1,
            updatedCurrentShipmentItems = updatedCurrentShipmentItems,
            updatedShipment = 2,
            updatedShipmentItems = updatedShipmentItems
        )

        createViewModel(shipmentArgs)

        sut.onUpdateShipment(movement)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val selectableItems = state.selectableItems
        assertThat(selectableItems.size).isEqualTo(2)
        assertThat(selectableItems.getValue(1).shippableItems.size).isEqualTo(updatedCurrentShipmentItems.size)
        assertThat(selectableItems.getValue(2).shippableItems.size).isEqualTo(updatedShipmentItems.size)
    }

    @Test
    fun `when moving a shippable item to a shipment that already has the items, then the items are combined`() =
        testBlocking {
            val shipmentArgs = SplitShipmentArgs(
                orderId = 1L,
                storeOptions = StoreOptionsModel.EMPTY,
                shipments = twoShipment
            )
            val updatedCurrentShipmentItems = defaultShipment.getValue(1)
            val updatedShipmentItems = listOf(
                ShippableItemModel(
                    itemId = 3L,
                    productId = 3L,
                    title = "Another product with quantity 3",
                    price = BigDecimal(10),
                    quantity = 3f,
                    imageUrl = null,
                    currency = "USD",
                    length = 3f,
                    width = 3f,
                    height = 3f,
                    weight = 8f
                )
            )

            val movement = SplitMovement(
                currentShipment = 1,
                updatedCurrentShipmentItems = updatedCurrentShipmentItems,
                updatedShipment = 2,
                updatedShipmentItems = updatedShipmentItems
            )

            createViewModel(shipmentArgs)

            sut.onUpdateShipment(movement)

            sut.viewState.observeForTesting { }

            val state = sut.viewState.value!!

            val selectableItems = state.selectableItems
            assertThat(selectableItems.size).isEqualTo(2)
            assertThat(selectableItems.getValue(2).shippableItems.size).isEqualTo(1)
            val item = selectableItems
                .getValue(2)
                .shippableItems
                .first() as SelectableShippableItemUI.ExpandableSelectableShippableItemUI
            // Assert that quantity is combined 3f + 3f
            assertThat(item.shippableItem.quantity).isEqualTo(6f)
        }

    private val defaultShipment = mapOf(
        1 to listOf(
            ShippableItemModel(
                itemId = 1L,
                productId = 1L,
                title = "A product with quantity 1",
                price = BigDecimal(30),
                quantity = 1f,
                imageUrl = null,
                currency = "USD",
                length = 3f,
                width = 3f,
                height = 3f,
                weight = 8f
            ),
            ShippableItemModel(
                itemId = 2L,
                productId = 2L,
                title = "A product with quantity 5",
                price = BigDecimal(10),
                quantity = 5f,
                imageUrl = null,
                currency = "USD",
                length = 3f,
                width = 3f,
                height = 3f,
                weight = 8f
            ),
            ShippableItemModel(
                itemId = 3L,
                productId = 3L,
                title = "Another product with quantity 3",
                price = BigDecimal(10),
                quantity = 3f,
                imageUrl = null,
                currency = "USD",
                length = 3f,
                width = 3f,
                height = 3f,
                weight = 8f
            )
        )
    )
    private val twoShipment = mapOf(
        1 to listOf(
            ShippableItemModel(
                itemId = 1L,
                productId = 1L,
                title = "A product with quantity 1",
                price = BigDecimal(30),
                quantity = 1f,
                imageUrl = null,
                currency = "USD",
                length = 3f,
                width = 3f,
                height = 3f,
                weight = 8f
            ),
            ShippableItemModel(
                itemId = 2L,
                productId = 2L,
                title = "A product with quantity 5",
                price = BigDecimal(10),
                quantity = 5f,
                imageUrl = null,
                currency = "USD",
                length = 3f,
                width = 3f,
                height = 3f,
                weight = 8f
            )
        ),
        2 to listOf(
            ShippableItemModel(
                itemId = 3L,
                productId = 3L,
                title = "Another product with quantity 3",
                price = BigDecimal(10),
                quantity = 3f,
                imageUrl = null,
                currency = "USD",
                length = 3f,
                width = 3f,
                height = 3f,
                weight = 8f
            )
        )
    )
}
