package com.woocommerce.android.ui.products

import com.woocommerce.android.R
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem

class StartProductAddTypeBottomSheetBuilder : ProductTypeBottomSheetBuilder {
    override fun buildBottomSheetList(): List<ProductTypesBottomSheetUiItem> {
        return listOf(
            ProductTypesBottomSheetUiItem(
                type = SIMPLE,
                titleResource = string.product_add_type_simple,
                descResource = string.product_add_type_simple_desc,
                iconResource = drawable.ic_gridicons_product
            ),
            ProductTypesBottomSheetUiItem(
                type = VARIABLE,
                titleResource = R.string.product_add_type_variable,
                descResource = R.string.product_add_type_variable_desc,
                iconResource = R.drawable.ic_gridicons_types
            ),
            ProductTypesBottomSheetUiItem(
                type = GROUPED,
                titleResource = R.string.product_add_type_grouped,
                descResource = R.string.product_add_type_grouped_desc,
                iconResource = R.drawable.ic_widgets
            ),
            ProductTypesBottomSheetUiItem(
                type = EXTERNAL,
                titleResource = R.string.product_add_type_external,
                descResource = R.string.product_add_type_external_desc,
                iconResource = R.drawable.ic_gridicons_up_right
            )
        )
    }
}
