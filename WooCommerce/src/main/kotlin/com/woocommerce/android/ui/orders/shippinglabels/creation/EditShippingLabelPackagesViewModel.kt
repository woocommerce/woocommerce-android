package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.MoveItemResult
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductDetailRepository
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
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
@Suppress("TooManyFunctions")
class EditShippingLabelPackagesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    parameterRepository: ParameterRepository,
    private val orderDetailRepository: OrderDetailRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val variationDetailRepository: VariationDetailRepository,
    private val shippingLabelRepository: ShippingLabelRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val KEY_PARAMETERS = "key_parameters"
    }

    private val arguments: EditShippingLabelPackagesFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val weightUnit: String by lazy {
        val parameters = parameterRepository.getParameters(KEY_PARAMETERS, savedState)
        parameters.weightUnit ?: ""
    }

    init {
        initState()
    }

    private fun initState() {
        if (viewState.packagesUiModels.isNotEmpty()) return
        launch {
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
        }
    }

    private suspend fun createDefaultPackage(): List<ShippingLabelPackage> {
        viewState = viewState.copy(showSkeletonView = true)

        val shippingPackagesResult = shippingLabelRepository.getShippingPackages()
        if (shippingPackagesResult.isError) {
            triggerEvent(ShowSnackbar(string.shipping_label_packages_loading_error))
            triggerEvent(Exit)
            return emptyList()
        }

        val lastUsedPackage = shippingLabelRepository.getAccountSettings().let { result ->
            if (result.isError) return@let null
            val savedPackageId = result.model!!.lastUsedBoxId
            shippingPackagesResult.model!!.find { it.id == savedPackageId }
        }

        val order = requireNotNull(orderDetailRepository.getOrder(arguments.orderId))
        loadProductsWeightsIfNeeded(order)

        viewState = viewState.copy(showSkeletonView = false)
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
                return productDetailRepository.fetchProduct(productId) != null ||
                    productDetailRepository.lastFetchProductErrorType == ProductErrorType.INVALID_PRODUCT_ID
            }
            return true
        }

        suspend fun fetchVariationIfNeeded(productId: Long, variationId: Long): Boolean {
            if (!fetchProductIfNeeded(productId)) return false
            if (variationDetailRepository.getVariation(productId, variationId) == null) {
                return variationDetailRepository.fetchVariation(productId, variationId) != null ||
                    variationDetailRepository.lastFetchVariationErrorType == ProductErrorType.INVALID_PRODUCT_ID
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
                triggerEvent(ShowSnackbar(R.string.shipping_label_package_details_fetch_products_error))
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
        triggerEvent(OpenPackageSelectorEvent(position))
    }

    fun onPackageSelected(position: Int, selectedPackage: ShippingPackage) {
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
        triggerEvent(ShowMoveItemDialog(item, shippingPackage, viewState.packagesUiModels.map { it.data }))
    }

    fun handleMoveItemResult(result: MoveItemResult) {
        val packages = viewState.packagesUiModels.toMutableList()
        val item = result.item
        val currentPackage = result.currentPackage

        fun moveItemToNewPackage(): List<ShippingLabelPackageUiModel> {
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
            packages.add(
                ShippingLabelPackageUiModel(
                    data = ShippingLabelPackage(
                        position = packages.size + 1,
                        selectedPackage = currentPackage.selectedPackage,
                        weight = item.weight + (currentPackage.selectedPackage?.boxWeight ?: 0f),
                        items = listOf(item.copy(quantity = 1))
                    )
                )
            )

            return packages.mapIndexed { index, shippingLabelPackageUiModel ->
                // Collapse all items except the added one
                shippingLabelPackageUiModel.copy(isExpanded = index == packages.size - 1)
            }
        }

        viewState = viewState.copy(
            packagesUiModels = when (result.destination) {
                is DestinationPackage.ExistingPackage -> TODO()
                DestinationPackage.NewPackage -> moveItemToNewPackage()
                DestinationPackage.OriginalPackage -> TODO()
            }.filter {
                // Remove empty packages
                it.data.items.isNotEmpty()
            }.mapIndexed { index, model ->
                // Recalculate positions
                model.copy(data = model.data.copy(position = index + 1))
            }
        )
    }

    fun onDoneButtonClicked() {
        triggerEvent(ExitWithResult(viewState.packagesUiModels))
    }

    fun onBackButtonClicked() {
        triggerEvent(Exit)
    }

    private fun Order.getShippableItems(): List<Order.Item> {
        val refunds = orderDetailRepository.getOrderRefunds(identifier.toIdSet().remoteOrderId)
        return refunds.getNonRefundedProducts(items)
            .filter {
                val product = productDetailRepository.getProduct(it.productId)
                // Exclude deleted and virtual products
                product != null && !product.isVirtual
            }
    }

    private fun Order.Item.toShippingItem(): ShippingLabelPackage.Item {
        val weight = if (isVariation) {
            variationDetailRepository.getVariation(productId, variationId)!!.weight
        } else {
            productDetailRepository.getProduct(productId)!!.weight
        }

        return ShippingLabelPackage.Item(
            productId = uniqueId,
            name = name,
            attributesList = attributesList,
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
        val packagesWithEditedWeight: Set<String> = setOf()
    ) : Parcelable {
        val isDataValid: Boolean
            get() = packagesUiModels.isNotEmpty() &&
                packagesUiModels.all {
                    !it.data.weight.isNaN() && it.data.weight > 0.0 && it.data.selectedPackage != null
                }
    }

    @Parcelize
    data class ShippingLabelPackageUiModel(
        val isExpanded: Boolean = false,
        val data: ShippingLabelPackage
    ) : Parcelable

    data class OpenPackageSelectorEvent(val position: Int) : MultiLiveEvent.Event()

    data class ShowMoveItemDialog(
        val item: ShippingLabelPackage.Item,
        val currentPackage: ShippingLabelPackage,
        val packagesList: List<ShippingLabelPackage>
    ) : MultiLiveEvent.Event()
}
