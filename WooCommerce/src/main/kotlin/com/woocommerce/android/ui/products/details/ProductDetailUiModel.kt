package com.woocommerce.android.ui.products.details

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.woocommerce.android.ui.products.models.ProductProperty

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

data class ProductDetailCardUiModel(
    val key: String,
    val style: ProductDetailCardStyle,
    val caption: String,
    val rows: List<ProductDetailRow>,
)

enum class ProductDetailCardStyle {
    PRIMARY,
    SECONDARY,
}

data class ProductDetailRow(
    val key: String,
    val property: ProductProperty,
    val showDivider: Boolean? = null,
)
