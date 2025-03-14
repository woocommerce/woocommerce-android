package com.woocommerce.android.ui.woopos.home.items.common

sealed class FetchResult<T> {
    data class Cached<T>(val data: List<T>) : FetchResult<T>()
    data class Remote<T>(val result: Result<List<T>>) : FetchResult<T>()
}
