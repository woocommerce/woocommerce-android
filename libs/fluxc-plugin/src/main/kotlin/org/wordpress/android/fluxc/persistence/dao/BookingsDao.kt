package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsOrderOption
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.BookingResourceEntity
import kotlin.collections.orEmpty

@Dao
interface BookingsDao {
    companion object {
        const val DEFAULT_SELECT_QUERY = """
            SELECT * FROM Bookings
            WHERE localSiteId = :localSiteId
            AND (:startDateBefore IS NULL OR start <= :startDateBefore)
            AND (:startDateAfter IS NULL OR start >= :startDateAfter)
            AND (:userId IS NULL OR userId = :userId)
            AND (:attendanceStatus IS NULL OR attendanceStatus = :attendanceStatus)
            AND status NOT IN (:excludedBookingStatuses)
            AND ((:resourceIdsSize = 0) OR resourceId IN (:resourceIds))
            AND ((:productIdsSize = 0) OR productId IN (:productIds))
            ORDER BY
                CASE WHEN :order = 'ASC' THEN start END ASC,
                CASE WHEN :order = 'DESC' THEN start END DESC,
                CASE WHEN :order = 'ASC' THEN id END ASC,
                CASE WHEN :order = 'DESC' THEN id END DESC
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
        userId: Long?,
        resourceIds: List<Long>,
        resourceIdsSize: Int,
        attendanceStatus: String?,
        excludedBookingStatuses: List<String>,
        productIds: List<Long>,
        productIdsSize: Int,
        order: BookingsOrderOption
    ): Flow<List<BookingEntity>>

    @Suppress("LongParameterList")
    @Query(DEFAULT_SELECT_QUERY)
    suspend fun getBookings(
        localSiteId: LocalId,
        limit: Int?,
        startDateBefore: Long?,
        startDateAfter: Long?,
        userId: Long?,
        resourceIds: List<Long>,
        resourceIdsSize: Int,
        attendanceStatus: String?,
        excludedBookingStatuses: List<String>,
        productIds: List<Long>,
        productIdsSize: Int,
        order: BookingsOrderOption
    ): List<BookingEntity>

    @Query("SELECT * FROM Bookings WHERE localSiteId = :localSiteId AND id = :bookingId LIMIT 1")
    fun observeBooking(localSiteId: LocalId, bookingId: Long): Flow<BookingEntity?>

    @Query("SELECT COUNT(*) FROM Bookings WHERE localSiteId = :localSiteId")
    fun observeBookingsCount(localSiteId: LocalId): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BookingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entities: List<BookingEntity>)

    @Query("DELETE FROM Bookings WHERE localSiteId = :localSiteId")
    suspend fun deleteAllForSite(localSiteId: LocalId)

    @Transaction
    suspend fun replaceAllForSite(siteId: LocalId, entities: List<BookingEntity>) {
        deleteAllForSite(siteId)
        upsert(entities)
    }

    @Suppress("LongParameterList")
    @Query(
        """
            DELETE FROM Bookings
            WHERE localSiteId = :localSiteId
            AND (:startDateBefore IS NULL OR start <= :startDateBefore)
            AND (:startDateAfter IS NULL OR start >= :startDateAfter)
            AND (:userId IS NULL OR userId = :userId)
            AND (:attendanceStatus IS NULL OR attendanceStatus = :attendanceStatus)
            AND status NOT IN (:excludedBookingStatuses)
            AND ((:resourceIdsSize = 0) OR resourceId IN (:resourceIds))
            AND ((:productIdsSize = 0) OR productId IN (:productIds))
            AND ((:keepIdsSize = 0) OR id NOT IN (:keepIds))
        """
    )
    suspend fun deleteStaleBookings(
        localSiteId: LocalId,
        startDateBefore: Long?,
        startDateAfter: Long?,
        userId: Long?,
        resourceIds: List<Long>,
        resourceIdsSize: Int,
        attendanceStatus: String?,
        excludedBookingStatuses: List<String>,
        productIds: List<Long>,
        productIdsSize: Int,
        keepIds: List<Long>,
        keepIdsSize: Int,
    )

    private suspend fun deleteStaleBookings(
        localSiteId: LocalId,
        filters: BookingFilters,
        keepIds: List<Long>
    ) {
        val resourceIdsKeySet = filters.teamMembers.values.map { it.value }
        val excludedBookingStatusKeySet = filters.excludedBookingStatuses.values.map { it.key }
        val productIds = filters.serviceEvents.values.map { it.productId }

        deleteStaleBookings(
            localSiteId = localSiteId,
            startDateBefore = filters.dateRange.before?.epochSecond,
            startDateAfter = filters.dateRange.after?.epochSecond,
            userId = filters.customer?.userId,
            resourceIds = resourceIdsKeySet.toList(),
            resourceIdsSize = resourceIdsKeySet.size,
            attendanceStatus = filters.attendanceStatus.value?.key,
            excludedBookingStatuses = excludedBookingStatusKeySet.toList(),
            productIds = productIds,
            productIdsSize = productIds.size,
            keepIds = keepIds,
            keepIdsSize = keepIds.size
        )
    }

    /**
     * Delete Booking entities that match the filters but are not present in the new list,
     * and then insert the new entities.
     */
    @Transaction
    suspend fun cleanAndUpsertBookings(
        localSiteId: LocalId,
        filters: BookingFilters,
        entities: List<BookingEntity>,
    ) {
        deleteStaleBookings(
            localSiteId = localSiteId,
            filters = filters,
            keepIds = entities.map { it.id.value },
        )
        upsert(entities)
    }

    fun observeBookings(
        localSiteId: LocalId,
        limit: Int? = null,
        filters: BookingFilters? = null,
        order: BookingsOrderOption
    ): Flow<List<BookingEntity>> {
        val resourceIdsKeySet = filters?.teamMembers?.values?.map { it.value }.orEmpty()
        val excludedBookingStatusKeySet = filters?.excludedBookingStatuses?.values?.map { it.key }.orEmpty()
        val productIds = filters?.serviceEvents?.values?.map { it.productId }.orEmpty()
        return observeBookings(
            localSiteId = localSiteId,
            limit = limit,
            startDateBefore = filters?.dateRange?.before?.epochSecond,
            startDateAfter = filters?.dateRange?.after?.epochSecond,
            userId = filters?.customer?.userId,
            resourceIds = resourceIdsKeySet.toList(),
            resourceIdsSize = resourceIdsKeySet.size,
            attendanceStatus = filters?.attendanceStatus?.value?.key,
            excludedBookingStatuses = excludedBookingStatusKeySet.toList(),
            productIds = productIds,
            productIdsSize = productIds.size,
            order = order
        )
    }

    suspend fun getBookings(
        localSiteId: LocalId,
        limit: Int? = null,
        filters: BookingFilters? = null,
        order: BookingsOrderOption
    ): List<BookingEntity> {
        val resourceIdsKeySet = filters?.teamMembers?.values?.map { it.value }.orEmpty()
        val excludedBookingStatusKeySet = filters?.excludedBookingStatuses?.values?.map { it.key }.orEmpty()
        val productIds = filters?.serviceEvents?.values?.map { it.productId }.orEmpty()
        return getBookings(
            localSiteId = localSiteId,
            limit = limit,
            startDateBefore = filters?.dateRange?.before?.epochSecond,
            startDateAfter = filters?.dateRange?.after?.epochSecond,
            userId = filters?.customer?.userId,
            resourceIds = resourceIdsKeySet.toList(),
            resourceIdsSize = resourceIdsKeySet.size,
            attendanceStatus = filters?.attendanceStatus?.value?.key,
            excludedBookingStatuses = excludedBookingStatusKeySet.toList(),
            productIds = productIds,
            productIdsSize = productIds.size,
            order = order
        )
    }

    @Query("SELECT * FROM Bookings WHERE localSiteId = :localSiteId AND id = :bookingId LIMIT 1")
    suspend fun getBooking(localSiteId: LocalId, bookingId: Long): BookingEntity?

    @Query("SELECT * FROM Bookings WHERE localSiteId = :localSiteId AND id IN (:bookingIds)")
    suspend fun getBookingsByIds(localSiteId: LocalId, bookingIds: List<Long>): List<BookingEntity>

    @Query("UPDATE Bookings SET location = :location WHERE localSiteId = :localSiteId AND id = :bookingId")
    suspend fun updateLocation(localSiteId: LocalId, bookingId: Long, location: String?)

    @Query("SELECT * FROM BookingResources WHERE localSiteId = :localSiteId AND id = :resourceId")
    fun observeResource(localSiteId: LocalId, resourceId: Long): Flow<BookingResourceEntity?>

    @Query("SELECT * FROM BookingResources WHERE localSiteId = :localSiteId")
    fun observeResources(localSiteId: LocalId): Flow<List<BookingResourceEntity>>

    @Query("SELECT * FROM BookingResources WHERE localSiteId = :localSiteId")
    suspend fun getResources(localSiteId: LocalId): List<BookingResourceEntity>

    @Query("SELECT * FROM BookingResources WHERE localSiteId = :localSiteId AND  id = :resourceId")
    suspend fun getResource(localSiteId: LocalId, resourceId: Long): BookingResourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(resource: BookingResourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResources(resources: List<BookingResourceEntity>)

    @Query("DELETE FROM BookingResources WHERE localSiteId = :localSiteId")
    suspend fun deleteAllResourcesForSite(localSiteId: LocalId)

    @Transaction
    suspend fun replaceAllResourcesForSite(siteId: LocalId, resources: List<BookingResourceEntity>) {
        deleteAllResourcesForSite(siteId)
        upsertResources(resources)
    }
}
