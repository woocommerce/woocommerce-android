package com.woocommerce.android.ui.woopos.home.items.common

import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

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

    suspend fun fetchMore(
        fetchMore: suspend () -> Result<List<T>>,
        productId: Long? = null,
    ): Result<List<T>> = withContext(Dispatchers.IO) {
        val result = fetchMore()
        if (result.isSuccess) {
            val newItems = result.getOrThrow()
            updateCache(fetchOptions = FetchOptions(productId = productId), newItems)
            Result.success(fetchFromCache(fetchOptions = FetchOptions(productId = productId)))
        } else {
            result.logFailure()
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error while loading more data"))
        }
    }

    private fun <T> Result<T>.logFailure() {
        val error = exceptionOrNull()
        val errorMessage = error?.message ?: "Unknown error"
        WooLog.e(WooLog.T.POS, "Loading products failed - $errorMessage", error)
    }
}
