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
        if (query == null) {
            bookingsRepository.observeBookings(
                limit = page * PAGE_SIZE,
                filters = filters,
                order = sortBy.toBookingsOrderOption()
            )
        } else {
            searchResults
        }
    }.flatMapLatest { it }

    suspend fun loadBookings(
        searchQuery: String? = null,
        forceRefresh: Boolean = false,
        filters: List<BookingsFilterOption> = emptyList(),
        sortBy: BookingListSortOption
    ): Result<Unit> = mutex.withLock {
        // Reset pagination attributes
        page.value = 1
        canLoadMore.set(true)

        this.searchQuery.value = searchQuery
        this.filters.value = filters
        this.sortBy.value = sortBy

        return@withLock if (searchQuery == null) {
            if (forceRefresh) {
                fetchBookings()
            } else {
                // Load from DB only
                Result.success(Unit)
            }
        } else {
            searchResults.value = emptyList()
            if (searchQuery.isEmpty()) {
                // If the query is empty, clear search results directly
                canLoadMore.set(false)
                Result.success(Unit)
            } else {
                searchBookings()
            }
        }
    }

    suspend fun loadMore(): Result<Unit> = mutex.withLock {
        if (!canLoadMore.get()) return@withLock Result.success(Unit)
        return if (searchQuery.value == null) {
            fetchBookings()
        } else {
            searchBookings()
        }
    }

    private suspend fun fetchBookings(): Result<Unit> {
        val order = sortBy.value.toBookingsOrderOption()
        return bookingsRepository.fetchBookings(
            page = page.value,
            perPage = PAGE_SIZE,
            filters = filters.value,
            order = order
        ).onSuccess { hasMorePages ->
            canLoadMore.set(hasMorePages)
            if (hasMorePages) {
                page.update { it + 1 }
            }
        }.map { }
    }

    private fun BookingListSortOption.toBookingsOrderOption() = when (this) {
        BookingListSortOption.NewestToOldest -> BookingsOrderOption.DESC
        BookingListSortOption.OldestToNewest -> BookingsOrderOption.ASC
    }

    private suspend fun searchBookings(): Result<Unit> {
        TODO("Not yet implemented")
    }
}
