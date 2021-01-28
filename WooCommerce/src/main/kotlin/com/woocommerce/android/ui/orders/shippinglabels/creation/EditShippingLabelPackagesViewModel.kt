package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize

class EditShippingLabelPackagesViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    parameterRepository: ParameterRepository,
    private val orderDetailRepository: OrderDetailRepository,
    private val productDetailRepository: ProductDetailRepository
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val KEY_PARAMETERS = "ket_parameters"
    }

    private val arguments: EditShippingLabelPackagesFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val parameters: SiteParameters by lazy {
        parameterRepository.getParameters(KEY_PARAMETERS, savedState)
    }

    init {
        initState()
    }

    private fun initState() {
        if (viewState.shippingLabelPackages.isNotEmpty()) return
        val packagesList = arguments.shippingLabelPackages.toList().ifEmpty {
            val order = requireNotNull(orderDetailRepository.getOrder(arguments.orderId))
            listOf(
                ShippingLabelPackage(
                    selectedPackage = arguments.availablePackages.first(),
                    weight = -1,
                    items = order.items.map { it.toShippingItem() }
                )
            )
        }
        viewState = ViewState(shippingLabelPackages = packagesList)
    }

    private fun Order.Item.toShippingItem(): ShippingLabelPackage.Item {
        val weight = productDetailRepository.getProduct(productId)?.weight ?: 0f
        return ShippingLabelPackage.Item(
            productId = productId,
            name = name,
            attributesList = attributesList,
            weight = "$weight ${parameters.weightUnit}"
        )
    }

    @Parcelize
    data class ViewState(
        val shippingLabelPackages: List<ShippingLabelPackage> = emptyList()
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<EditShippingLabelPackagesViewModel>
}
