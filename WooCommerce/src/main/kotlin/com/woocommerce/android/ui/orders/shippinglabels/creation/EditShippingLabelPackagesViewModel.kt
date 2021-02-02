package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.order.toIdSet

class EditShippingLabelPackagesViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    parameterRepository: ParameterRepository,
    private val orderDetailRepository: OrderDetailRepository,
    private val productDetailRepository: ProductDetailRepository
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val KEY_PARAMETERS = "key_parameters"
    }

    private val arguments: EditShippingLabelPackagesFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val parameters: SiteParameters by lazy {
        parameterRepository.getParameters(KEY_PARAMETERS, savedState)
    }

    val availablePackages
        get() = arguments.availablePackages

    init {
        initState()
    }

    private fun initState() {
        if (viewState.shippingLabelPackages.isNotEmpty()) return
        launch {
            loadProductsWeightsIfNeeded()
            val packagesList = arguments.shippingLabelPackages.toList().ifEmpty {
                val order = requireNotNull(orderDetailRepository.getOrder(arguments.orderId))
                listOf(
                    ShippingLabelPackage(
                        selectedPackage = arguments.availablePackages.first(),
                        weight = Double.NaN,
                        items = order.getShippableItems().map { it.toShippingItem() }
                    )
                )
            }
            viewState = ViewState(shippingLabelPackages = packagesList)
        }
    }

    private suspend fun loadProductsWeightsIfNeeded() {
        val order = requireNotNull(orderDetailRepository.getOrder(arguments.orderId))
        if (order.items.any { productDetailRepository.getProduct(it.productId) == null }) {
            viewState = viewState.copy(showSkeletonView = true)
            order.items.forEach {
                if (productDetailRepository.getProduct(it.productId) == null) {
                    // Ignore any errors, we will hide the weight if we can't fetch the product
                    productDetailRepository.fetchProduct(it.productId)
                }
            }
            viewState = viewState.copy(showSkeletonView = false)
        }
    }

    fun onWeightEdited(position: Int, weight: Double) {
        val packages = viewState.shippingLabelPackages.toMutableList()
        packages[position] = packages[position].copy(weight = weight)
        viewState = viewState.copy(shippingLabelPackages = packages)
    }

    fun onPackageSpinnerClicked(position: Int) {
        triggerEvent(OpenPackageSelectorEvent(position))
    }

    fun onPackageSelected(position: Int, selectedPackage: ShippingPackage) {
        val packages = viewState.shippingLabelPackages.toMutableList()
        packages[position] = packages[position].copy(selectedPackage = selectedPackage)
        viewState = viewState.copy(shippingLabelPackages = packages)
    }

    private fun Order.getShippableItems(): List<Order.Item> {
        val refunds = orderDetailRepository.getOrderRefunds(identifier.toIdSet().remoteOrderId)
        return refunds.getNonRefundedProducts(items)
            .filter { !(productDetailRepository.getProduct(it.productId)?.isVirtual ?: false) }
    }

    private fun Order.Item.toShippingItem(): ShippingLabelPackage.Item {
        val weight = productDetailRepository.getProduct(productId)?.weight?.let {
            "$it ${parameters.weightUnit}"
        } ?: ""
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
        val showSkeletonView: Boolean = false
    ) : Parcelable

    data class OpenPackageSelectorEvent(val position: Int) : MultiLiveEvent.Event()

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<EditShippingLabelPackagesViewModel>
}
