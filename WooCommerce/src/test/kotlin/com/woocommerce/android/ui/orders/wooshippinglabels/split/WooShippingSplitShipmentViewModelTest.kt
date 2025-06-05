package com.woocommerce.android.ui.orders.wooshippinglabels.split

import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.wooshippinglabels.SplitShipment
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.SplitShipmentArgs
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
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
    private val splitShipment: SplitShipment = mock()
    lateinit var sut: WooShippingSplitShipmentViewModel

    private fun createViewModel(
        shipmentArgs: SplitShipmentArgs = SplitShipmentArgs(
            orderId = 1L,
            storeOptions = StoreOptionsModel.EMPTY,
            shipments = defaultShipments
        )
    ) {
        val savedState = WooShippingSplitShipmentFragmentArgs(shipmentArgs).toSavedStateHandle()
        sut = WooShippingSplitShipmentViewModel(
            savedState,
            currencyFormatter,
            getSplitMovements,
            splitShipment,
            TestScope(coroutinesTestRule.testDispatcher)
        )
    }

    @Test
    fun `when split shipments is opened, then display the correct number of expandable products`() = testBlocking {
        createViewModel()

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val selectableItems = state.selectableItems[0]!!
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
                shipments = defaultShipments
            )

            createViewModel(shipmentArgs)

            sut.viewState.observeForTesting { }

            val state = sut.viewState.value!!

            val selectableItems = state.selectableItems[0]!!
            val expandableProducts = selectableItems.shippableItems
                .filterIsInstance<SelectableShippableItemUI.SingleSelectableShippableItemUI>()
            assertThat(expandableProducts.size).isEqualTo(1)
        }

    @Test
    fun `when an expandable product inner selection changes, then the product is updated`() = testBlocking {
        val shippableItemIndex = 2

        createViewModel()

        sut.onUpdateSelection(shippableItemIndex, List(3) { it }.toSet())

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val selectableItems = state.selectableItems[0]!!
        val expandableProducts = selectableItems.shippableItems.filter {
            it is SelectableShippableItemUI.ExpandableSelectableShippableItemUI &&
                it.isSelected
        }
        assertThat(expandableProducts.size).isEqualTo(1)
    }

    @Test
    fun `when an expandable product selection changes, then the product is updated`() = testBlocking {
        val shippableItemIndex = 2

        createViewModel()

        sut.onUpdateSelection(shippableItemIndex, null)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val selectableItems = state.selectableItems[0]!!
        val expandableProducts = selectableItems.shippableItems.filter {
            it is SelectableShippableItemUI.ExpandableSelectableShippableItemUI &&
                it.isSelected
        }
        assertThat(expandableProducts.size).isEqualTo(1)
    }

    @Test
    fun `when a single product selection changes, then the product is updated`() = testBlocking {
        val shippableItemIndex = 0

        createViewModel()

        sut.onUpdateSelection(shippableItemIndex, null)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val selectableItems = state.selectableItems[0]!!
        val expandableProducts = selectableItems.shippableItems.filter {
            it is SelectableShippableItemUI.SingleSelectableShippableItemUI &&
                it.isSelected
        }
        assertThat(expandableProducts.size).isEqualTo(1)
    }

    @Test
    fun `when moving a shippable item to a new shipment, then a new shipment is created`() = testBlocking {
        val updatedCurrentShipmentItems = defaultShipments[0].items.subList(fromIndex = 1, toIndex = 3)
        val updatedShipmentItems = defaultShipments[0].items.subList(fromIndex = 0, toIndex = 1)

        val movement = SplitMovement(
            sourceShipmentKey = 0,
            updatedSourceShipmentItems = updatedCurrentShipmentItems,
            destinationShipmentKey = 1,
            movingShipmentItems = updatedShipmentItems
        )

        createViewModel()

        sut.onUpdateShipment(movement)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val selectableItems = state.selectableItems
        assertThat(selectableItems.size).isEqualTo(2)
        assertThat(selectableItems[0]!!.shippableItems.size).isEqualTo(updatedCurrentShipmentItems.size)
        assertThat(selectableItems[1]!!.shippableItems.size).isEqualTo(updatedShipmentItems.size)
    }

    @Test
    fun `when moving a shippable item to a shipment that already has the items, then the items are combined`() =
        testBlocking {
            val shipmentArgs = SplitShipmentArgs(
                orderId = 1L,
                storeOptions = StoreOptionsModel.EMPTY,
                shipments = twoShipments
            )
            val updatedCurrentShipmentItems = defaultShipments[0].items
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
                sourceShipmentKey = 0,
                updatedSourceShipmentItems = updatedCurrentShipmentItems,
                destinationShipmentKey = 1,
                movingShipmentItems = updatedShipmentItems
            )

            createViewModel(shipmentArgs)

            sut.onUpdateShipment(movement)

            sut.viewState.observeForTesting { }

            val state = sut.viewState.value!!

            val selectableItems = state.selectableItems
            assertThat(selectableItems.size).isEqualTo(2)
            assertThat(selectableItems[1]!!.shippableItems.size).isEqualTo(1)
            val item = selectableItems[1]!!.shippableItems
                .first() as SelectableShippableItemUI.ExpandableSelectableShippableItemUI
            // Assert that quantity is combined 3f + 3f
            assertThat(item.shippableItem.quantity).isEqualTo(6f)
        }

    @Test
    fun `when moving a shippable item to new shipment, then show singular snackbar`() = testBlocking {
        val updatedCurrentShipmentItems = defaultShipments[0].items.subList(fromIndex = 1, toIndex = 3)
        val updatedShipmentItems = defaultShipments[0].items.subList(fromIndex = 0, toIndex = 1)

        val movement = SplitMovement(
            sourceShipmentKey = 0,
            updatedSourceShipmentItems = updatedCurrentShipmentItems,
            destinationShipmentKey = 1,
            movingShipmentItems = updatedShipmentItems
        )

        createViewModel()

        sut.onUpdateShipment(movement)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val splitMessage = state.splitMessage
        assertThat(splitMessage).isInstanceOf(SplitShipmentMessage.Success::class.java)
        val snackbarData = (splitMessage as SplitShipmentMessage.Success).snackbarData
        assertThat(snackbarData.message).isEqualTo(R.string.woo_shipping_split_shipment_moved_notice_one)
        assertThat(snackbarData.messageParameters).isEqualTo(listOf(1, 2))
    }

    @Test
    fun `when moving shippable items to new shipment, then show plural snackbar`() = testBlocking {
        val updatedCurrentShipmentItems = defaultShipments[0].items.subList(fromIndex = 0, toIndex = 1)
        val updatedShipmentItems = defaultShipments[0].items.subList(fromIndex = 1, toIndex = 2)

        val movement = SplitMovement(
            sourceShipmentKey = 0,
            updatedSourceShipmentItems = updatedCurrentShipmentItems,
            destinationShipmentKey = 1,
            movingShipmentItems = updatedShipmentItems
        )

        createViewModel()

        sut.onUpdateShipment(movement)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val splitMessage = state.splitMessage
        assertThat(splitMessage).isInstanceOf(SplitShipmentMessage.Success::class.java)
        val snackbarData = (splitMessage as SplitShipmentMessage.Success).snackbarData
        assertThat(snackbarData.message).isEqualTo(R.string.woo_shipping_split_shipment_moved_notice_plural)
        assertThat(snackbarData.messageParameters).isEqualTo(listOf(5, 2))
    }

    @Test
    fun `when initialized, then display the correct item quantity`() = testBlocking {
        val shipmentArgs = SplitShipmentArgs(
            orderId = 1L,
            storeOptions = StoreOptionsModel.EMPTY,
            shipments = twoShipments
        )

        val expectedQuantity = 6 // Total single items in the Shipment 1 from `twoShipment`

        createViewModel(shipmentArgs)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val selectableItems = state.selectableItems[0]!!
        assertThat(selectableItems.totalItemQuantity).isEqualTo(expectedQuantity)
    }

    @Test
    fun `when removing a shipment, then the total shipment count is reduced by 1`() = testBlocking {
        val movingItems = twoShipments[1].items

        // Removing "Shipment 2"
        val movement = SplitMovement(
            sourceShipmentKey = 1,
            updatedSourceShipmentItems = emptyList(),
            destinationShipmentKey = 0,
            movingShipmentItems = movingItems
        )

        createViewModel()

        sut.onUpdateShipment(movement)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        assertThat(state.selectableItems.size).isEqualTo(1)
    }

    @Test
    fun `when removing a shipment, then all items are moved to destination shipment`() = testBlocking {
        val shipmentArgs = SplitShipmentArgs(
            orderId = 1L,
            storeOptions = StoreOptionsModel.EMPTY,
            shipments = twoShipments
        )
        val movingItems = twoShipments[1].items

        // Removing "Shipment 2"
        val movement = SplitMovement(
            sourceShipmentKey = 1,
            updatedSourceShipmentItems = emptyList(),
            destinationShipmentKey = 0,
            movingShipmentItems = movingItems
        )
        val expectedItemCount = twoShipments[0].items.size + twoShipments[1].items.size

        createViewModel(shipmentArgs)

        sut.onUpdateShipment(movement)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        val modifiedShipment = state.selectableItems.values.first()
        assertThat(modifiedShipment.shippableItems.size).isEqualTo(expectedItemCount)
    }

    @Test
    fun `when there are 2 shipments, then do not display the Merge all unfulfilled option`() = testBlocking {
        val shipmentArgs = SplitShipmentArgs(
            orderId = 1L,
            storeOptions = StoreOptionsModel.EMPTY,
            shipments = twoShipments
        )

        createViewModel(shipmentArgs)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!

        assertThat(state.overflowMenuItems.size).isEqualTo(2)
    }

    @Test
    fun `when there are 2 shipments and 1 purchased shipment, then do not display the Merge all unfulfilled option`() =
        testBlocking {
            val shipmentArgs = SplitShipmentArgs(
                orderId = 1L,
                storeOptions = StoreOptionsModel.EMPTY,
                shipments = twoShipments + purchasedShipmentUIModel
            )

            createViewModel(shipmentArgs)

            sut.viewState.observeForTesting { }

            val state = sut.viewState.value!!

            assertThat(state.overflowMenuItems.size).isEqualTo(2)
        }

    @Test
    fun `when there are 3 shipments, then display the Merge all unfulfilled option`() = testBlocking {
        val shipmentArgs = SplitShipmentArgs(
            orderId = 1L,
            storeOptions = StoreOptionsModel.EMPTY,
            shipments = threeShipments
        )

        createViewModel(shipmentArgs)

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!
        val lastMenuItem = state.overflowMenuItems.last()
        val expectedShipmentCountToBeMerged = threeShipments.size
        val expectedMenuItemCount = expectedShipmentCountToBeMerged + 1

        assertThat(state.overflowMenuItems.size).isEqualTo(expectedMenuItemCount)

        // Verifying if the last item is "Merge all unfulfilled"
        assertThat(lastMenuItem.size).isEqualTo(expectedShipmentCountToBeMerged)
    }

    @Test
    fun `when merging unfulfilled shipments, a single unfulfilled shipment contains all items`() = testBlocking {
        val shipmentArgs = SplitShipmentArgs(
            orderId = 1L,
            storeOptions = StoreOptionsModel.EMPTY,
            shipments = threeShipments
        )

        createViewModel(shipmentArgs)

        // Trigger merging all shipments
        sut.onRemoveShipments(
            removingShipmentKeys = (0 until threeShipments.size).toList(),
            destinationShipmentKey = null
        )

        sut.viewState.observeForTesting { }

        val state = sut.viewState.value!!
        val currentShipments = state.selectableItems
        val expectedItemQuantity = 10 // All items from `threeShipments`

        // Verify there is only one shipment
        assertThat(currentShipments.size).isEqualTo(1)

        assertThat(currentShipments.values.first().totalItemQuantity).isEqualTo(expectedItemQuantity)
    }

    @Test
    fun `when merging unfulfilled shipments, do not merge purchased shipments`() =
        testBlocking {
            val shipmentArgs = SplitShipmentArgs(
                orderId = 1L,
                storeOptions = StoreOptionsModel.EMPTY,
                shipments = listOf(purchasedShipmentUIModel) + threeShipments
            )

            createViewModel(shipmentArgs)

            // Trigger merging all shipments
            sut.onRemoveShipments(
                removingShipmentKeys = (0 until shipmentArgs.shipments.size).toList(),
                destinationShipmentKey = null
            )

            sut.viewState.observeForTesting { }

            val state = sut.viewState.value!!
            val currentShipments = state.selectableItems
            val expectedItemQuantity = 10 // All items from `threeShipments`

            // Verify there are two shipments
            assertThat(currentShipments.size).isEqualTo(2)

            assertThat(currentShipments.values.toList()[1].totalItemQuantity).isEqualTo(expectedItemQuantity)
        }

    private val defaultShipments = listOf(
        ShipmentUIModel(
            id = "0",
            items = listOf(
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
    )

    private val twoShipments = listOf(
        ShipmentUIModel(
            id = "0",
            items = listOf(
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
            )
        ),
        ShipmentUIModel(
            id = "1",
            items = listOf(
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
    )

    private val threeShipments = listOf(
        ShipmentUIModel(
            id = "0",
            items = listOf(
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
            )
        ),
        ShipmentUIModel(
            id = "1",
            items = listOf(
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
        ),
        ShipmentUIModel(
            id = "2",
            items = listOf(
                ShippableItemModel(
                    itemId = 3L,
                    productId = 3L,
                    title = "Another product with quantity 3",
                    price = BigDecimal(10),
                    quantity = 1f,
                    imageUrl = null,
                    currency = "USD",
                    length = 3f,
                    width = 3f,
                    height = 3f,
                    weight = 8f
                )
            )
        )
    )

    val purchasedShipmentUIModel = ShipmentUIModel(
        id = "10",
        items = listOf(
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
            )
        ),
        purchased = true
    )
}
