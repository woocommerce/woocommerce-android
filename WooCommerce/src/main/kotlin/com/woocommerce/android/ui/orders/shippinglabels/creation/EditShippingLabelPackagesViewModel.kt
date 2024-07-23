package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.model.IProduct
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.model.createIndividualShippingPackage
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.MoveItemResult
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class EditShippingLabelPackagesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    parameterRepository: ParameterRepository,
    private val orderDetailRepository: OrderDetailRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val variationDetailRepository: VariationDetailRepository,
    private val shippingLabelRepository: ShippingLabelRepository,
    private val analyticsWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState) {
    companion object {
        private const val KEY_PARAMETERS = "key_parameters"
    }

    private val arguments: EditShippingLabelPackagesFragmentArgs by savedState.navArgs()

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val siteParameters: SiteParameters by lazy { parameterRepository.getParameters(KEY_PARAMETERS, savedState) }

    private var availablePackages: List<ShippingPackage>? = null

    init {
        initState()
    }

    private fun initState() {
        if (viewState.packagesUiModels.isNotEmpty()) return

        launch {
            viewState = viewState.copy(showSkeletonView = true)
            val shippingPackagesResult = shippingLabelRepository.getShippingPackages()
            if (shippingPackagesResult.isError) {
                triggerEvent(ShowSnackbar(string.shipping_label_packages_loading_error))
                triggerEvent(Exit)
            } else {
                availablePackages = shippingPackagesResult.model
            }

            val packagesList = if (arguments.shippingLabelPackages.isEmpty()) {
                createDefaultPackage()
            } else {
                arguments.shippingLabelPackages.toList()
            }
            viewState = ViewState(
                packagesUiModels = packagesList.mapIndexed { index, shippingLabelPackage ->
                    ShippingLabelPackageUiModel(isExpanded = index == 0, data = shippingLabelPackage)
                }
            )
            viewState = viewState.copy(showSkeletonView = false)
        }
    }

    private suspend fun createDefaultPackage(): List<ShippingLabelPackage> {
        val lastUsedPackage = shippingLabelRepository.getLastUsedPackage()
        val order = requireNotNull(orderDetailRepository.getOrderById(arguments.orderId))
        loadProductsWeightsIfNeeded(order)

        val items = order.getShippableItems().map { it.toShippingItem() }
        val totalWeight = items.sumByFloat { it.weight * it.quantity } + (lastUsedPackage?.boxWeight ?: 0f)
        return listOf(
            ShippingLabelPackage(
                position = 1,
                selectedPackage = lastUsedPackage,
                weight = if (totalWeight != 0f) totalWeight else Float.NaN,
                items = items
            )
        )
    }

    private suspend fun loadProductsWeightsIfNeeded(order: Order) {
        suspend fun fetchProductIfNeeded(productId: Long): Boolean {
            if (productDetailRepository.getProduct(productId) == null) {
                return productDetailRepository.fetchProductOrLoadFromCache(productId) != null ||
                    productDetailRepository.lastFetchProductErrorType == ProductErrorType.INVALID_PRODUCT_ID
            }
            return true
        }

        suspend fun fetchVariationIfNeeded(productId: Long, variationId: Long): Boolean {
            if (!fetchProductIfNeeded(productId)) return false
            if (variationDetailRepository.getVariation(productId, variationId) == null) {
                val response = variationDetailRepository.fetchVariation(productId, variationId)
                return !response.isError || response.error.type == ProductErrorType.INVALID_PRODUCT_ID
            }
            return true
        }

        order.items.forEach {
            val result = if (it.isVariation) {
                fetchVariationIfNeeded(it.productId, it.variationId)
            } else {
                fetchProductIfNeeded(it.productId)
            }
            if (!result) {
                // If we fail to fetch a non deleted product, display an error
                triggerEvent(ShowSnackbar(string.shipping_label_package_details_fetch_products_error))
                triggerEvent(Exit)
                return
            }
        }
    }

    fun onWeightEdited(position: Int, weight: Float) {
        val packages = viewState.packagesUiModels.toMutableList()
        packages[position] = packages[position].copy(data = packages[position].data.copy(weight = weight))
        viewState = viewState.copy(
            packagesUiModels = packages,
            packagesWithEditedWeight = viewState.packagesWithEditedWeight + packages[position].data.packageId
        )
    }

    fun onExpandedChanged(position: Int, isExpanded: Boolean) {
        val packages = viewState.packagesUiModels.toMutableList()
        packages[position] = packages[position].copy(isExpanded = isExpanded)
        viewState = viewState.copy(packagesUiModels = packages)
    }

    fun onPackageSpinnerClicked(position: Int) {
        availablePackages?.let {
            if (it.isNotEmpty()) {
                triggerEvent(OpenPackageSelectorEvent(position))
            } else {
                triggerEvent(OpenPackageCreatorEvent(position))
            }
        }
    }

    fun onPackageSelected(position: Int, selectedPackage: ShippingPackage) {
        launch {
            availablePackages = shippingLabelRepository.getShippingPackages().model ?: listOf(selectedPackage)
        }

        val packages = viewState.packagesUiModels.toMutableList()
        val updatedPackage = with(packages[position].data) {
            val weight = if (!viewState.packagesWithEditedWeight.contains(packageId)) {
                items.sumByFloat { it.weight } + selectedPackage.boxWeight
            } else {
                weight
            }
            copy(selectedPackage = selectedPackage, weight = weight)
        }
        packages[position] = packages[position].copy(data = updatedPackage)
        viewState = viewState.copy(packagesUiModels = packages)
    }

    fun onMoveButtonClicked(item: ShippingLabelPackage.Item, shippingPackage: ShippingLabelPackage) {
        analyticsWrapper.track(AnalyticsEvent.SHIPPING_LABEL_MOVE_ITEM_TAPPED)
        triggerEvent(ShowMoveItemDialog(item, shippingPackage, viewState.packages))
    }

    fun onHazmatCategoryClicked(
        currentSelection: ShippingLabelHazmatCategory?,
        packagePosition: Int,
        onHazmatCategorySelected: OnHazmatCategorySelected
    ) {
        analyticsWrapper.track(AnalyticsEvent.HAZMAT_CATEGORY_SELECTOR_OPENED)
        triggerEvent(OpenHazmatCategorySelector(packagePosition, currentSelection, onHazmatCategorySelected))
    }

    fun onHazmatCategorySelected(
        newSelection: ShippingLabelHazmatCategory,
        packagePosition: Int
    ) {
        analyticsWrapper.track(
            AnalyticsEvent.HAZMAT_CATEGORY_SELECTED,
            mapOf(
                AnalyticsTracker.KEY_CATEGORY to newSelection.toString(),
                AnalyticsTracker.KEY_ORDER_ID to arguments.orderId
            )
        )
        val packages = viewState.packagesUiModels.toMutableList()
        with(packages[packagePosition].data) {
            selectedPackage?.copy(hazmatCategory = newSelection)
                ?.let { copy(selectedPackage = it) }
        }?.let { packages[packagePosition] = packages[packagePosition].copy(data = it) }
        viewState = viewState.copy(packagesUiModels = packages)
    }

    fun onURLClicked(url: String) {
        triggerEvent(OpenURL(url))
    }

    fun onContainsHazmatChanged(isActive: Boolean) {
        if (isActive) {
            analyticsWrapper.track(AnalyticsEvent.CONTAINS_HAZMAT_CHECKED)
        }
    }

    // all the logic is inside local functions, so it should be OK, but detekt complains still
    @Suppress("ComplexMethod")
    fun handleMoveItemResult(result: MoveItemResult) {
        val packages = viewState.packagesUiModels.toMutableList()
        val item = result.item
        val currentPackage = result.currentPackage

        fun removeItemFromCurrentPackage(): MutableList<ShippingLabelPackageUiModel> {
            val updatedItems = if (item.quantity > 1) {
                // if the item quantity is more than one, subtract 1 from it
                val mutableItems = currentPackage.items.toMutableList()
                val updatedItem = item.copy(quantity = item.quantity - 1)
                mutableItems[mutableItems.indexOf(item)] = updatedItem
                mutableItems
            } else {
                // otherwise remove it completely
                currentPackage.items - item
            }

            val indexOfCurrentPackage = packages.indexOfFirst { it.data == currentPackage }
            packages[indexOfCurrentPackage] = ShippingLabelPackageUiModel(
                data = currentPackage.copy(items = updatedItems)
            )
            return packages
        }

        suspend fun moveItemToNewPackage(): List<ShippingLabelPackageUiModel> {
            val updatedPackages = removeItemFromCurrentPackage()
            val selectedPackage = currentPackage.selectedPackage?.takeIf { !it.isIndividual }
                ?: shippingLabelRepository.getLastUsedPackage()
            updatedPackages.add(
                ShippingLabelPackageUiModel(
                    data = ShippingLabelPackage(
                        position = packages.size + 1,
                        selectedPackage = selectedPackage,
                        weight = item.weight + (selectedPackage?.boxWeight ?: 0f),
                        items = listOf(item.copy(quantity = 1))
                    )
                )
            )

            return updatedPackages.mapIndexed { index, shippingLabelPackageUiModel ->
                // Collapse all items except the added one
                shippingLabelPackageUiModel.copy(isExpanded = index == packages.size - 1)
            }
        }

        fun moveItemToExistingPackage(destination: ShippingLabelPackage): List<ShippingLabelPackageUiModel> {
            val updatedPackages = removeItemFromCurrentPackage()
            val updatedItemsOfDestination = destination.items.toMutableList().apply {
                val existingItem = firstOrNull { it.isSameProduct(item) }
                if (existingItem != null) {
                    // If the existing package has same product, then just increase the quantity
                    set(indexOf(existingItem), existingItem.copy(quantity = existingItem.quantity + 1))
                } else {
                    // Otherwise add a new item with quantity set to 1
                    add(item.copy(quantity = 1))
                }
            }

            val indexOfDestinationPackage = updatedPackages.indexOfFirst { it.data == destination }
            updatedPackages[indexOfDestinationPackage] = ShippingLabelPackageUiModel(
                data = destination.copy(items = updatedItemsOfDestination)
            )

            return packages.mapIndexed { index, shippingLabelPackageUiModel ->
                // Collapse all items except the destination one
                shippingLabelPackageUiModel.copy(isExpanded = index == indexOfDestinationPackage)
            }
        }

        suspend fun moveItemToIndividualPackage(): List<ShippingLabelPackageUiModel> {
            val updatedPackages = removeItemFromCurrentPackage()

            // We fetch products when this screen is opened, so we can retrieve details from DB
            val product: IProduct? = orderDetailRepository.getOrderById(arguments.orderId)
                ?.items
                ?.find { it.uniqueId == item.productId }
                ?.let {
                    if (it.isVariation) {
                        variationDetailRepository.getVariation(it.productId, it.variationId)
                    } else {
                        productDetailRepository.getProduct(it.productId)
                    }
                }

            val individualPackage = item.createIndividualShippingPackage(product)
            updatedPackages.add(
                ShippingLabelPackageUiModel(
                    data = ShippingLabelPackage(
                        position = packages.size + 1,
                        selectedPackage = individualPackage,
                        weight = item.weight,
                        items = listOf(item.copy(quantity = 1))
                    )
                )
            )
            return updatedPackages.mapIndexed { index, shippingLabelPackageUiModel ->
                // Collapse all items except the added one
                shippingLabelPackageUiModel.copy(isExpanded = index == packages.size - 1)
            }
        }

        launch {
            viewState = viewState.copy(
                packagesUiModels = when (result.destination) {
                    is DestinationPackage.ExistingPackage ->
                        moveItemToExistingPackage(result.destination.destinationPackage)
                    DestinationPackage.NewPackage -> moveItemToNewPackage()
                    DestinationPackage.OriginalPackage -> moveItemToIndividualPackage()
                }.filter {
                    // Remove empty packages
                    it.data.items.isNotEmpty()
                }.mapIndexed { index, model ->
                    // Recalculate positions
                    model.copy(data = model.data.copy(position = index + 1))
                }
            )
        }
    }

    fun onDoneButtonClicked() {
        triggerEvent(ExitWithResult(viewState.packages))
    }

    fun onBackButtonClicked() {
        triggerEvent(Exit)
    }

    private fun Order.getShippableItems(): List<Order.Item> {
        val refunds = orderDetailRepository.getOrderRefunds(id)
        return refunds.getNonRefundedProducts(items)
            .filter {
                val product = productDetailRepository.getProduct(it.productId)
                // Exclude deleted and virtual products
                product != null && !product.isVirtual
            }
    }

    private suspend fun Order.Item.toShippingItem(): ShippingLabelPackage.Item {
        val weight = if (isVariation) {
            variationDetailRepository.getVariation(productId, variationId)!!.weight
        } else {
            productDetailRepository.getProduct(productId)!!.weight
        }

        return ShippingLabelPackage.Item(
            productId = uniqueId,
            name = name,
            attributesDescription = attributesDescription,
            // for shipping purposes, consider portion quantities as complete values
            quantity = ceil(quantity).toInt(),
            value = price,
            weight = weight
        )
    }

    @Parcelize
    data class ViewState(
        val packagesUiModels: List<ShippingLabelPackageUiModel> = emptyList(),
        val showSkeletonView: Boolean = false,
        val packagesWithEditedWeight: Set<String> = setOf(),
        val hazmatContentIsChecked: Boolean = false
    ) : Parcelable {
        @IgnoredOnParcel
        val packages: List<ShippingLabelPackage>
            get() = packagesUiModels.map { it.data }
        val isDataValid: Boolean
            get() = packagesUiModels.isNotEmpty() &&
                packagesUiModels.all { it.isValid }
    }

    @Parcelize
    data class ShippingLabelPackageUiModel(
        val isExpanded: Boolean = false,
        val data: ShippingLabelPackage
    ) : Parcelable {
        @IgnoredOnParcel
        val isValid: Boolean
            get() = !data.weight.isNaN() &&
                data.weight > 0.0 &&
                data.selectedPackage != null &&
                data.selectedPackage.dimensions.isValid
    }

    data class OpenPackageSelectorEvent(val position: Int) : MultiLiveEvent.Event()
    data class OpenPackageCreatorEvent(val position: Int) : MultiLiveEvent.Event()

    data class ShowMoveItemDialog(
        val item: ShippingLabelPackage.Item,
        val currentPackage: ShippingLabelPackage,
        val packagesList: List<ShippingLabelPackage>
    ) : MultiLiveEvent.Event()

    data class OpenHazmatCategorySelector(
        val packagePosition: Int,
        val currentSelection: ShippingLabelHazmatCategory?,
        val onHazmatCategorySelected: OnHazmatCategorySelected
    ) : MultiLiveEvent.Event()

    data class OpenURL(val url: String) : MultiLiveEvent.Event()
}
