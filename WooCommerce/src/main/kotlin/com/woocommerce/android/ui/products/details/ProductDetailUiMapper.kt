package com.woocommerce.android.ui.products.details

import com.woocommerce.android.R
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductDetailViewState.AuxiliaryState
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.Error
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.Loading
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductDetailViewState.AuxiliaryState.None
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductPropertyCard

class ProductDetailUiMapper {
    fun mapScreenState(
        auxiliaryState: AuxiliaryState,
        hasProduct: Boolean,
        cards: List<ProductDetailCardUiModel>,
        showAddMore: Boolean,
        showLinkedProductPromo: Boolean,
    ): ProductDetailScreenState = when (auxiliaryState) {
        Loading -> ProductDetailScreenState.Loading
        is Error -> if (auxiliaryState.message == R.string.product_detail_product_not_selected) {
            ProductDetailScreenState.Empty(auxiliaryState.message)
        } else {
            ProductDetailScreenState.Error(auxiliaryState.message)
        }
        None -> if (hasProduct) {
            ProductDetailScreenState.Content(
                cards = cards,
                showAddMore = showAddMore,
                showLinkedProductPromo = showLinkedProductPromo,
            )
        } else {
            ProductDetailScreenState.Empty()
        }
    }

    fun map(cards: List<ProductPropertyCard>): List<ProductDetailCardUiModel> {
        val cardKeyOccurrences = mutableMapOf<String, Int>()
        return cards.map { card ->
            val rows = mapRows(card.properties)
            val cardBaseKey = when (card.type) {
                ProductPropertyCard.Type.PRIMARY -> PRIMARY_CARD_KEY
                ProductPropertyCard.Type.SECONDARY -> if (card.properties.any { it.isBlazeProperty() }) {
                    BLAZE_CARD_KEY
                } else {
                    SECONDARY_CARD_KEY
                }
            }
            val cardKey = cardBaseKey.withOccurrence(cardKeyOccurrences)

            ProductDetailCardUiModel(
                key = cardKey,
                style = when (card.type) {
                    ProductPropertyCard.Type.PRIMARY -> ProductDetailCardStyle.PRIMARY
                    ProductPropertyCard.Type.SECONDARY -> ProductDetailCardStyle.SECONDARY
                },
                caption = card.caption,
                rows = rows,
            )
        }
    }

    private fun mapRows(properties: List<ProductProperty>): List<ProductDetailRowUiModel> {
        val keyOccurrences = mutableMapOf<String, Int>()
        return properties.mapIndexed { index, property ->
            val key = property.semanticKey().withOccurrence(keyOccurrences)
            property.toUiModel(key).let { row ->
                if (row is ProductDetailRowUiModel.Rating) {
                    row.copy(showDivider = index != properties.lastIndex)
                } else {
                    row
                }
            }
        }
    }

    private fun ProductProperty.toUiModel(key: String): ProductDetailRowUiModel = when (this) {
        ProductProperty.Divider -> ProductDetailRowUiModel.Divider(key)
        is ProductProperty.Property -> toPropertyUiModel(key)
        is ProductProperty.ComplexProperty -> toComplexPropertyUiModel(key)
        is ProductProperty.RatingBar -> toRatingUiModel(key)
        is ProductProperty.Editable -> toEditableUiModel(key)
        is ProductProperty.PropertyGroup -> toPropertyGroupUiModel(key)
        is ProductProperty.Link -> toLinkUiModel(key)
        is ProductProperty.Button -> toButtonUiModel(key)
        is ProductProperty.Switch -> toSwitchUiModel(key)
        is ProductProperty.Warning -> ProductDetailRowUiModel.Warning(key, content)
    }

    private fun ProductProperty.Property.toPropertyUiModel(key: String) =
        ProductDetailRowUiModel.Property(
            key = key,
            title = title,
            value = value,
            showDivider = isDividerVisible,
        )

    private fun ProductProperty.ComplexProperty.toComplexPropertyUiModel(key: String) =
        ProductDetailRowUiModel.ComplexProperty(
            key = key,
            title = title,
            value = value,
            icon = icon,
            showTitle = showTitle,
            maxLines = maxLines,
            showDivider = isDividerVisible,
            onClick = onClick,
        )

    private fun ProductProperty.RatingBar.toRatingUiModel(key: String) =
        ProductDetailRowUiModel.Rating(
            key = key,
            title = title,
            value = value,
            rating = rating,
            icon = icon,
            showDivider = false,
            onClick = onClick,
        )

    private fun ProductProperty.Editable.toEditableUiModel(key: String) =
        ProductDetailRowUiModel.Editable(
            key = key,
            hint = hint,
            text = text,
            shouldFocus = shouldFocus,
            isReadOnly = isReadOnly,
            badgeText = badgeText,
            badgeTone = badgeColor?.let {
                if (it == R.color.product_status_badge_pending) {
                    ProductDetailBadgeTone.WARNING
                } else {
                    ProductDetailBadgeTone.NEUTRAL
                }
            },
            onTextChanged = onTextChanged,
        )

    private fun ProductProperty.PropertyGroup.toPropertyGroupUiModel(key: String) =
        ProductDetailRowUiModel.PropertyGroup(
            key = key,
            title = title,
            properties = properties.entries.map { ProductDetailPropertyValueUiModel(it.key, it.value) },
            icon = icon,
            showTitle = showTitle,
            showDivider = isDividerVisible,
            isHighlighted = isHighlighted,
            propertyFormat = propertyFormat,
            onClick = onClick,
        )

    private fun ProductProperty.Link.toLinkUiModel(key: String) =
        ProductDetailRowUiModel.Link(
            key = key,
            title = title,
            icon = icon,
            showDivider = isDividerVisible,
            onClick = onClick,
        )

    private fun ProductProperty.Button.toButtonUiModel(key: String) =
        ProductDetailRowUiModel.Button(
            key = key,
            text = text,
            icon = icon,
            showDivider = isDividerVisible,
            tooltip = tooltip?.let {
                ProductDetailTooltipUiModel(
                    title = it.title,
                    text = it.text,
                    dismissButtonText = it.dismissButtonText,
                    onDismiss = it.onDismiss,
                )
            },
            link = link?.let { ProductDetailButtonLinkUiModel(it.text, it.onClick) },
            onClick = onClick,
        )

    private fun ProductProperty.Switch.toSwitchUiModel(key: String) =
        ProductDetailRowUiModel.Switch(
            key = key,
            title = title,
            isOn = isOn,
            icon = icon,
            onStateChanged = onStateChanged,
        )

    private fun ProductProperty.semanticKey(): String = when (this) {
        ProductProperty.Divider -> "divider"
        is ProductProperty.Property -> "property_$title"
        is ProductProperty.ComplexProperty -> "complex_${title ?: NO_RESOURCE}_${icon ?: NO_RESOURCE}"
        is ProductProperty.RatingBar -> "rating_$title"
        is ProductProperty.Editable -> "editable_$hint"
        is ProductProperty.PropertyGroup -> "group_$title"
        is ProductProperty.Link -> "link_$title"
        is ProductProperty.Button -> "button_$text"
        is ProductProperty.Switch -> "switch_$title"
        is ProductProperty.Warning -> "warning"
    }

    private fun ProductProperty.isBlazeProperty() =
        this is ProductProperty.Link && title == R.string.product_details_blaze_card

    private fun String.withOccurrence(occurrences: MutableMap<String, Int>): String {
        val occurrence = occurrences.getOrDefault(this, 0)
        occurrences[this] = occurrence + 1
        return if (occurrence == 0) this else "${this}_$occurrence"
    }

    private companion object {
        const val PRIMARY_CARD_KEY = "primary"
        const val SECONDARY_CARD_KEY = "secondary"
        const val BLAZE_CARD_KEY = "blaze"
        const val NO_RESOURCE = 0
    }
}
