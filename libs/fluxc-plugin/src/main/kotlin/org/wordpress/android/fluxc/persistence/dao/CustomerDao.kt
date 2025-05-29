package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.customer.WCCustomerModel

@Dao
interface CustomerDao {
    @Query(
        """
        SELECT * FROM CustomerEntity
        WHERE localSiteId = :localSiteId
        AND remoteCustomerId = :remoteCustomerId
        LIMIT 1
        """
    )
    suspend fun getCustomerByRemoteId(
        localSiteId: LocalOrRemoteId.LocalId,
        remoteCustomerId: Long
    ): WCCustomerModel?

    @Query(
        """
        SELECT * FROM CustomerEntity
        WHERE localSiteId = :localSiteId
        """
    )
    suspend fun getCustomersForSite(
        localSiteId: LocalOrRemoteId.LocalId
    ): List<WCCustomerModel>

    @Query(
        """
        SELECT * FROM CustomerEntity
        WHERE localSiteId = :localSiteId
        AND remoteCustomerId IN (:remoteCustomerIds)
        """
    )
    suspend fun getCustomerByRemoteIds(
        localSiteId: LocalOrRemoteId.LocalId,
        remoteCustomerIds: List<Long>
    ): List<WCCustomerModel>

    @Upsert
    suspend fun upsertCustomer(customer: WCCustomerModel)

    @Upsert
    suspend fun upsertCustomers(customers: List<WCCustomerModel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<WCCustomerModel>)

    @Query(
        """
        DELETE FROM CustomerEntity
        WHERE localSiteId = :localSiteId
        """
    )
    suspend fun deleteCustomersForSite(localSiteId: LocalOrRemoteId.LocalId): Int
}
