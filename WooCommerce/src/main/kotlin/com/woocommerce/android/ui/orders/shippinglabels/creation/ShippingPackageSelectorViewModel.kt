package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch

class ShippingPackageSelectorViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    parameterRepository: ParameterRepository,
    private val shippingLabelRepository: ShippingLabelRepository
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val KEY_PARAMETERS = "key_parameters"
    }

    private val arguments: ShippingPackageSelectorFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val dimensionUnit: String by lazy {
        parameterRepository.getParameters(KEY_PARAMETERS, savedState).dimensionUnit ?: ""
    }

    init {
        launch {
            loadPackages()
        }
    }

    private fun loadPackages() {
        launch {
            viewState = viewState.copy(isLoading = true)
            val packagesListResult = shippingLabelRepository.getShippingPackages()
            if (packagesListResult.isError) {
                triggerEvent(ShowSnackbar(R.string.shipping_label_packages_loading_error))
                triggerEvent(Exit)
                return@launch
            }
            viewState = viewState.copy(isLoading = false, packagesList = packagesListResult.model!!)
        }
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

    @Parcelize
    data class ViewState(
        val packagesList: List<ShippingPackage> = emptyList(),
        val isLoading: Boolean = false
    ) : Parcelable

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<ShippingPackageSelectorViewModel>
}

@Parcelize
data class ShippingPackageSelectorResult(
    val position: Int,
    val selectedPackage: ShippingPackage
) : Parcelable
