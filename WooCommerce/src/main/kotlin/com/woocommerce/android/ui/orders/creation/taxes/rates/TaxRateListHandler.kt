package com.woocommerce.android.ui.orders.creation.taxes.rates

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class TaxRateListHandler @Inject constructor(private val repository: TaxRateRepository) {
    private val mutex = Mutex()
    private var page = 1
    private var canLoadMore = true

    val taxRatesFlow: Flow<List<TaxRate>> = repository.observeTaxRates()

    suspend fun fetchTaxRates(
        forceRefresh: Boolean = false
    ): Result<Unit> = mutex.withLock {
        // Reset pagination attributes
        page = 1
        canLoadMore = true
        return if (forceRefresh) {
            loadTaxRates()
        } else {
            Result.success(Unit)
        }
    }

    suspend fun loadMore(): Result<Unit> = mutex.withLock {
        if (!canLoadMore) return@withLock Result.success(Unit)
        return loadTaxRates()
    }

    private suspend fun loadTaxRates(): Result<Unit> {
        return repository.fetchTaxRates(page, PAGE_SIZE).onSuccess {
            canLoadMore = it
            page++
        }.map {}
    }

    companion object {
        private const val PAGE_SIZE = 10
    }
}
