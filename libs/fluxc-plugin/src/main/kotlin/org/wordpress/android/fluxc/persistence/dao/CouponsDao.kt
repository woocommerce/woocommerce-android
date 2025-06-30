package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails

@Dao
interface CouponsDao {
    @Transaction
    @Query("SELECT * FROM Coupons WHERE localSiteId = :localSiteId ORDER BY dateCreated DESC")
    fun observeCoupons(localSiteId: LocalId): Flow<List<CouponWithEmails>>

    @Transaction
    @Query("SELECT * FROM Coupons WHERE localSiteId = :localSiteId AND id IN (:couponIds) ORDER BY dateCreated DESC")
    fun observeCoupons(localSiteId: LocalId, couponIds: List<Long>): Flow<List<CouponWithEmails>>

    @Transaction
    @Query("SELECT * FROM Coupons " +
        "WHERE localSiteId = :localSiteId AND id IN (:couponIds) ORDER BY dateCreated DESC"
    )
    suspend fun getCoupons(localSiteId: LocalId, couponIds: List<RemoteId>): List<CouponWithEmails>

    @Transaction
    @Query("SELECT * FROM Coupons WHERE localSiteId = :localSiteId AND id = :couponId")
    fun observeCoupon(localSiteId: LocalId, couponId: RemoteId): Flow<CouponWithEmails?>

    @Transaction
    @Query("SELECT * FROM Coupons WHERE localSiteId = :localSiteId AND id = :couponId")
    suspend fun getCoupon(localSiteId: LocalId, couponId: RemoteId): CouponWithEmails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCoupon(entity: CouponEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCouponEmail(entity: CouponEmailEntity): Long

    @Query("DELETE FROM Coupons WHERE localSiteId = :localSiteId AND id = :couponId")
    suspend fun deleteCoupon(localSiteId: LocalId, couponId: RemoteId)

    @Query("DELETE FROM Coupons WHERE localSiteId = :localSiteId")
    suspend fun deleteAllCoupons(localSiteId: LocalId)

    @Query("SELECT COUNT(*) FROM CouponEmails WHERE localSiteId = :localSiteId AND couponId = :couponId")
    suspend fun getEmailCount(localSiteId: LocalId, couponId: RemoteId): Int
}
