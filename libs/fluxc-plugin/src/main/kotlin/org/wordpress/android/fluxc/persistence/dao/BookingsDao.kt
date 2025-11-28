package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
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
            AND (:customerId IS NULL OR customerId = :customerId)
            AND ((:attendanceStatusesSize = 0) OR attendanceStatus IN (:attendanceStatuses))
            AND ((:resourceIdsSize = 0) OR resourceId IN (:resourceIds))
            AND ((:productIdsSize = 0) OR productId IN (:productIds))
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
        resourceIds: List<Long>,
        resourceIdsSize: Int,
        attendanceStatuses: List<String>,
        attendanceStatusesSize: Int,
        productIds: List<Long>,
        productIdsSize: Int,
        order: BookingsOrderOption
    ): Flow<List<BookingEntity>>

    @Query("SELECT * FROM Bookings WHERE localSiteId = :localSiteId AND id = :bookingId LIMIT 1")
    fun observeBooking(localSiteId: LocalId, bookingId: Long): Flow<BookingEntity?>

    @Query("SELECT COUNT(*) FROM Bookings WHERE localSiteId = :localSiteId")
    fun observeBookingsCount(localSiteId: LocalId): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: BookingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entities: List<BookingEntity>)

    @Query("DELETE FROM Bookings WHERE localSiteId = :localSiteId")
    suspend fun deleteAllForSite(localSiteId: LocalId)

    @Transaction
    suspend fun replaceAllForSite(siteId: LocalId, entities: List<BookingEntity>) {
        deleteAllForSite(siteId)
        insertOrReplace(entities)
    }

    @Suppress("LongParameterList")
    @Query(
        """
            DELETE FROM Bookings
            WHERE localSiteId = :localSiteId
            AND (:startDateBefore IS NULL OR start <= :startDateBefore)
            AND (:startDateAfter IS NULL OR start >= :startDateAfter)
            AND ((:idsSize = 0) OR id NOT IN (:ids))
        """
    )
    suspend fun deleteForSiteWithDateRangeFilter(
        localSiteId: LocalId,
        startDateBefore: Long?,
        startDateAfter: Long?,
        ids: List<Long>,
        idsSize: Int,
    )

    private suspend fun deleteForSiteWithDateRangeFilter(
        localSiteId: LocalId,
        dateRange: BookingsFilterOption.DateRange,
        ids: List<Long>
    ) {
        deleteForSiteWithDateRangeFilter(
            localSiteId = localSiteId,
            startDateBefore = dateRange.before?.epochSecond,
            startDateAfter = dateRange.after?.epochSecond,
            ids = ids,
            idsSize = ids.size,
        )
    }

    /**
     * Delete Booking entities that are not present in the new list and then insert the new entities
     */
    @Transaction
    suspend fun cleanAndUpsertBookings(
        localSiteId: LocalId,
        dateRange: BookingsFilterOption.DateRange,
        entities: List<BookingEntity>,
    ) {
        deleteForSiteWithDateRangeFilter(
            localSiteId = localSiteId,
            dateRange = dateRange,
            ids = entities.map { it.id.value },
        )
        insertOrReplace(entities)
    }

    fun observeBookings(
        localSiteId: LocalId,
        limit: Int? = null,
        filters: BookingFilters? = null,
        order: BookingsOrderOption
    ): Flow<List<BookingEntity>> {
        val resourceIdsKeySet = filters?.teamMembers?.values?.map { it.value }.orEmpty()
        val attendanceStatusKeySet = filters?.attendanceStatuses?.values?.map { it.key }.orEmpty()
        val productIds = filters?.serviceEvents?.values?.map { it.productId }.orEmpty()
        return observeBookings(
            localSiteId = localSiteId,
            limit = limit,
            startDateBefore = filters?.dateRange?.before?.epochSecond,
            startDateAfter = filters?.dateRange?.after?.epochSecond,
            customerId = filters?.customer?.customerId,
            resourceIds = resourceIdsKeySet.toList(),
            resourceIdsSize = resourceIdsKeySet.size,
            attendanceStatuses = attendanceStatusKeySet.toList(),
            attendanceStatusesSize = attendanceStatusKeySet.size,
            productIds = productIds,
            productIdsSize = productIds.size,
            order = order
        )
    }

    @Query("SELECT * FROM Bookings WHERE localSiteId = :localSiteId AND id = :bookingId LIMIT 1")
    suspend fun getBooking(localSiteId: LocalId, bookingId: Long): BookingEntity?

    @Query("SELECT * FROM BookingResources WHERE localSiteId = :localSiteId AND id = :resourceId")
    fun observeResource(localSiteId: LocalId, resourceId: Long): Flow<BookingResourceEntity?>

    @Query("SELECT * FROM BookingResources WHERE localSiteId = :localSiteId")
    fun observeResources(localSiteId: LocalId): Flow<List<BookingResourceEntity>>

    @Query("SELECT * FROM BookingResources WHERE localSiteId = :localSiteId")
    suspend fun getResources(localSiteId: LocalId): List<BookingResourceEntity>

    @Query("SELECT * FROM BookingResources WHERE localSiteId = :localSiteId AND  id = :resourceId")
    suspend fun getResource(localSiteId: LocalId, resourceId: Long): BookingResourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(resource: BookingResourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceResources(resources: List<BookingResourceEntity>)

    @Query("DELETE FROM BookingResources WHERE localSiteId = :localSiteId")
    suspend fun deleteAllResourcesForSite(localSiteId: LocalId)

    @Transaction
    suspend fun replaceAllResourcesForSite(siteId: LocalId, resources: List<BookingResourceEntity>) {
        deleteAllResourcesForSite(siteId)
        insertOrReplaceResources(resources)
    }
}
