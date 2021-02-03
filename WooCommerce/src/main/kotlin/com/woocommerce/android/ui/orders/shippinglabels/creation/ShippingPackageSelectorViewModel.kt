package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize

class ShippingPackageSelectorViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    parameterRepository: ParameterRepository
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val KEY_PARAMETERS = "key_parameters"
    }

    private val arguments: ShippingPackageSelectorFragmentArgs by savedState.navArgs()

    val availablePackages: List<ShippingPackage>
        get() = arguments.availablePackages.toList()

    val dimensionUnit: String by lazy {
        parameterRepository.getParameters(KEY_PARAMETERS, savedState).dimensionUnit ?: ""
    }

    fun onPackageSelected(shippingPackage: ShippingPackage) {
        triggerEvent(
            ExitWithResult(
                ShippingPackageSelectorResult(
                    position = arguments.position,
                    selectedPackage = shippingPackage
                )
            )
        )
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ShippingPackageSelectorViewModel>
}

@Parcelize
data class ShippingPackageSelectorResult(
    val position: Int,
    val selectedPackage: ShippingPackage
) : Parcelable
