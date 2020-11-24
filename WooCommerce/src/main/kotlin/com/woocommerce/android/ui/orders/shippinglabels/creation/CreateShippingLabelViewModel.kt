package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import android.util.Log
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.Event.DataLoaded
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.Event.OriginAddressInvalid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.Event.OriginAddressValidated
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.Event.ShippingAddressValidated
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.Event.SuggestedOriginAddressSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.SideEffect.LoadData
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.SideEffect.NoOp
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.SideEffect.OpenOriginAddressEditor
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.SideEffect.OpenShippingAddressEditor
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.SideEffect.ShowDataLoadingError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.SideEffect.ShowOriginAddressSuggestions
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.SideEffect.ShowPackagingDetails
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.SideEffect.ShowShippingAddressSuggestions
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.SideEffect.ValidateOriginAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.SideEffect.ValidateShippingAddress
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class CreateShippingLabelViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val repository: ShippingLabelRepository,
    private val networkStatus: NetworkStatus,
    private val creationFlow: ShippingLabelCreationFlow
) : ScopedViewModel(savedState, dispatchers) {
//    private val arguments: CreateShippingLabelFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        subscribeToCreationFlow()
    }

    private fun subscribeToCreationFlow() = launch {
        creationFlow.effects.collect { effect ->
            Log.d("FLOW", effect.toString())
            when (effect) {
                LoadData -> {
                    creationFlow.handleEvent(DataLoaded)
                }
                ShowDataLoadingError -> {}
                ValidateOriginAddress -> {
                    creationFlow.handleEvent(OriginAddressValidated)
                }
                ShowOriginAddressSuggestions -> {
                }
                OpenOriginAddressEditor -> { }
                ValidateShippingAddress -> {
                    creationFlow.handleEvent(ShippingAddressValidated)
                }
                ShowShippingAddressSuggestions -> {}
                OpenShippingAddressEditor -> {}
                ShowPackagingDetails -> {}
                NoOp -> {}
            }
        }
        creationFlow.start()
    }

    @Parcelize
    data class ViewState(
        val automaton: String? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<CreateShippingLabelViewModel>
}
