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
    private val searchResults = MutableStateFlow(emptyList<Booking>())

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookingsFlow: Flow<List<Booking>> = combine(searchQuery, page) { query, page ->
        if (query == null) {
            bookingsRepository.observeBookings(limit = page * PAGE_SIZE)
        } else {
            searchResults
        }
    }.flatMapLatest { it }

    suspend fun loadBookings(
        searchQuery: String? = null,
        forceRefresh: Boolean = false,
    ): Result<Unit> = mutex.withLock {
        // Reset pagination attributes
        page.value = 1
        canLoadMore.set(true)

        this.searchQuery.value = searchQuery
        return if (searchQuery == null) {
            if (forceRefresh) {
                fetchBookings()
            } else {
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
        return bookingsRepository.fetchBookings(
            page = page.value,
            perPage = PAGE_SIZE
        ).onSuccess { hasMorePages ->
            canLoadMore.set(hasMorePages)
            if (hasMorePages) {
                page.update { it + 1 }
            }
        }.map { }
    }

    private suspend fun searchBookings(): Result<Unit> {
        TODO("Not yet implemented")
    }
}
