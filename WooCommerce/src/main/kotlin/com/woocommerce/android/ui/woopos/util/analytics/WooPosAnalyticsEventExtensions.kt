package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.analytics.IAnalyticsEvent
import com.woocommerce.android.pos.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.pos.analytics.WooPosAnalyticsEventConstant.CartSource
import com.woocommerce.android.pos.analytics.WooPosAnalyticsEventConstant.ItemsListItemType
import com.woocommerce.android.pos.analytics.WooPosAnalyticsEventConstant.ItemsListProductType
import com.woocommerce.android.pos.analytics.WooPosAnalyticsEventConstant.ItemsListSource
import com.woocommerce.android.pos.analytics.WooPosAnalyticsEventConstant.ItemsListSourceType
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource.SyncStrategy
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability

/**
 * Extension function to add properties to analytics events.
 * This is a convenience method to add properties to WooPosAnalyticsEvent instances.
 */
internal fun IAnalyticsEvent.addProperties(additionalProperties: Map<String, String>) {
    when (this) {
        is WooPosAnalyticsEvent -> addProperties(additionalProperties)
        else -> error("Cannot add properties to non-WooPosAnalytics event")
    }
}

/**
 * Creates an ItemAddedToCart event from ItemClickedData.
 */
fun WooPosItemsViewModel.ItemClickedData.toItemAddedToCart(
    source: ItemsListSource,
    sourceType: ItemsListSourceType
): WooPosAnalyticsEvent.Event.ItemAddedToCart {
    return WooPosAnalyticsEvent.Event.ItemAddedToCart(
        source = source,
        sourceType = sourceType,
        itemType = when (this) {
            is WooPosItemsViewModel.ItemClickedData.Product -> ItemsListItemType.PRODUCT
            is WooPosItemsViewModel.ItemClickedData.Coupon -> ItemsListItemType.COUPON
            is WooPosItemsViewModel.ItemClickedData.VariableProduct -> {
                error("VariableProduct is not a valid item type")
            }
        },
        productType = when (this) {
            is WooPosItemsViewModel.ItemClickedData.Product.Simple -> ItemsListProductType.SIMPLE
            is WooPosItemsViewModel.ItemClickedData.Product.Variation -> ItemsListProductType.VARIATION
            is WooPosItemsViewModel.ItemClickedData.Coupon -> null
            is WooPosItemsViewModel.ItemClickedData.VariableProduct -> {
                error("VariableProduct is not a valid item type")
            }
        }
    )
}

/**
 * Creates an ItemAddedToCart event from WooPosCartItemViewState (for barcode scanner).
 */
fun WooPosCartItemViewState.toItemAddedToCart(): WooPosAnalyticsEvent.Event.ItemAddedToCart {
    return WooPosAnalyticsEvent.Event.ItemAddedToCart(
        source = null,
        sourceType = ItemsListSourceType.BARCODE_SCANNER,
        itemType = when (this) {
            is WooPosCartItemViewState.Loading -> ItemsListItemType.LOADING
            is WooPosCartItemViewState.Coupon -> ItemsListItemType.COUPON
            is WooPosCartItemViewState.Product.Simple -> ItemsListItemType.PRODUCT
            is WooPosCartItemViewState.Product.Variation -> ItemsListItemType.PRODUCT
            is WooPosCartItemViewState.Error -> ItemsListItemType.ERROR
        },
        productType = when (this) {
            is WooPosCartItemViewState.Coupon,
            is WooPosCartItemViewState.Error,
            is WooPosCartItemViewState.Loading -> null

            is WooPosCartItemViewState.Product.Simple -> ItemsListProductType.SIMPLE
            is WooPosCartItemViewState.Product.Variation -> ItemsListProductType.VARIATION
        },
        error = if (this is WooPosCartItemViewState.Error) this.message else null
    )
}

/**
 * Creates an ItemRemovedFromCart event from WooPosCartItemViewState.
 */
fun WooPosCartItemViewState.toItemRemovedFromCart(
    source: CartSource
): WooPosAnalyticsEvent.Event.ItemRemovedFromCart {
    return WooPosAnalyticsEvent.Event.ItemRemovedFromCart(
        source = source,
        itemType = when (this) {
            is WooPosCartItemViewState.Product -> ItemsListItemType.PRODUCT
            is WooPosCartItemViewState.Coupon -> ItemsListItemType.COUPON
            is WooPosCartItemViewState.Error -> ItemsListItemType.ERROR
            is WooPosCartItemViewState.Loading -> ItemsListItemType.LOADING
        },
        productType = when (this) {
            is WooPosCartItemViewState.Product.Simple -> ItemsListProductType.SIMPLE
            is WooPosCartItemViewState.Product.Variation -> ItemsListProductType.VARIATION
            is WooPosCartItemViewState.Coupon,
            is WooPosCartItemViewState.Error,
            is WooPosCartItemViewState.Loading -> null
        }
    )
}

/**
 * Converts SyncStrategy to analytics value string.
 */
fun SyncStrategy.toAnalyticsValue(): String {
    return when (this) {
        SyncStrategy.REMOTE -> "remote"
        SyncStrategy.LOCAL_CATALOG -> "local_catalog"
    }
}

/**
 * Converts NonLaunchabilityReason to analytics reason string.
 */
fun WooPosLaunchability.NonLaunchabilityReason.toAnalyticsReason(): String {
    return when (this) {
        WooPosLaunchability.NonLaunchabilityReason.WooCommercePluginNotFound -> "unknown_wc_plugin"
        WooPosLaunchability.NonLaunchabilityReason.UnsupportedWooCommerceVersion -> "wc_plugin_version"
        WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled -> "feature_switch_disabled"
        WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency -> "store_currency"
        WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable,
        WooPosLaunchability.NonLaunchabilityReason.UnknownNoPositiveCache,
        WooPosLaunchability.NonLaunchabilityReason.NoSiteSelected -> "other"
    }
}
