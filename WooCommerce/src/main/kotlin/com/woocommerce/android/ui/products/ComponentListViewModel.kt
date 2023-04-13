package com.woocommerce.android.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Component
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ComponentListViewModel @Inject constructor(savedState: SavedStateHandle) : ScopedViewModel(savedState) {
    private val navArgs: CompositeProductFragmentArgs by savedState.navArgs()

    private val _componentList = MutableLiveData<List<Component>>()
    val componentList: LiveData<List<Component>> = _componentList

    init {
        _componentList.value = navArgs.components.toList()
    }

    fun onComponentSelected(component: Component) {
        // track interaction and navigate to the component details
        triggerEvent(ViewComponentDetails(component))
    }
}

data class ViewComponentDetails(val component: Component) : MultiLiveEvent.Event()
