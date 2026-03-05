package com.woocommerce.android.ui.bookings.list

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsOrderOption
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class BookingListHandler @Inject constructor(
    private val bookingsRepository: BookingsRepository
) {
    companion object {
        @VisibleForTesting
        const val PAGE_SIZE = 25
        private const val MIN_FETCH_DURATION_MS = 100L
    }

    private val mutex = Mutex()
    private var page = MutableStateFlow(1)
    private val _canLoadMore = AtomicBoolean(false)
    val hasMorePages: Boolean get() = _canLoadMore.get()

    private val searchQuery = MutableStateFlow<String?>(null)
    private val filters = MutableStateFlow(BookingFilters.EMPTY)
    private val sortBy = MutableStateFlow(BookingListSortOption.NewestToOldest)

    private val searchResults = MutableStateFlow(emptyList<Booking>())

    private data class QueryParams(
        val searchQuery: String?,
        val filters: BookingFilters,
        val page: Int,
        val sortBy: BookingListSortOption
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookingsFlow: Flow<List<Booking>> = combine(
        searchQuery,
        filters,
        page,
        sortBy
    ) { query, filters, page, sortBy ->
        QueryParams(query, filters, page, sortBy)
    }.flatMapLatest { params ->
        if (params.searchQuery.isNullOrEmpty()) {
            val limit = params.page * PAGE_SIZE
            val order = params.sortBy.toBookingsOrderOption()
            flow {
                // Emit a snapshot from the DB immediately so cached data shows without delay
                val snapshot = bookingsRepository.getBookingsList(
                    limit = limit,
                    filters = params.filters,
                    order = order
                )
                emit(snapshot)
                // Then subscribe to Room flow for ongoing updates
                emitAll(
                    bookingsRepository.observeBookings(
                        limit = limit,
                        filters = params.filters,
                        order = order
                    )
                )
            }
        } else {
            searchResults.map { it.take(params.page * PAGE_SIZE) }
        }
    }

    suspend fun loadBookings(
        searchQuery: String? = null,
        filters: BookingFilters = BookingFilters.EMPTY,
        sortBy: BookingListSortOption
    ): Result<Int> = mutex.withLock {
        // Reset pagination attributes
        page.value = 1
        _canLoadMore.set(true)

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
                // Mimic network delay to allow the UI to show then hide the refreshing indicator
                delay(MIN_FETCH_DURATION_MS)
                Result.success(0)
            } else {
                fetchBookings()
            }
        }
    }

    suspend fun loadMore(): Result<Int> = mutex.withLock {
        if (!_canLoadMore.get()) return@withLock Result.success(0)
        return fetchBookings()
    }

    private suspend fun fetchBookings(): Result<Int> {
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
            _canLoadMore.set(result.hasMorePages)
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
        }.map { it.bookings.size }
    }

    private fun BookingListSortOption.toBookingsOrderOption() = when (this) {
        BookingListSortOption.NewestToOldest -> BookingsOrderOption.DESC
        BookingListSortOption.OldestToNewest -> BookingsOrderOption.ASC
    }
}
