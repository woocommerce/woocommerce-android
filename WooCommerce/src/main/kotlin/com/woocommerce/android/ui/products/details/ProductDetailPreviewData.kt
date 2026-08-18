package com.woocommerce.android.ui.products.details

import com.woocommerce.android.R

internal object ProductDetailPreviewData {
    private val addRows = listOf(
        ProductDetailRowUiModel.Editable(
            key = "title",
            hint = R.string.product_detail_title_hint,
            text = "",
            shouldFocus = false,
            isReadOnly = false,
            badgeText = null,
            badgeTone = null,
            onTextChanged = {},
        ),
        ProductDetailRowUiModel.ComplexProperty(
            key = "description",
            title = R.string.product_description,
            value = "Describe your product",
            icon = null,
            showTitle = false,
            maxLines = 1,
            showDivider = false,
            onClick = {},
        ),
        ProductDetailRowUiModel.Button(
            key = "write_with_ai",
            text = R.string.product_sharing_write_with_ai,
            icon = R.drawable.ic_ai,
            showDivider = true,
            tooltip = null,
            link = ProductDetailButtonLinkUiModel(
                text = R.string.ai_product_description_learn_more_link,
                onClick = {},
            ),
            onClick = {},
        ),
    )

    private val addDetails = listOf(
        ProductDetailRowUiModel.PropertyGroup(
            key = "price",
            title = R.string.product_price,
            properties = listOf(ProductDetailPropertyValueUiModel("", "Add price")),
            icon = R.drawable.ic_gridicons_money,
            showTitle = false,
            showDivider = true,
            isHighlighted = false,
            propertyFormat = R.string.product_property_default_formatter,
            onClick = {},
        ),
        ProductDetailRowUiModel.PropertyGroup(
            key = "inventory",
            title = R.string.product_inventory,
            properties = listOf(ProductDetailPropertyValueUiModel("Stock status", "In stock")),
            icon = R.drawable.ic_gridicons_list_checkmark,
            showTitle = true,
            showDivider = true,
            isHighlighted = false,
            propertyFormat = R.string.product_property_default_formatter,
            onClick = {},
        ),
        ProductDetailRowUiModel.ComplexProperty(
            key = "type",
            title = R.string.product_type,
            value = "Physical product",
            icon = R.drawable.ic_gridicons_product,
            showTitle = true,
            maxLines = 1,
            showDivider = false,
            onClick = null,
        ),
    )

    val addProductState = ProductDetailScreenState.Content(
        cards = listOf(
            ProductDetailCardUiModel("primary", ProductDetailCardStyle.PRIMARY, "", addRows),
            ProductDetailCardUiModel("details", ProductDetailCardStyle.SECONDARY, "", addDetails),
        ),
        showAddMore = true,
        showLinkedProductPromo = false,
    )

    val existingProductState = ProductDetailScreenState.Content(
        cards = listOf(
            ProductDetailCardUiModel(
                key = "primary",
                style = ProductDetailCardStyle.PRIMARY,
                caption = "",
                rows = listOf(
                    addRows.first().let { (it as ProductDetailRowUiModel.Editable).copy(text = "Beanie") },
                    (addRows[1] as ProductDetailRowUiModel.ComplexProperty).copy(
                        value = "A warm beanie for every season.",
                        showTitle = true,
                    ),
                    addRows[2],
                ),
            ),
            ProductDetailCardUiModel(
                key = "details",
                style = ProductDetailCardStyle.SECONDARY,
                caption = "",
                rows = addDetails + ProductDetailRowUiModel.Rating(
                    key = "reviews",
                    title = R.string.product_reviews,
                    value = "6 approved reviews",
                    rating = 4.5f,
                    icon = R.drawable.ic_reviews,
                    showDivider = true,
                    onClick = {},
                ),
            ),
        ),
        showAddMore = true,
        showLinkedProductPromo = true,
    )

    val warningState = ProductDetailScreenState.Content(
        cards = listOf(
            ProductDetailCardUiModel(
                key = "warning",
                style = ProductDetailCardStyle.SECONDARY,
                caption = "",
                rows = listOf(
                    ProductDetailRowUiModel.Warning("warning", "Some variations are missing a price."),
                    ProductDetailRowUiModel.ComplexProperty(
                        key = "variations",
                        title = R.string.product_variations,
                        value = "3 variations",
                        icon = R.drawable.ic_gridicons_types,
                        showTitle = true,
                        maxLines = 1,
                        showDivider = false,
                        onClick = {},
                    ),
                ),
            ),
        ),
        showAddMore = true,
        showLinkedProductPromo = false,
    )
}
