package com.woocommerce.android.ui.woopos.home.items.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

abstract class WooPosBaseDataSource<T> : FetchDataSource<T> {

    protected abstract suspend fun fetchFromCache(fetchOptions: FetchOptions): List<T>
    protected abstract suspend fun fetchFromRemote(
        fetchOptions: FetchOptions,
    ): Result<List<T>>
    protected abstract suspend fun updateCache(fetchOptions: FetchOptions, data: List<T>)

    override fun fetchData(
        fetchOptions: FetchOptions
    ): Flow<FetchResult<T>> = flow {
        if (fetchOptions.forceRefresh) {
            updateCache(fetchOptions, data = emptyList())
        }

        val cachedData = fetchFromCache(fetchOptions)
        emit(FetchResult.Cached(cachedData))

        val remoteResult = fetchFromRemote(fetchOptions)
        if (remoteResult.isSuccess) {
            val remoteData = remoteResult.getOrThrow()
            updateCache(fetchOptions = fetchOptions, data = remoteData)
            emit(FetchResult.Remote(Result.success(remoteData)))
        } else {
            emit(
                FetchResult.Remote(
                    Result.failure(remoteResult.exceptionOrNull() ?: Exception("Unknown error"))
                )
            )
        }
    }.flowOn(Dispatchers.IO)
}
