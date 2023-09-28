package com.woocommerce.android.ui.orders.creation.configuration

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.creation.GetProductRules
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductConfigurationViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val getProductRules: GetProductRules
) : ScopedViewModel(savedState) {

    private val navArgs: ProductConfigurationFragmentArgs by savedState.navArgs()
    val viewState: MutableStateFlow<ViewState> = MutableStateFlow(ViewState.Loading)

    init {
        getProductConfiguration()
    }

    private fun getProductConfiguration() {
        launch {
            val rules = getProductRules.getRules(navArgs.productId) ?: run {
                viewState.value = ViewState.Error("rules not found")
                return@launch
            }
            val configuration = ProductConfiguration.getConfiguration(rules)
            viewState.value = ViewState.DisplayConfiguration(rules, configuration)
        }
    }

    fun onCancel(){
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    sealed class ViewState {
        object Loading : ViewState()

        data class Error(val message: String) : ViewState()
        data class DisplayConfiguration(
            val productRules: ProductRules,
            val productConfiguration: ProductConfiguration,
        ) : ViewState()
    }
}
