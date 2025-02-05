package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverviewComposedEntities
import org.wordpress.android.fluxc.persistence.entity.BalanceType
import org.wordpress.android.fluxc.persistence.entity.DepositType
import org.wordpress.android.fluxc.persistence.entity.DepositType.LAST_PAID
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsBalanceEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsDepositsOverviewEntity
import org.wordpress.android.fluxc.persistence.entity.WooPaymentsManualDepositEntity

@Dao
interface WooPaymentsDepositsOverviewDao {
    @Transaction
    suspend fun getOverviewComposed(localSiteId: LocalId): WooPaymentsDepositsOverviewComposedEntities? {
        val overview = getWooPaymentsDepositsOverviewEntity(localSiteId) ?: return null
        return WooPaymentsDepositsOverviewComposedEntities(
            overview = overview,

            lastPaidDeposits = getDeposits(localSiteId, LAST_PAID),
            lastManualDeposits = getManualDeposits(localSiteId),

            pendingBalances = getBalances(localSiteId, BalanceType.PENDING),
            availableBalances = getBalances(localSiteId, BalanceType.AVAILABLE),
            instantBalances = getBalances(localSiteId, BalanceType.INSTANT)
        )
    }

    @Query("SELECT * FROM WooPaymentsDepositsOverview WHERE localSiteId = :localSiteId")
    suspend fun getWooPaymentsDepositsOverviewEntity(localSiteId: LocalId): WooPaymentsDepositsOverviewEntity?

    @Query(
        """
        SELECT * FROM WooPaymentsDeposits 
        WHERE localSiteId = :localSiteId AND depositType = :type
    """
    )
    fun getDeposits(
        localSiteId: LocalId,
        type: DepositType
    ): List<WooPaymentsDepositEntity>

    @Query(
        """
        SELECT * FROM WooPaymentsManualDeposits 
        WHERE localSiteId = :localSiteId
    """
    )
    fun getManualDeposits(localSiteId: LocalId): List<WooPaymentsManualDepositEntity>

    @Query(
        """
        SELECT * FROM WooPaymentsBalance
        WHERE localSiteId = :localSiteId AND balanceType = :balanceType
    """
    )
    fun getBalances(
        localSiteId: LocalId,
        balanceType: BalanceType,
    ): List<WooPaymentsBalanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverview(overview: WooPaymentsDepositsOverviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: WooPaymentsDepositEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManualDeposit(manualDeposit: WooPaymentsManualDepositEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalance(balance: WooPaymentsBalanceEntity)

    @Transaction
    @Suppress("LongParameterList")
    suspend fun insertOverviewAll(
        localSiteId: LocalId,
        overviewEntity: WooPaymentsDepositsOverviewEntity,
        lastPaidDepositsEntities: List<WooPaymentsDepositEntity>,
        manualDepositEntities: List<WooPaymentsManualDepositEntity>,
        pendingBalancesEntities: List<WooPaymentsBalanceEntity>,
        availableBalancesEntities: List<WooPaymentsBalanceEntity>,
        instantBalancesEntities: List<WooPaymentsBalanceEntity>
    ) {
        insertOverview(overviewEntity)

        lastPaidDepositsEntities.forEach { insertDeposit(it) }
        manualDepositEntities.forEach { insertManualDeposit(it) }

        pendingBalancesEntities.forEach { insertBalance(it) }
        availableBalancesEntities.forEach { insertBalance(it) }
        instantBalancesEntities.forEach { insertBalance(it) }
    }

    @Query("DELETE FROM WooPaymentsDepositsOverview WHERE localSiteId = :localSiteId")
    suspend fun delete(localSiteId: LocalId)
}

