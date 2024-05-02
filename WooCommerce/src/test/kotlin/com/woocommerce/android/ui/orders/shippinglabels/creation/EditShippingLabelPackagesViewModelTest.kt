package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CATEGORY
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ORDER_ID
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesViewModel.OpenHazmatCategorySelector
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesViewModel.ViewState
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory.AIR_ELIGIBLE_ETHANOL
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class EditShippingLabelPackagesViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
    }

    private val availablePackages = listOf(
        CreateShippingLabelTestUtils.generatePackage("id1", "provider1"),
        CreateShippingLabelTestUtils.generatePackage("id2", "provider2")
    )

    private val testOrder = OrderTestUtils.generateTestOrder(ORDER_ID)
    private val testProduct = ProductTestUtils.generateProduct()

    private val orderDetailRepository: OrderDetailRepository = mock()
    private val productDetailRepository: ProductDetailRepository = mock()
    private val variationDetailRepository: VariationDetailRepository = mock()
    private val shippingLabelRepository: ShippingLabelRepository = mock()
    private val parameterRepository: ParameterRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val defaultItem = ShippingLabelPackage.Item(
        productId = 15,
        name = "test",
        quantity = 1,
        attributesDescription = "",
        weight = 1f,
        value = BigDecimal.TEN
    )

    private lateinit var viewModel: EditShippingLabelPackagesViewModel

    suspend fun setup(currentPackages: Array<ShippingLabelPackage>) {
        val savedState = EditShippingLabelPackagesFragmentArgs(
            orderId = ORDER_ID,
            shippingLabelPackages = currentPackages
        ).toSavedStateHandle()
        whenever(shippingLabelRepository.getShippingPackages()).thenReturn(WooResult(availablePackages))
        whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(testOrder)
        whenever(productDetailRepository.getProduct(any())).thenReturn(testProduct)
        viewModel = EditShippingLabelPackagesViewModel(
            savedState,
            productDetailRepository = productDetailRepository,
            orderDetailRepository = orderDetailRepository,
            variationDetailRepository = variationDetailRepository,
            shippingLabelRepository = shippingLabelRepository,
            parameterRepository = parameterRepository,
            analyticsWrapper = analyticsTrackerWrapper
        )
    }

    @Test
    fun `test first opening of the screen`() = testBlocking {
        whenever(shippingLabelRepository.getLastUsedPackage()).thenReturn(availablePackages.first())

        setup(emptyArray())
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        verify(orderDetailRepository).getOrderById(any())
        verify(shippingLabelRepository).getLastUsedPackage()
        assertThat(viewState!!.packagesUiModels.size).isEqualTo(1)
        assertThat(viewState!!.packages.first().selectedPackage).isEqualTo(availablePackages.first())
    }

    @Test
    fun `test edit flow`() = testBlocking {
        val currentShippingPackages = arrayOf(
            CreateShippingLabelTestUtils.generateShippingLabelPackage(
                selectedPackage = availablePackages[0]
            )
        )
        setup(currentShippingPackages)
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        verify(orderDetailRepository, never()).getOrderById(any())
        verify(shippingLabelRepository, never()).getAccountSettings()
        assertThat(viewState!!.packages).isEqualTo(currentShippingPackages.toList())
    }

    @Test
    fun `no last used package`() = testBlocking {
        whenever(shippingLabelRepository.getLastUsedPackage()).thenReturn(null)

        setup(emptyArray())
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState!!.packages.first().selectedPackage).isNull()
    }

    @Test
    fun `edit weight of package`() = testBlocking {
        whenever(shippingLabelRepository.getLastUsedPackage()).thenReturn(availablePackages.first())

        setup(emptyArray())
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        viewModel.onWeightEdited(0, 10.0f)
        assertThat(viewState!!.packagesUiModels.first().data.weight).isEqualTo(10.0f)
        assertThat(viewState!!.isDataValid).isTrue()
    }

    @Test
    fun `select a package`() = testBlocking {
        whenever(shippingLabelRepository.getLastUsedPackage()).thenReturn(availablePackages.first())

        setup(emptyArray())
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        viewModel.onPackageSpinnerClicked(0)
        viewModel.onPackageSelected(0, availablePackages[1])
        assertThat(viewState!!.packages.first().selectedPackage).isEqualTo(availablePackages[1])
    }

    @Test
    fun `exit without saving changes`() = testBlocking {
        whenever(shippingLabelRepository.getLastUsedPackage()).thenReturn(availablePackages.first())

        setup(emptyArray())
        var event: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { event = it }
        viewModel.onBackButtonClicked()

        assertThat(event).isEqualTo(Exit)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `save changes and exit`() = testBlocking {
        whenever(shippingLabelRepository.getLastUsedPackage()).thenReturn(availablePackages.first())

        setup(emptyArray())
        var event: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onWeightEdited(0, 10.0f)
        viewModel.onDoneButtonClicked()

        assertThat(event).isInstanceOf(ExitWithResult::class.java)
        val createdShippingPackages = (event as ExitWithResult<List<ShippingLabelPackage>>).data
        assertThat(createdShippingPackages.size).isEqualTo(1)
        assertThat(createdShippingPackages.first().weight).isEqualTo(10.0f)
    }

    @Test
    fun `given item's quantity bigger than 1, when the item is moved, then decrease the quantity`() = testBlocking {
        val item = defaultItem.copy(quantity = 2)
        val currentShippingPackages = arrayOf(
            CreateShippingLabelTestUtils.generateShippingLabelPackage(
                items = listOf(item)
            )
        )
        setup(currentShippingPackages)

        viewModel.onMoveButtonClicked(item, currentShippingPackages.first())
        viewModel.handleMoveItemResult(
            MoveShippingItemViewModel.MoveItemResult(
                item,
                currentShippingPackages.first(),
                MoveShippingItemViewModel.DestinationPackage.NewPackage
            )
        )

        val newPackages = viewModel.viewStateData.liveData.value!!.packages
        assertThat(newPackages.size).isEqualTo(2)
        assertThat(newPackages[0].items.first().quantity).isEqualTo(1)
        assertThat(newPackages[1].items).isEqualTo(listOf(item.copy(quantity = 1)))
    }

    @Test
    fun `given item's quantity equals 1, when the item is moved, then remove it from the package`() = testBlocking {
        val currentShippingPackages = arrayOf(
            CreateShippingLabelTestUtils.generateShippingLabelPackage(
                items = listOf(defaultItem, defaultItem.copy(productId = 16))
            )
        )
        setup(currentShippingPackages)

        viewModel.onMoveButtonClicked(defaultItem, currentShippingPackages.first())
        viewModel.handleMoveItemResult(
            MoveShippingItemViewModel.MoveItemResult(
                defaultItem,
                currentShippingPackages.first(),
                MoveShippingItemViewModel.DestinationPackage.NewPackage
            )
        )

        val newPackages = viewModel.viewStateData.liveData.value!!.packages
        assertThat(newPackages.size).isEqualTo(2)
        assertThat(newPackages[0].items).doesNotContain(defaultItem)
        assertThat(newPackages[1].items).isEqualTo(listOf(defaultItem))
    }

    @Test
    fun `given package has same product, when item is moved to this package, then increase quantity`() = testBlocking {
        val currentShippingPackages = arrayOf(
            CreateShippingLabelTestUtils.generateShippingLabelPackage(
                position = 1,
                items = listOf(defaultItem)
            ),
            CreateShippingLabelTestUtils.generateShippingLabelPackage(
                position = 2,
                items = listOf(defaultItem)
            )
        )
        setup(currentShippingPackages)

        viewModel.onMoveButtonClicked(defaultItem, currentShippingPackages[1])
        viewModel.handleMoveItemResult(
            MoveShippingItemViewModel.MoveItemResult(
                defaultItem,
                currentShippingPackages[1],
                MoveShippingItemViewModel.DestinationPackage.ExistingPackage(currentShippingPackages[0])
            )
        )

        val newPackages = viewModel.viewStateData.liveData.value!!.packages
        assertThat(newPackages.size).isEqualTo(1)
        assertThat(newPackages[0].items.first().quantity).isEqualTo(2)
    }

    @Test
    fun `given package hasn't same product, when item is moved to this package, then add new item`() = testBlocking {
        val currentShippingPackages = arrayOf(
            CreateShippingLabelTestUtils.generateShippingLabelPackage(
                position = 1,
                items = listOf(defaultItem.copy(productId = 16))
            ),
            CreateShippingLabelTestUtils.generateShippingLabelPackage(
                position = 2,
                items = listOf(defaultItem)
            )
        )
        setup(currentShippingPackages)

        viewModel.onMoveButtonClicked(defaultItem, currentShippingPackages[1])
        viewModel.handleMoveItemResult(
            MoveShippingItemViewModel.MoveItemResult(
                defaultItem,
                currentShippingPackages[1],
                MoveShippingItemViewModel.DestinationPackage.ExistingPackage(currentShippingPackages[0])
            )
        )

        val newPackages = viewModel.viewStateData.liveData.value!!.packages
        assertThat(newPackages.size).isEqualTo(1)
        assertThat(newPackages[0].items.size).isEqualTo(2)
        assertThat(newPackages[0].items).contains(defaultItem)
    }

    @Test
    fun `when item is moved to original packaging, then add correct package to the list`() = testBlocking {
        val currentShippingPackages = arrayOf(
            CreateShippingLabelTestUtils.generateShippingLabelPackage(
                position = 1,
                items = listOf(defaultItem)
            )
        )
        setup(currentShippingPackages)

        viewModel.onMoveButtonClicked(defaultItem, currentShippingPackages[0])
        viewModel.handleMoveItemResult(
            MoveShippingItemViewModel.MoveItemResult(
                defaultItem,
                currentShippingPackages[0],
                MoveShippingItemViewModel.DestinationPackage.OriginalPackage
            )
        )

        val newPackages = viewModel.viewStateData.liveData.value!!.packages
        assertThat(newPackages.size).isEqualTo(1)
        assertThat(newPackages[0].selectedPackage!!.isIndividual).isTrue
        with(newPackages[0].selectedPackage!!.dimensions) {
            assertThat(width).isEqualTo(testProduct.width)
            assertThat(length).isEqualTo(testProduct.length)
            assertThat(height).isEqualTo(testProduct.height)
        }
    }

    @Test
    fun `when select hazmat category is clicked, then trigger hazmat dialog event`() = testBlocking {
        setup(emptyArray())
        var event: MultiLiveEvent.Event? = null
        val onHazmatCategorySelected: OnHazmatCategorySelected = { _ -> }
        viewModel.event.observeForever { event = it }

        viewModel.onHazmatCategoryClicked(
            currentSelection = AIR_ELIGIBLE_ETHANOL,
            packagePosition = 0,
            onHazmatCategorySelected = onHazmatCategorySelected
        )

        assertThat(event).isEqualTo(
            OpenHazmatCategorySelector(
                packagePosition = 0,
                currentSelection = AIR_ELIGIBLE_ETHANOL,
                onHazmatCategorySelected = onHazmatCategorySelected
            )
        )
    }

    @Test
    fun `when onHazmatCategorySelected, then update the packages info`() = testBlocking {
        val currentShippingPackages = arrayOf(
            CreateShippingLabelTestUtils.generateShippingLabelPackage(
                position = 1,
                items = listOf(defaultItem)
            )
        )
        setup(currentShippingPackages)

        viewModel.onHazmatCategorySelected(
            packagePosition = 0,
            newSelection = AIR_ELIGIBLE_ETHANOL
        )

        val newPackages = viewModel.viewStateData.liveData.value!!.packages
        assertThat(newPackages.size).isEqualTo(1)
        assertThat(newPackages[0].selectedPackage?.hazmatCategory).isEqualTo(AIR_ELIGIBLE_ETHANOL)
    }

    @Test
    fun `when onContainsHazmatChanged is true, then trigger expected track event`() = testBlocking {
        // Given
        setup(emptyArray())

        // When
        viewModel.onContainsHazmatChanged(true)

        // Then
        verify(analyticsTrackerWrapper).track(
            stat = AnalyticsEvent.CONTAINS_HAZMAT_CHECKED
        )
    }

    @Test
    fun `when onHazmatCategoryClicked, then trigger expected track event`() = testBlocking {
        // Given
        setup(emptyArray())

        // When
        viewModel.onHazmatCategoryClicked(
            currentSelection = AIR_ELIGIBLE_ETHANOL,
            packagePosition = 0,
            onHazmatCategorySelected = {}
        )

        // Then
        verify(analyticsTrackerWrapper).track(
            stat = AnalyticsEvent.HAZMAT_CATEGORY_SELECTOR_OPENED
        )
    }

    @Test
    fun `when onHazmatCategorySelected, then trigger expected track event`() = testBlocking {
        // Given
        setup(emptyArray())

        // When
        viewModel.onHazmatCategorySelected(
            packagePosition = 0,
            newSelection = AIR_ELIGIBLE_ETHANOL
        )

        // Then
        verify(analyticsTrackerWrapper).track(
            stat = AnalyticsEvent.HAZMAT_CATEGORY_SELECTED,
            properties = mapOf(
                KEY_CATEGORY to AIR_ELIGIBLE_ETHANOL.toString(),
                KEY_ORDER_ID to ORDER_ID
            )
        )
    }
}
