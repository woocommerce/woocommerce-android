package com.woocommerce.android.ui.products.details

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
sealed interface ProductDetailScreenState {
    data object Loading : ProductDetailScreenState

    data class Empty(@StringRes val message: Int? = null) : ProductDetailScreenState

    data class Error(@StringRes val message: Int) : ProductDetailScreenState

    data class Content(
        val cards: List<ProductDetailCardUiModel>,
        val showAddMore: Boolean,
        val showLinkedProductPromo: Boolean,
    ) : ProductDetailScreenState
}

@Immutable
sealed interface ProductDetailImageUiState {
    data object Loading : ProductDetailImageUiState
    data object Gallery : ProductDetailImageUiState
    data object AddImage : ProductDetailImageUiState
    data object Unavailable : ProductDetailImageUiState
    data object Hidden : ProductDetailImageUiState
}

@Immutable
data class ProductDetailCardUiModel(
    val key: String,
    val style: ProductDetailCardStyle,
    val caption: String,
    val rows: List<ProductDetailRowUiModel>,
)

enum class ProductDetailCardStyle {
    PRIMARY,
    SECONDARY,
}

enum class ProductDetailBadgeTone {
    NEUTRAL,
    WARNING,
}

@Immutable
sealed interface ProductDetailRowUiModel {
    val key: String

    data class Divider(
        override val key: String,
    ) : ProductDetailRowUiModel

    data class Property(
        override val key: String,
        @StringRes val title: Int,
        val value: String,
        val showDivider: Boolean,
    ) : ProductDetailRowUiModel

    data class ComplexProperty(
        override val key: String,
        @StringRes val title: Int?,
        val value: String,
        @DrawableRes val icon: Int?,
        val showTitle: Boolean,
        val maxLines: Int,
        val showDivider: Boolean,
        val onClick: (() -> Unit)?,
    ) : ProductDetailRowUiModel

    data class Rating(
        override val key: String,
        @StringRes val title: Int,
        val value: String,
        val rating: Float,
        @DrawableRes val icon: Int,
        val showDivider: Boolean,
        val onClick: (() -> Unit)?,
    ) : ProductDetailRowUiModel

    data class Editable(
        override val key: String,
        @StringRes val hint: Int,
        val text: String,
        val shouldFocus: Boolean,
        val isReadOnly: Boolean,
        @StringRes val badgeText: Int?,
        val badgeTone: ProductDetailBadgeTone?,
        val onTextChanged: ((String) -> Unit)?,
    ) : ProductDetailRowUiModel

    data class PropertyGroup(
        override val key: String,
        @StringRes val title: Int,
        val properties: List<ProductDetailPropertyValueUiModel>,
        @DrawableRes val icon: Int?,
        val showTitle: Boolean,
        val showDivider: Boolean,
        val isHighlighted: Boolean,
        @StringRes val propertyFormat: Int,
        val onClick: (() -> Unit)?,
    ) : ProductDetailRowUiModel

    data class Link(
        override val key: String,
        @StringRes val title: Int,
        @DrawableRes val icon: Int?,
        val showDivider: Boolean,
        val onClick: (() -> Unit)?,
    ) : ProductDetailRowUiModel

    data class Button(
        override val key: String,
        @StringRes val text: Int,
        @DrawableRes val icon: Int?,
        val showDivider: Boolean,
        val tooltip: ProductDetailTooltipUiModel?,
        val link: ProductDetailButtonLinkUiModel?,
        val onClick: () -> Unit,
    ) : ProductDetailRowUiModel

    data class Switch(
        override val key: String,
        @StringRes val title: Int,
        val isOn: Boolean,
        @DrawableRes val icon: Int?,
        val onStateChanged: ((Boolean) -> Unit)?,
    ) : ProductDetailRowUiModel

    data class Warning(
        override val key: String,
        val content: String,
    ) : ProductDetailRowUiModel
}

@Immutable
data class ProductDetailPropertyValueUiModel(
    val label: String,
    val value: String,
)

@Immutable
data class ProductDetailTooltipUiModel(
    @StringRes val title: Int,
    @StringRes val text: Int,
    @StringRes val dismissButtonText: Int,
    val onDismiss: () -> Unit,
)

@Immutable
data class ProductDetailButtonLinkUiModel(
    @StringRes val text: Int,
    val onClick: () -> Unit,
)
