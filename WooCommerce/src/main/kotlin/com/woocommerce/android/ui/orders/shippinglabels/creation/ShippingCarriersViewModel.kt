package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ShippingRate
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch

class ShippingCarriersViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val shippingLabelRepository: ShippingLabelRepository
) : ScopedViewModel(savedState, dispatchers) {
    private val arguments: ShippingCarriersFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val _shippingRates = MutableLiveData<List<List<ShippingRate>>>()
    val shippingRates: LiveData<List<List<ShippingRate>>> = _shippingRates

    init {
        loadShippingRates()
    }

    private fun loadShippingRates() {
        launch {
            viewState = viewState.copy(isLoadingProgressDialogVisible = true)

            _shippingRates.value = shippingLabelRepository.getShippingRates(
                arguments.orderId,
                arguments.originAddress,
                arguments.destinationAddress,
                arguments.packages.toList()
            )

            viewState = viewState.copy(isLoadingProgressDialogVisible = false)
        }
    }

    fun onDoneButtonClicked() {
    }

    fun onExit() {
        triggerEvent(Exit)
    }

    @Parcelize
    data class ViewState(
        val bannerMessage: String? = null,
        val isLoadingProgressDialogVisible: Boolean? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ShippingCarriersViewModel>
}
