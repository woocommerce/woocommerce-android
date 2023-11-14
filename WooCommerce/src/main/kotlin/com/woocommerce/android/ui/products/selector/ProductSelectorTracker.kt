package com.woocommerce.android.ui.products.selector

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_SEARCH_TYPE_ALL
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_SEARCH_TYPE_SKU
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ProductSelectorFlow
import javax.inject.Inject

class ProductSelectorTracker @Inject constructor(private val tracker: AnalyticsTrackerWrapper) {
    fun trackClearSelectionButtonClicked(flow: ProductSelectorFlow, source: ProductSelectorSource) {
        when (flow) {
            ProductSelectorFlow.OrderCreation -> {
                val sourceProperty = when (source) {
                    ProductSelectorSource.ProductSelector -> AnalyticsTracker.VALUE_PRODUCT_SELECTOR
                    ProductSelectorSource.VariationSelector -> AnalyticsTracker.VALUE_VARIATION_SELECTOR
                }
                tracker.track(
                    AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_CLEAR_SELECTION_BUTTON_TAPPED,
                    mapOf(AnalyticsTracker.KEY_SOURCE to sourceProperty)
                )
            }
            ProductSelectorFlow.CouponEdition -> {}
            ProductSelectorFlow.Undefined -> {}
        }
    }

    fun trackItemSelected(flow: ProductSelectorFlow) {
        when (flow) {
            ProductSelectorFlow.OrderCreation -> {
                tracker.track(
                    AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_ITEM_SELECTED,
                )
            }
            ProductSelectorFlow.CouponEdition -> {}
            ProductSelectorFlow.Undefined -> {}
        }
    }

    fun trackItemUnselected(flow: ProductSelectorFlow) {
        when (flow) {
            ProductSelectorFlow.OrderCreation -> {
                tracker.track(
                    AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_ITEM_UNSELECTED,
                )
            }
            ProductSelectorFlow.CouponEdition -> {}
            ProductSelectorFlow.Undefined -> {}
        }
    }

    fun trackDoneButtonClicked(
        flow: ProductSelectorFlow,
        selectedItems: List<ProductSelectorViewModel.SelectedItem>,
        selectedItemsSource: List<ProductSourceForTracking>,
        isFilterActive: Boolean,
    ) {
        when (flow) {
            ProductSelectorFlow.OrderCreation -> {
                tracker.track(
                    AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED,
                    mapOf(
                        AnalyticsTracker.KEY_PRODUCT_COUNT to selectedItems.size,
                        AnalyticsTracker.KEY_PRODUCT_SELECTOR_SOURCE to selectedItemsSource.map { it.name },
                        AnalyticsTracker.KEY_PRODUCT_SELECTOR_FILTER_STATUS to isFilterActive
                    )
                )
            }
            ProductSelectorFlow.CouponEdition -> {}
            ProductSelectorFlow.Undefined -> {}
        }
    }

    fun trackSearchTriggered(searchType: ProductListHandler.SearchType) {
        tracker.track(
            AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_SEARCH_TRIGGERED,
            mapOf(
                AnalyticsTracker.KEY_SEARCH_TYPE to when (searchType) {
                    ProductListHandler.SearchType.DEFAULT -> VALUE_SEARCH_TYPE_ALL
                    ProductListHandler.SearchType.SKU -> VALUE_SEARCH_TYPE_SKU
                }
            )
        )
    }

    enum class ProductSelectorSource {
        ProductSelector,
        VariationSelector
    }
}
