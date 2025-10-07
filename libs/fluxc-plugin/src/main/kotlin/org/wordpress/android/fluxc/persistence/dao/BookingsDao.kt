package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsOrderOption
import org.wordpress.android.fluxc.persistence.entity.BookingEntity

@Dao
interface BookingsDao {
    companion object {
        const val DEFAULT_SELECT_QUERY = """
            SELECT * FROM Bookings
            WHERE localSiteId = :localSiteId
            AND (:startDateBefore IS NULL OR start < :startDateBefore)
            AND (:startDateAfter IS NULL OR start > :startDateAfter)
            AND (:customerId IS NULL OR customerId = :customerId)
            ORDER BY
                CASE WHEN :order = 'ASC' THEN start END ASC,
                CASE WHEN :order = 'DESC' THEN start END DESC
            LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END
            """
    }

    @Suppress("LongParameterList")
    @Query(DEFAULT_SELECT_QUERY)
    fun observeBookings(
        localSiteId: LocalId,
        limit: Int?,
        startDateBefore: Long?,
        startDateAfter: Long?,
        customerId: Long?,
        order: BookingsOrderOption
    ): Flow<List<BookingEntity>>

    @Suppress("LongParameterList")
    @Query(DEFAULT_SELECT_QUERY)
    suspend fun getBookings(
        localSiteId: LocalId,
        limit: Int?,
        startDateBefore: Long?,
        startDateAfter: Long?,
        customerId: Long?,
        order: BookingsOrderOption
    ): List<BookingEntity>

    @Query("SELECT * FROM Bookings WHERE localSiteId = :localSiteId AND id = :bookingId LIMIT 1")
    fun observeBooking(localSiteId: LocalId, bookingId: Long): Flow<BookingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: BookingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entities: List<BookingEntity>)

    @Query("DELETE FROM Bookings WHERE localSiteId = :localSiteId")
    suspend fun deleteAllForSite(localSiteId: LocalId)

    fun observeBookings(
        localSiteId: LocalId,
        limit: Int? = null,
        filters: List<BookingsFilterOption> = emptyList(),
        order: BookingsOrderOption
    ): Flow<List<BookingEntity>> {
        val dateRangeFilter = filters.filterIsInstance<BookingsFilterOption.DateRange>().firstOrNull()
        val customerFilter = filters.filterIsInstance<BookingsFilterOption.Customer>().firstOrNull()

        return observeBookings(
            localSiteId = localSiteId,
            limit = limit,
            startDateBefore = dateRangeFilter?.before?.epochSecond,
            startDateAfter = dateRangeFilter?.after?.epochSecond,
            customerId = customerFilter?.customerId,
            order = order
        )
    }

    suspend fun getBookings(
        localSiteId: LocalId,
        limit: Int? = null,
        filters: List<BookingsFilterOption> = emptyList(),
        order: BookingsOrderOption
    ): List<BookingEntity> {
        val dateRangeFilter = filters.filterIsInstance<BookingsFilterOption.DateRange>().firstOrNull()
        val customerFilter = filters.filterIsInstance<BookingsFilterOption.Customer>().firstOrNull()

        return getBookings(
            localSiteId = localSiteId,
            limit = limit,
            startDateBefore = dateRangeFilter?.before?.epochSecond,
            startDateAfter = dateRangeFilter?.after?.epochSecond,
            customerId = customerFilter?.customerId,
            order = order
        )
    }
}
