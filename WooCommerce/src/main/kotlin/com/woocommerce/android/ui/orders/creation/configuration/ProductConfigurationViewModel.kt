package com.woocommerce.android.ui.orders.creation.configuration

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.creation.GetProductRules
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ProductConfigurationViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val getProductRules: GetProductRules,
    getChildrenProductInfo: GetChildrenProductInfo
) : ScopedViewModel(savedState) {

    private val navArgs: ProductConfigurationFragmentArgs by savedState.navArgs()

    private val productId = navArgs.productId

    private val rules = MutableStateFlow<ProductRules?>(null)

    private val configuration = MutableStateFlow<ProductConfiguration?>(null)

    private val productsInformation = getChildrenProductInfo(productId)

    val viewState = combine(
        flow = rules.drop(1),
        flow2 = configuration.drop(1),
        flow3 = productsInformation
    ) { rules, configuration, productsInfo ->
        if (rules == null || configuration == null) {
            ViewState.Error("rules not found")
        } else {
            ViewState.DisplayConfiguration(rules, configuration, productsInfo)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ViewState.Loading)

    init {
        launch {
            getProductRules.getRules(navArgs.productId)?.let {
                rules.value = it
                configuration.value = ProductConfiguration.getConfiguration(it)
            }
        }
    }

    fun onUpdateChildrenConfiguration(itemId: Long, ruleKey: String, value: String) {
        configuration.value?.let { currentConfiguration ->
            currentConfiguration.updateChildrenConfiguration(itemId, ruleKey, value)
            configuration.value =
                ProductConfiguration(currentConfiguration.configuration, currentConfiguration.childrenConfiguration)
        }
    }

    fun onCancel() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onSaveConfiguration() {
        configuration.value?.let {
            triggerEvent(MultiLiveEvent.Event.ExitWithResult(ProductConfigurationResult(productId, it)))
        } ?: triggerEvent(MultiLiveEvent.Event.Exit)
    }

    sealed class ViewState {
        object Loading : ViewState()

        data class Error(val message: String) : ViewState()
        data class DisplayConfiguration(
            val productRules: ProductRules,
            val productConfiguration: ProductConfiguration,
            val productsInfo: Map<Long, ProductInfo>
        ) : ViewState()
    }
}

data class ProductInfo(
    val id: Long,
    val title: String,
    val imageUrl: String?
)

@Parcelize
data class ProductConfigurationResult(
    val productId: Long,
    val productConfiguration: ProductConfiguration
) : Parcelable
