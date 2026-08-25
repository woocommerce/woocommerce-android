package com.woocommerce.android.ui.products.details

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.woocommerce.android.R
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

data class ProductDetailPageUiState(
    val title: String,
    val topAppBar: ProductDetailTopAppBarUiState,
    val screen: ProductDetailScreenState,
    val image: ProductDetailImageUiState,
    val showUploadError: Boolean,
)

data class ProductDetailTopAppBarUiState(
    val navigation: ProductDetailTopAppBarNavigation?,
    val primaryAction: ProductDetailTopAppBarAction?,
    val shareAction: ProductDetailTopAppBarAction?,
    val overflowActions: List<ProductDetailTopAppBarAction>,
)

enum class ProductDetailTopAppBarNavigation {
    BACK,
    CLOSE,
}

enum class ProductDetailTopAppBarAction(
    @StringRes val label: Int,
    val isDestructive: Boolean = false,
) {
    SAVE(R.string.save),
    PUBLISH(R.string.product_add_tool_bar_menu_button_done),
    SAVE_AS_DRAFT(R.string.product_detail_save_as_draft),
    SHARE(R.string.share),
    VIEW_PRODUCT(R.string.product_view_in_store),
    SETTINGS(R.string.product_settings),
    DUPLICATE(R.string.product_duplicate),
    TRASH(R.string.product_trash, isDestructive = true),
}

@Immutable
data class ProductDetailPageCallbacks(
    val topAppBar: ProductDetailTopAppBarCallbacks,
    val image: ProductDetailImageCallbacks,
    val content: ProductDetailContentCallbacks,
    val onUploadErrorClicked: () -> Unit,
)

@Immutable
data class ProductDetailTopAppBarCallbacks(
    val onNavigationClicked: () -> Unit,
    val onActionClicked: (ProductDetailTopAppBarAction) -> Unit,
)

@Immutable
data class ProductDetailImageCallbacks(
    val onImageClicked: () -> Unit,
    val onAddImageClicked: () -> Unit,
    val onImagesUnavailableClicked: () -> Unit,
)

@Immutable
data class ProductDetailContentCallbacks(
    val onLinkedProductPromoClicked: () -> Unit,
    val onLinkedProductPromoDismissed: () -> Unit,
    val onAddMoreClicked: () -> Unit,
)

sealed interface ProductDetailImageUiState {
    data object Loading : ProductDetailImageUiState

    data class Gallery(val items: List<ProductDetailImageUiItem>) : ProductDetailImageUiState

    data object AddImage : ProductDetailImageUiState
    data object Unavailable : ProductDetailImageUiState
    data object Hidden : ProductDetailImageUiState
}

@Immutable
sealed interface ProductDetailImageUiItem {
    val key: String

    data class Persisted(
        override val key: String,
        val source: String,
        val isCover: Boolean,
    ) : ProductDetailImageUiItem

    data class Uploading(
        override val key: String,
        val source: String,
    ) : ProductDetailImageUiItem

    data object Add : ProductDetailImageUiItem {
        override val key = "add_image"
    }
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
