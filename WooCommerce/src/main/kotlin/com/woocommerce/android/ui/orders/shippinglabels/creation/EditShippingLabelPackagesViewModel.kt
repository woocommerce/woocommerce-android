package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch

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
        loadPackages()
    }

    private fun loadPackages() {
        if (viewState.shippingLabelPackages.isNotEmpty()) return
        launch {
            val availablePackagesResult = shippingLabelRepository.getShippingPackages()
            if (availablePackagesResult.isError) {
                triggerEvent(ShowSnackbar(R.string.shipping_label_packages_loading_error))
                triggerEvent(Exit)
                return@launch
            }
            val availablePackagesList = availablePackagesResult.model!!
            val packagesList = arguments.packages.toList().ifEmpty {
                val order = requireNotNull(orderDetailRepository.getOrder(arguments.orderId))
                listOf(
                    ShippingLabelPackage(
                        selectedPackage = availablePackagesList.first { !it.isCustom },
                        weight = -1,
                        items = order.items
                    )
                )
            }
            viewState = ViewState(
                shippingLabelPackages = packagesList,
                availablePackages = availablePackagesList
            )
        }
    }

    @Parcelize
    data class ViewState(
        val shippingLabelPackages: List<ShippingLabelPackage> = emptyList(),
        val availablePackages: List<ShippingPackage> = emptyList()
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<EditShippingLabelPackagesViewModel>
}
