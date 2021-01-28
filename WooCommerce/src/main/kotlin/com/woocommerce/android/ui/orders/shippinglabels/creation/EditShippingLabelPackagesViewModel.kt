package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize

class EditShippingLabelPackagesViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository,
    private val shippingLabelRepository: ShippingLabelRepository
) : ScopedViewModel(savedState, dispatchers) {
    private val arguments: EditShippingLabelPackagesFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

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
                    items = order.items
                )
            )
        }
        viewState = ViewState(shippingLabelPackages = packagesList)
    }

    @Parcelize
    data class ViewState(
        val shippingLabelPackages: List<ShippingLabelPackage> = emptyList()
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<EditShippingLabelPackagesViewModel>
}
