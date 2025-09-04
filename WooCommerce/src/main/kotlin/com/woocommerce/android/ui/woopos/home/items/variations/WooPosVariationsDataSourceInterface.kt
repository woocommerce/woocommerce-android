package com.woocommerce.android.ui.woopos.home.items.variations

import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import kotlinx.coroutines.flow.Flow

interface WooPosVariationsDataSourceInterface {
    suspend fun resetState()
    fun canLoadMore(numOfVariations: Int): Boolean
    fun fetchFirstPage(productId: Long, forceRefresh: Boolean = true): Flow<FetchResult>
    suspend fun loadMore(productId: Long): Result<List<WooPosVariation>>
}
