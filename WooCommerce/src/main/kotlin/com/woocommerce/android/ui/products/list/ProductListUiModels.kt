package com.woocommerce.android.ui.products.list

internal data class ProductListScreenState(
    val products: List<ProductListItemUiModel> = emptyList(),
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
    val isSelecting: Boolean = false,
    val isPullToRefreshEnabled: Boolean = true,
)

internal data class ProductListItemUiModel(
    val remoteId: Long,
    val name: String,
    val imageUrl: String,
    val status: String?,
    val isStatusPending: Boolean,
    val stockAndPrice: String,
    val sku: String?,
    val isSelected: Boolean,
    val isUploadingMedia: Boolean,
    val isHighlighted: Boolean,
)
