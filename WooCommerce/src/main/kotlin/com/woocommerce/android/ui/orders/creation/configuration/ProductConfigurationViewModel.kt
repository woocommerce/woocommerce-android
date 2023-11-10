package com.woocommerce.android.ui.orders.creation.configuration

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.creation.GetProductRules
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
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
    private val resourceProvider: ResourceProvider,
    getChildrenProductInfo: GetChildrenProductInfo
) : ScopedViewModel(savedState) {

    private val navArgs: ProductConfigurationFragmentArgs by savedState.navArgs()

    private val productId = navArgs.productId

    private val rules = MutableStateFlow<ProductRules?>(null)

    private val configuration = MutableStateFlow<ProductConfiguration?>(null)

    private val productsInformation = getChildrenProductInfo(productId).toStateFlow(null)

    val viewState = combine(
        flow = rules.drop(1),
        flow2 = configuration.drop(1),
        flow3 = productsInformation.drop(1)
    ) { rules, configuration, productsInfo ->
        if (rules == null || configuration == null || productsInfo == null) {
            ViewState.Error("Information not found")
        } else {
            val issues = getConfigurationIssues(productsInfo, configuration)
            ViewState.DisplayConfiguration(configuration, productsInfo, issues)
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
                ProductConfiguration(
                    currentConfiguration.rules,
                    currentConfiguration.configurationType,
                    currentConfiguration.configuration,
                    currentConfiguration.childrenConfiguration
                )
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

    private fun getConfigurationIssues(
        productsInfo: Map<Long, ProductInfo>,
        productConfiguration: ProductConfiguration
    ): List<String> {
        val issues = mutableListOf<String>()
        productConfiguration.getConfigurationIssues(resourceProvider).forEach { entry ->
            if (entry.key == ProductConfiguration.PARENT_KEY) {
                issues.add(entry.value)
            } else {
                val productName = productsInfo[entry.key]?.title ?: StringUtils.EMPTY
                issues.add(resourceProvider.getString(R.string.configuration_children_issue, productName, entry.value))
            }
        }
        return issues
    }

    fun onSelectChildrenAttributes(itemId: Long) {
        val productId = productsInformation.value?.get(itemId)?.productId ?: return
        val rules = rules.value ?: return
        val rule = rules.childrenRules?.get(itemId)?.get(VariableProductRule.KEY) as VariableProductRule
        triggerEvent(ProductConfigurationNavigationTarget.NavigateToVariationSelector(itemId, productId, rule))
    }

    sealed class ViewState {
        object Loading : ViewState()

        data class Error(val message: String) : ViewState()
        data class DisplayConfiguration(
            val productConfiguration: ProductConfiguration,
            val productsInfo: Map<Long, ProductInfo>,
            val configurationIssues: List<String>
        ) : ViewState()
    }
}

data class ProductInfo(
    val id: Long,
    val productId: Long,
    val title: String,
    val imageUrl: String?
)

@Parcelize
data class ProductConfigurationResult(
    val productId: Long,
    val productConfiguration: ProductConfiguration
) : Parcelable
