package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType

class EditShippingLabelPackagesViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    parameterRepository: ParameterRepository,
    private val orderDetailRepository: OrderDetailRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val variationDetailRepository: VariationDetailRepository,
    private val shippingLabelRepository: ShippingLabelRepository
) : ScopedViewModel(savedState, dispatchers) {
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
        if (viewState.shippingLabelPackages.isNotEmpty()) return
        launch {
            val packagesList = if (arguments.shippingLabelPackages.isEmpty()) {
                createDefaultPackage()
            } else {
                arguments.shippingLabelPackages.toList()
            }
            viewState = ViewState(shippingLabelPackages = packagesList)
        }
    }

    private suspend fun createDefaultPackage(): List<ShippingLabelPackage> {
        viewState = viewState.copy(showSkeletonView = true)

        val shippingPackagesResult = shippingLabelRepository.getShippingPackages()
        if (shippingPackagesResult.isError) {
            triggerEvent(ShowSnackbar(R.string.shipping_label_packages_loading_error))
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
        val totalWeight = items.sumByFloat { it.weight } + (lastUsedPackage?.boxWeight ?: 0f)
        return listOf(
            ShippingLabelPackage(
                packageId = "package1",
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
        val packages = viewState.shippingLabelPackages.toMutableList()
        packages[position] = packages[position].copy(weight = weight)
        viewState = viewState.copy(
            shippingLabelPackages = packages,
            packagesWithEditedWeight = viewState.packagesWithEditedWeight + packages[position].packageId
        )
    }

    fun onPackageSpinnerClicked(position: Int) {
        triggerEvent(OpenPackageSelectorEvent(position))
    }

    fun onPackageSelected(position: Int, selectedPackage: ShippingPackage) {
        val packages = viewState.shippingLabelPackages.toMutableList()
        packages[position] = with(packages[position]) {
            val weight = if (!viewState.packagesWithEditedWeight.contains(packageId)) {
                items.sumByFloat { it.weight } + selectedPackage.boxWeight
            } else {
                weight
            }
            copy(selectedPackage = selectedPackage, weight = weight)
        }
        viewState = viewState.copy(shippingLabelPackages = packages)
    }

    fun onDoneButtonClicked() {
        triggerEvent(ExitWithResult(viewState.shippingLabelPackages))
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
            productId = productId,
            name = name,
            attributesList = attributesList,
            weight = weight
        )
    }

    @Parcelize
    data class ViewState(
        val shippingLabelPackages: List<ShippingLabelPackage> = emptyList(),
        val showSkeletonView: Boolean = false,
        val packagesWithEditedWeight: Set<String> = setOf()
    ) : Parcelable {
        val isDataValid: Boolean
            get() = shippingLabelPackages.isNotEmpty() &&
                shippingLabelPackages.all {
                    !it.weight.isNaN() && it.weight > 0.0 && it.selectedPackage != null
                }
    }

    data class OpenPackageSelectorEvent(val position: Int) : MultiLiveEvent.Event()

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<EditShippingLabelPackagesViewModel>
}
