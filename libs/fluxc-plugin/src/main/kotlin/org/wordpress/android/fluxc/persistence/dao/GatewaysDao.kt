package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.GatewayEntity

@Dao
abstract class GatewaysDao {
    @Query(
        """
        SELECT * FROM GatewayEntity
        WHERE siteId = :siteId
        """
    )
    abstract suspend fun getGatewaysForSite(siteId: LocalId): List<GatewayEntity>

    @Query(
        """
        SELECT * FROM GatewayEntity
        WHERE siteId = :siteId
        AND gatewayId = :gatewayId
        LIMIT 1
        """
    )
    abstract suspend fun getGateway(siteId: LocalId, gatewayId: String): GatewayEntity?

    @Transaction
    open suspend fun replaceAllForSite(siteId: LocalId, gateways: List<GatewayEntity>) {
        deleteForSite(siteId)
        if (gateways.isNotEmpty()) upsertGateways(gateways)
    }

    @Query(
        """
        DELETE FROM GatewayEntity
        WHERE siteId = :siteId
        """
    )
    protected abstract suspend fun deleteForSite(siteId: LocalId)

    @Upsert
    abstract suspend fun upsertGateways(gateways: List<GatewayEntity>)

    @Upsert
    abstract suspend fun upsertGateway(gateway: GatewayEntity)
}
