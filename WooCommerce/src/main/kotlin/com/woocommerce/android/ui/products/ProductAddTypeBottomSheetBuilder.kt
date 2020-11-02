package com.woocommerce.android.ui.products

import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem

class ProductAddTypeBottomSheetBuilder : ProductTypeBottomSheetBuilder {
    override fun buildBottomSheetList(): List<ProductTypesBottomSheetUiItem> {
        return listOf(
            ProductTypesBottomSheetUiItem(
                type = SIMPLE,
                titleResource = string.product_add_type_simple,
                descResource = string.product_add_type_simple_desc,
                iconResource = drawable.ic_gridicons_product,
                isEnabled = true
            ),
            ProductTypesBottomSheetUiItem(
                type = VARIABLE,
                titleResource = string.product_add_type_variable,
                descResource = string.product_add_type_variable_desc,
                iconResource = drawable.ic_gridicons_types,
                isEnabled = false
            ),
            ProductTypesBottomSheetUiItem(
                type = GROUPED,
                titleResource = string.product_add_type_grouped,
                descResource = string.product_add_type_grouped_desc,
                iconResource = drawable.ic_widgets,
                isEnabled = true
            ),
            ProductTypesBottomSheetUiItem(
                type = EXTERNAL,
                titleResource = string.product_add_type_external,
                descResource = string.product_add_type_external_desc,
                iconResource = drawable.ic_gridicons_up_right,
                isEnabled = true
            )
        )
    }
}
