package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ShippingLabelCreateServicePackageViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val shippingLabelRepository: ShippingLabelRepository
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        launch {
            getSelectableServicePackages()
        }
    }

    private fun getSelectableServicePackages() {
        launch {
            viewState = viewState.copy(isLoading = true)
            val result = shippingLabelRepository.getSelectableServicePackages()
            if (result.isError) {
                triggerEvent(ShowSnackbar(R.string.shipping_label_packages_loading_error))
                return@launch
            }

            val uiModels = result.model!!.map { ServicePackageUiModel(it) }
            viewState = viewState.copy(isLoading = false, uiModels = uiModels)
        }
    }

    fun onPackageSelected(id: String) {
        val newList = viewState.uiModels.map {
            ServicePackageUiModel(
                data = it.data,
                isChecked = id == it.data.id
            )
        }

        viewState = viewState.copy(uiModels = newList)
    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = false,
        val uiModels: List<ServicePackageUiModel> = emptyList()
    ) : Parcelable

    @Parcelize
    data class ServicePackageUiModel(
        val data: ShippingPackage,
        val isChecked: Boolean = false
    ) : Parcelable
}
