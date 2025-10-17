package com.woocommerce.android.ui.bookings.list

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsOrderOption
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
    private val sortBy = MutableStateFlow(BookingListSortOption.NewestToOldest)

    private val searchResults = MutableStateFlow(emptyList<Booking>())

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookingsFlow: Flow<List<Booking>> = combine(
        searchQuery,
        filters,
        page,
        sortBy
    ) { query, filters, page, sortBy ->
        if (query.isNullOrEmpty()) {
            bookingsRepository.observeBookings(
                limit = page * PAGE_SIZE,
                filters = filters,
                order = sortBy.toBookingsOrderOption()
            )
        } else {
            searchResults.take(page * PAGE_SIZE)
        }
    }.flatMapLatest { it }

    suspend fun loadBookings(
        searchQuery: String? = null,
        filters: List<BookingsFilterOption> = emptyList(),
        sortBy: BookingListSortOption
    ): Result<Unit> = mutex.withLock {
        // Reset pagination attributes
        page.value = 1
        canLoadMore.set(true)

        val previousQuery = this.searchQuery.value
        this.searchQuery.value = searchQuery
        this.filters.value = filters
        this.sortBy.value = sortBy

        return@withLock if (searchQuery == null) {
            fetchBookings()
        } else {
            if (searchQuery != previousQuery) {
                searchResults.value = emptyList()
            }
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
        val pageToFetch = page.value
        val isSearching = !searchQuery.value.isNullOrEmpty()
        val order = sortBy.value.toBookingsOrderOption()
        return bookingsRepository.fetchBookings(
            page = pageToFetch,
            perPage = PAGE_SIZE,
            query = searchQuery.value,
            filters = filters.value,
            order = order
        ).onSuccess { result ->
            canLoadMore.set(result.hasMorePages)
            if (result.hasMorePages) {
                page.update { it + 1 }
            }
            if (isSearching) {
                if (pageToFetch == 1) {
                    searchResults.value = result.bookings
                } else {
                    searchResults.update { it + result.bookings }
                }
            }
        }.map { }
    }

    private fun BookingListSortOption.toBookingsOrderOption() = when (this) {
        BookingListSortOption.NewestToOldest -> BookingsOrderOption.DESC
        BookingListSortOption.OldestToNewest -> BookingsOrderOption.ASC
    }
}
