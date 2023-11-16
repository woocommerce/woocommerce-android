package com.woocommerce.android.ui.orders.creation.configuration

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CHANGED_FIELD
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CHANGED_FIELD_OPTIONAL
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CHANGED_FIELD_QUANTITY
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CHANGED_FIELD_VARIATION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_OTHER
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
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
    private val getProductConfiguration: GetProductConfiguration,
    private val tracker: AnalyticsTrackerWrapper,
    getChildrenProductInfo: GetChildrenProductInfo
) : ScopedViewModel(savedState) {

    private val navArgs: ProductConfigurationFragmentArgs by savedState.navArgs()

    private val flow = navArgs.flow

    private val productId = flow.productId

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
            getProductRules.getRules(navArgs.flow.productId)?.let {
                rules.value = it
                configuration.value = when (flow) {
                    is Flow.Selection -> getProductConfiguration(it)
                    is Flow.Edit -> flow.configuration
                }
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

            val ruleChanged = when (ruleKey) {
                OptionalRule.KEY -> VALUE_CHANGED_FIELD_OPTIONAL
                VariableProductRule.KEY -> VALUE_CHANGED_FIELD_VARIATION
                QuantityRule.KEY -> VALUE_CHANGED_FIELD_QUANTITY
                else -> VALUE_OTHER
            }

            tracker.track(
                AnalyticsEvent.ORDER_FORM_BUNDLE_PRODUCT_CONFIGURATION_CHANGED,
                mapOf(KEY_CHANGED_FIELD to ruleChanged)
            )
        }
    }

    fun onCancel() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onSaveConfiguration() {
        configuration.value?.let {
            tracker.track(AnalyticsEvent.ORDER_FORM_BUNDLE_PRODUCT_CONFIGURATION_SAVE_TAPPED)
            when (flow) {
                is Flow.Selection -> triggerEvent(
                    MultiLiveEvent.Event.ExitWithResult(
                        SelectProductConfigurationResult(
                            productId = flow.productID,
                            productConfiguration = it
                        )
                    )
                )

                is Flow.Edit -> triggerEvent(
                    MultiLiveEvent.Event.ExitWithResult(
                        EditProductConfigurationResult(
                            productId = flow.productID,
                            itemId = flow.itemId,
                            productConfiguration = it
                        )
                    )
                )
            }
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

sealed class Flow(val productId: Long) : Parcelable {
    @Parcelize
    data class Selection(val productID: Long) : Flow(productID)

    @Parcelize
    data class Edit(
        val productID: Long,
        val itemId: Long,
        val configuration: ProductConfiguration
    ) : Flow(productID)
}

@Parcelize
data class SelectProductConfigurationResult(
    val productId: Long,
    val productConfiguration: ProductConfiguration
) : Parcelable

@Parcelize
data class EditProductConfigurationResult(
    val itemId: Long,
    val productId: Long,
    val productConfiguration: ProductConfiguration
) : Parcelable
