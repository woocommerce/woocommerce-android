package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.CustomerFromAnalyticsEntity

@Dao
interface CustomerFromAnalyticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomersFromAnalytics(customers: List<CustomerFromAnalyticsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerFromAnalytics(customers: CustomerFromAnalyticsEntity)

    @Query("SELECT * FROM CustomerFromAnalytics WHERE localSiteId = :localSiteId AND id = :analyticCustomerId")
    suspend fun getCustomerByAnalyticCustomerId(
        localSiteId: LocalOrRemoteId.LocalId,
        analyticCustomerId: Long
    ): CustomerFromAnalyticsEntity?
}
