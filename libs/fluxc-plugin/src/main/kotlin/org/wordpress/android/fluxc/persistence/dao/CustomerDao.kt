package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.customer.WCCustomerModel

@Dao
internal abstract class CustomerDao {
    @Query(
        """
        SELECT * FROM CustomerEntity
        WHERE localSiteId = :siteId
        AND remoteCustomerId = :customerId
        """
    )
    abstract suspend fun getCustomerByRemoteId(siteId: LocalId, customerId: RemoteId): WCCustomerModel?

    @Query(
        """
        SELECT * FROM CustomerEntity
        WHERE localSiteId = :siteId
        """
    )
    abstract suspend fun getCustomersForSite(siteId: LocalId): List<WCCustomerModel>

    @Query(
        """
        DELETE FROM CustomerEntity
        WHERE localSiteId = :siteId
        """
    )
    abstract suspend fun deleteCustomersForSite(siteId: LocalId)

    @Upsert
    abstract suspend fun upsertCustomer(customer: WCCustomerModel)

    @Upsert
    abstract suspend fun upsertCustomers(customers: List<WCCustomerModel>)
}
