package com.woocommerce.android.ui.bookings.list

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class BookingListHandler @Inject constructor(
    private val bookingsRepository: BookingsRepository
) {
    companion object {
        @VisibleForTesting
        const val PAGE_SIZE = 25
    }

    private val mutex = Mutex()
    private var page = MutableStateFlow(1)
    private val canLoadMore = AtomicBoolean(false)

    private val searchQuery = MutableStateFlow<String?>(null)
    private val filters = MutableStateFlow<List<BookingsFilterOption>>(emptyList())

    private val searchResults = MutableStateFlow(emptyList<Booking>())

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookingsFlow: Flow<List<Booking>> = combine(searchQuery, filters, page) { query, filters, page ->
        if (query.isNullOrEmpty()) {
            bookingsRepository.observeBookings(limit = page * PAGE_SIZE, filters)
        } else {
            searchResults
        }
    }.flatMapLatest { it }

    suspend fun loadBookings(
        searchQuery: String? = null,
        filters: List<BookingsFilterOption> = emptyList()
    ): Result<Unit> = mutex.withLock {
        // Reset pagination attributes
        page.value = 1
        canLoadMore.set(true)

        this.searchQuery.value = searchQuery
        this.filters.value = filters

        return@withLock if (searchQuery == null) {
            fetchBookings()
        } else {
            searchResults.value = emptyList()
            if (searchQuery.isEmpty()) {
                // If the query is empty, return cached results directly
                canLoadMore.set(false)
                Result.success(Unit)
            } else {
                fetchBookings()
            }
        }
    }

    suspend fun loadMore(): Result<Unit> = mutex.withLock {
        if (!canLoadMore.get()) return@withLock Result.success(Unit)
        return fetchBookings()
    }

    private suspend fun fetchBookings(): Result<Unit> {
        val isSearching = !searchQuery.value.isNullOrEmpty()
        return bookingsRepository.fetchBookings(
            page = page.value,
            perPage = PAGE_SIZE,
            query = searchQuery.value,
            filters = filters.value
        ).onSuccess { result ->
            canLoadMore.set(result.hasMorePages)
            if (result.hasMorePages) {
                page.update { it + 1 }
            }
            if (isSearching) {
                searchResults.update { it + result.bookings }
            }
        }.map { }
    }
}
