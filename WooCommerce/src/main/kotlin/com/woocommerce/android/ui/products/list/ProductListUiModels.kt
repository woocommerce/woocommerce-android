package com.woocommerce.android.ui.products.list

internal data class ProductListScreenState(
    val products: List<ProductListItemUiModel> = emptyList(),
    val selectedProductIds: Set<Long> = emptySet(),
    val uploadingProductIds: Set<Long> = emptySet(),
    val highlightedProductId: Long? = null,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val isSkuSearch: Boolean = false,
    val filterCount: Int = 0,
    val sortingTitle: String = "",
    val showBrowsingControls: Boolean = false,
    val isAddProductAvailable: Boolean = true,
    val isBarcodeScanningAvailable: Boolean = false,
    val isSkeletonShown: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val isEmptyViewVisible: Boolean = false,
    val isPullToRefreshEnabled: Boolean = true,
) {
    val isSelecting: Boolean
        get() = selectedProductIds.isNotEmpty()

    val selectedProductCount: Int
        get() = selectedProductIds.size

    val headerMode: ProductListHeaderMode
        get() = when {
            isSelecting -> ProductListHeaderMode.Selection
            isSearchActive -> ProductListHeaderMode.Search
            else -> ProductListHeaderMode.Browsing
        }

    val headerContent: ProductListHeaderContent
        get() = ProductListHeaderContent(
            mode = headerMode,
            selectedProductCount = selectedProductCount,
        )
}

internal data class ProductListHeaderContent(
    val mode: ProductListHeaderMode,
    val selectedProductCount: Int,
)

internal enum class ProductListHeaderMode {
    Selection,
    Search,
    Browsing,
}

internal data class ProductListItemUiModel(
    val remoteId: Long,
    val name: String,
    val imageUrl: String,
    val status: String?,
    val isStatusPending: Boolean,
    val stockAndPrice: String,
    val sku: String?,
)
