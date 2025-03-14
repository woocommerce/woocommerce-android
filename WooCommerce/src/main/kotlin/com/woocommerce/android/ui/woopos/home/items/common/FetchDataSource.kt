package com.woocommerce.android.ui.woopos.home.items.common

import kotlinx.coroutines.flow.Flow

interface FetchDataSource<T> {
    fun fetchData(
        fetchOptions: FetchOptions
    ): Flow<FetchResult<T>>
}
