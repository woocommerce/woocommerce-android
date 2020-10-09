package com.woocommerce.android.ui.products

import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem

class ProductDetailTypeBottomSheetBuilder : ProductTypeBottomSheetBuilder {
    override fun buildBottomSheetList(): List<ProductTypesBottomSheetUiItem> {
        return listOf(
            ProductTypesBottomSheetUiItem(
                type = SIMPLE,
                titleResource = R.string.product_type_simple,
                descResource = R.string.product_type_physical_desc,
                iconResource = R.drawable.ic_gridicons_product,
                isEnabled = true
            ),
            ProductTypesBottomSheetUiItem(
                type = VARIABLE,
                titleResource = R.string.product_type_variable,
                descResource = R.string.product_type_variation_desc,
                iconResource = R.drawable.ic_gridicons_types,
                isEnabled = true
            ),
            ProductTypesBottomSheetUiItem(
                type = GROUPED,
                titleResource = R.string.product_type_grouped,
                descResource = R.string.product_type_grouped_desc,
                iconResource = R.drawable.ic_widgets,
                isEnabled = true
            ),
            ProductTypesBottomSheetUiItem(
                type = EXTERNAL,
                titleResource = R.string.product_type_external,
                descResource = R.string.product_type_external_desc,
                iconResource = R.drawable.ic_gridicons_up_right,
                isEnabled = true
            )
        )
    }
}
