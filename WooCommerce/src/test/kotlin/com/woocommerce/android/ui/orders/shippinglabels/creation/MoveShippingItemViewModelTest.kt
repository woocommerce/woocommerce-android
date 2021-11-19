package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

class MoveShippingItemViewModelTest : BaseUnitTest() {
    lateinit var viewModel: MoveShippingItemViewModel

    private val defaultItem = ShippingLabelPackage.Item(
        productId = 0L,
        name = "product",
        attributesDescription = "",
        quantity = 3,
        weight = 10f,
        value = BigDecimal.valueOf(10L)
    )

    private val defaultNavArgs by lazy {
        val shippingLabelPackage =
            CreateShippingLabelTestUtils.generateShippingLabelPackage(items = listOf(defaultItem))
        MoveShippingItemDialogArgs(
            item = shippingLabelPackage.items.first(),
            currentPackage = shippingLabelPackage,
            packagesList = listOf(shippingLabelPackage).toTypedArray()
        )
    }

    fun setup(args: MoveShippingItemDialogArgs = defaultNavArgs) {
        viewModel = MoveShippingItemViewModel(
            savedState = args.initSavedStateHandle()
        )
    }

    @Test
    fun `given the item's quantity is more than 1, when the UI loads, then display new package option`() {
        setup()

        assertThat(viewModel.availableDestinations).contains(MoveShippingItemViewModel.DestinationPackage.NewPackage)
    }

    @Test
    fun `given the package has other items, when the UI loads, then display new package option`() {
        val currentItem = defaultItem.copy(quantity = 1)
        val otherItem = defaultItem.copy(productId = 1L)
        val shippingLabelPackage =
            CreateShippingLabelTestUtils.generateShippingLabelPackage(items = listOf(currentItem, otherItem))
        val args = MoveShippingItemDialogArgs(
            item = currentItem,
            currentPackage = shippingLabelPackage,
            packagesList = listOf(shippingLabelPackage).toTypedArray()
        )

        setup(args)

        assertThat(viewModel.availableDestinations).contains(MoveShippingItemViewModel.DestinationPackage.NewPackage)
    }

    @Test
    fun `given the package has only a single item, when the UI loads, then hide new package option`() {
        val currentItem = defaultItem.copy(quantity = 1)
        val shippingLabelPackage =
            CreateShippingLabelTestUtils.generateShippingLabelPackage(items = listOf(currentItem))
        val args = MoveShippingItemDialogArgs(
            item = currentItem,
            currentPackage = shippingLabelPackage,
            packagesList = listOf(shippingLabelPackage).toTypedArray()
        )

        setup(args)

        assertThat(viewModel.availableDestinations)
            .doesNotContain(MoveShippingItemViewModel.DestinationPackage.NewPackage)
    }

    @Test
    fun `given there are other packages, when the UI loads, then display existing package`() {
        val firstPackage = CreateShippingLabelTestUtils.generateShippingLabelPackage(position = 1)
        val secondPackage = CreateShippingLabelTestUtils.generateShippingLabelPackage(position = 2)
        val args = MoveShippingItemDialogArgs(
            item = firstPackage.items.first(),
            currentPackage = firstPackage,
            packagesList = listOf(firstPackage, secondPackage).toTypedArray()
        )

        setup(args)

        assertThat(viewModel.availableDestinations)
            .contains(MoveShippingItemViewModel.DestinationPackage.ExistingPackage(secondPackage))
    }

    @Test
    fun `given there are individual packages, when the UI loads, then exclude them from existing packages list`() {
        val firstPackage = CreateShippingLabelTestUtils.generateShippingLabelPackage(position = 1)
        val secondPackage = CreateShippingLabelTestUtils.generateShippingLabelPackage(
            position = 2,
            selectedPackage = CreateShippingLabelTestUtils.generateIndividualPackage()
        )
        val args = MoveShippingItemDialogArgs(
            item = firstPackage.items.first(),
            currentPackage = firstPackage,
            packagesList = listOf(firstPackage, secondPackage).toTypedArray()
        )

        setup(args)

        assertThat(viewModel.availableDestinations)
            .doesNotContain(MoveShippingItemViewModel.DestinationPackage.ExistingPackage(secondPackage))
    }

    @Test
    fun `given item is in individual package, when the UI loads, then hide the original package option`() {
        val currentItem = defaultItem.copy(quantity = 1)
        val shippingLabelPackage =
            CreateShippingLabelTestUtils.generateShippingLabelPackage(
                items = listOf(currentItem),
                selectedPackage = CreateShippingLabelTestUtils.generateIndividualPackage()
            )

        val args = MoveShippingItemDialogArgs(
            item = currentItem,
            currentPackage = shippingLabelPackage,
            packagesList = listOf(shippingLabelPackage).toTypedArray()
        )

        setup(args)

        assertThat(viewModel.availableDestinations)
            .doesNotContain(MoveShippingItemViewModel.DestinationPackage.OriginalPackage)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `when move button is clicked, then navigate back with the selected destination`() {
        val destination = MoveShippingItemViewModel.DestinationPackage.NewPackage
        setup()

        viewModel.onDestinationPackageSelected(destination)
        viewModel.onMoveButtonClicked()

        assertThat(viewModel.event.value).isInstanceOf(MultiLiveEvent.Event.ExitWithResult::class.java)
        val event =
            viewModel.event.value as MultiLiveEvent.Event.ExitWithResult<MoveShippingItemViewModel.MoveItemResult>
        assertThat(event.data.destination).isEqualTo(destination)
    }

    @Test
    fun `when cancel is clicked, then close the dialog`() {
        setup()

        viewModel.onCancelButtonClicked()

        assertThat(viewModel.event.value).isEqualTo(MultiLiveEvent.Event.Exit)
    }
}
