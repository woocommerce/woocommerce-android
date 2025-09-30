package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.BookingEntity

@Dao
interface BookingsDao {
    @Query("""SELECT * FROM Bookings WHERE localSiteId = :localSiteId
        ORDER BY dateCreated DESC
        LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END""")
    fun observeBookings(localSiteId: LocalId, limit: Int?): Flow<List<BookingEntity>>

    @Query("SELECT * FROM Bookings WHERE localSiteId = :localSiteId ORDER BY dateCreated DESC")
    suspend fun getBookings(localSiteId: LocalId): List<BookingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: BookingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entities: List<BookingEntity>)

    @Query("DELETE FROM Bookings WHERE localSiteId = :localSiteId")
    suspend fun deleteAllForSite(localSiteId: LocalId)
}
