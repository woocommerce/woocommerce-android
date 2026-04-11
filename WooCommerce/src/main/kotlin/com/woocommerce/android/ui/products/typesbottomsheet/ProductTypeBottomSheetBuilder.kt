package com.woocommerce.android.ui.products.typesbottomsheet

import com.woocommerce.android.R
import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.typesbottomsheet.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem
import com.woocommerce.android.ui.subscriptions.IsEligibleForSubscriptions
import javax.inject.Inject

class ProductTypeBottomSheetBuilder @Inject constructor(
    private val isEligibleForSubscriptions: IsEligibleForSubscriptions,
    private val ciabSiteGateKeeper: CIABSiteGateKeeper
) {
    @Suppress("LongMethod")
    suspend fun buildBottomSheetList(): List<ProductTypesBottomSheetUiItem> {
        val isEligibleForSubscriptions = isEligibleForSubscriptions()

        return listOf(
            ProductTypesBottomSheetUiItem(
                type = ProductType.SIMPLE,
                titleResource = R.string.product_type_simple_title,
                descResource = R.string.product_type_simple_desc,
                iconResource = R.drawable.ic_gridicons_product
            ),
            ProductTypesBottomSheetUiItem(
                type = ProductType.SIMPLE,
                titleResource = R.string.product_type_virtual_title,
                descResource = R.string.product_type_virtual_desc,
                iconResource = R.drawable.ic_gridicons_cloud_outline,
                isVirtual = true
            ),
            ProductTypesBottomSheetUiItem(
                type = ProductType.SUBSCRIPTION,
                titleResource = R.string.product_type_simple_subscription_title,
                descResource = R.string.product_type_simple_subscription_desc,
                iconResource = R.drawable.ic_event_repeat,
                isVisible = isEligibleForSubscriptions
            ),
            ProductTypesBottomSheetUiItem(
                type = ProductType.VARIABLE,
                titleResource = R.string.product_type_variable_title,
                descResource = R.string.product_type_variable_desc,
                iconResource = R.drawable.ic_gridicons_types,
                isVisible = ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.VariableProducts)
            ),

            ProductTypesBottomSheetUiItem(
                type = ProductType.VARIABLE_SUBSCRIPTION,
                titleResource = R.string.product_type_variable_subscription_title,
                descResource = R.string.product_type_variable_subscription_desc,
                iconResource = R.drawable.ic_event_repeat,
                isVisible = isEligibleForSubscriptions
            ),
            ProductTypesBottomSheetUiItem(
                type = ProductType.GROUPED,
                titleResource = R.string.product_type_grouped_title,
                descResource = R.string.product_type_grouped_desc,
                iconResource = R.drawable.ic_widgets,
                isVisible = ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.GroupedProducts)
            ),
            ProductTypesBottomSheetUiItem(
                type = ProductType.EXTERNAL,
                titleResource = R.string.product_type_external_title,
                descResource = R.string.product_type_external_desc,
                iconResource = R.drawable.ic_arrow_outward_24dp
            ),
            ProductTypesBottomSheetUiItem(
                type = ProductType.BOOKABLE_SERVICE,
                titleResource = R.string.product_type_bookable_service_title,
                descResource = R.string.product_type_bookable_service_desc,
                iconResource = R.drawable.ic_calendar_add_outline,
                isVisible = ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.BookableServiceCreation),
                supportsNativeEditor = false
            )
        )
    }
}
