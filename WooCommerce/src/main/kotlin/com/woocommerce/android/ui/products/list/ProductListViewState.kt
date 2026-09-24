package com.woocommerce.android.ui.products.list

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductListViewState(
    val isSkeletonShown: Boolean? = null,
    val isLoading: Boolean? = null,
    val isLoadingMore: Boolean? = null,
    val canLoadMore: Boolean? = null,
    val isRefreshing: Boolean? = null,
    val isTrashing: Boolean? = null,
    val query: String? = null,
    val isSkuSearch: Boolean = false,
    val filterCount: Int? = null,
    val isSearchActive: Boolean? = null,
    val isEmptyViewVisible: Boolean? = null,
    val sortingTitleResource: Int? = null,
    val displaySortAndFilterCard: Boolean? = null,
    val isAddProductButtonVisible: Boolean? = null
) : Parcelable
