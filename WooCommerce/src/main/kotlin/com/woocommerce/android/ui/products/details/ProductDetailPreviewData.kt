package com.woocommerce.android.ui.products.details

import com.woocommerce.android.R
import com.woocommerce.android.ui.products.models.ProductProperty

internal object ProductDetailPreviewData {
    private val addRows = listOf(
        ProductDetailRow(
            key = "title",
            property = ProductProperty.Editable(
                hint = R.string.product_detail_title_hint,
                onTextChanged = {},
            ),
        ),
        ProductDetailRow(
            key = "description",
            property = ProductProperty.ComplexProperty(
                title = R.string.product_description,
                value = "Describe your product",
                showTitle = false,
                isDividerVisible = false,
                onClick = {},
            ),
        ),
        ProductDetailRow(
            key = "write_with_ai",
            property = ProductProperty.Button(
                text = R.string.product_sharing_write_with_ai,
                icon = R.drawable.ic_ai,
                tooltip = ProductProperty.Button.Tooltip(
                    title = R.string.ai_product_description_tooltip_title,
                    text = R.string.ai_product_description_tooltip_message,
                    dismissButtonText = R.string.ai_product_description_tooltip_dismiss,
                    onDismiss = {},
                ),
                link = ProductProperty.Button.Link(
                    text = R.string.ai_product_description_learn_more_link,
                    onClick = {},
                ),
                onClick = {},
            ),
        ),
    )

    private val addDetails = listOf(
        ProductDetailRow(
            key = "price",
            property = ProductProperty.PropertyGroup(
                title = R.string.product_price,
                properties = mapOf("" to "Add price"),
                icon = R.drawable.ic_gridicons_money,
                showTitle = false,
                onClick = {},
            ),
        ),
        ProductDetailRow(
            key = "inventory",
            property = ProductProperty.PropertyGroup(
                title = R.string.product_inventory,
                properties = mapOf("Stock status" to "In stock"),
                icon = R.drawable.ic_gridicons_list_checkmark,
                onClick = {},
            ),
        ),
        ProductDetailRow(
            key = "type",
            property = ProductProperty.ComplexProperty(
                title = R.string.product_type,
                value = "Physical product",
                icon = R.drawable.ic_gridicons_product,
                isDividerVisible = false,
            ),
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
                    addRows.first().let { row ->
                        row.copy(property = (row.property as ProductProperty.Editable).copy(text = "Beanie"))
                    },
                    addRows[1].let { row ->
                        row.copy(
                            property = (row.property as ProductProperty.ComplexProperty).copy(
                                value = "A warm beanie for every season.",
                                showTitle = true,
                            ),
                        )
                    },
                    addRows[2].let { row ->
                        row.copy(property = (row.property as ProductProperty.Button).copy(tooltip = null))
                    },
                ),
            ),
            ProductDetailCardUiModel(
                key = "details",
                style = ProductDetailCardStyle.SECONDARY,
                caption = "",
                rows = addDetails + ProductDetailRow(
                    key = "reviews",
                    property = ProductProperty.RatingBar(
                        title = R.string.product_reviews,
                        value = "6 approved reviews",
                        rating = 4.5f,
                        icon = R.drawable.ic_reviews,
                        onClick = {},
                    ),
                    showDivider = true,
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
                    ProductDetailRow(
                        key = "warning",
                        property = ProductProperty.Warning("Some variations are missing a price."),
                    ),
                    ProductDetailRow(
                        key = "variations",
                        property = ProductProperty.ComplexProperty(
                            title = R.string.product_variations,
                            value = "3 variations",
                            icon = R.drawable.ic_gridicons_types,
                            isDividerVisible = false,
                            onClick = {},
                        ),
                    ),
                ),
            ),
        ),
        showAddMore = true,
        showLinkedProductPromo = false,
    )
}
