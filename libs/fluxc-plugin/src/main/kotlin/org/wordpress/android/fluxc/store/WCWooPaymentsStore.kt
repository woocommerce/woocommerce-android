package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsRestClient
import org.wordpress.android.fluxc.persistence.dao.WooPaymentsDepositsOverviewDao
import org.wordpress.android.fluxc.persistence.entity.BalanceType
import org.wordpress.android.fluxc.persistence.entity.DepositType
import org.wordpress.android.fluxc.persistence.mappers.WooPaymentsDepositsOverviewMapper
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCWooPaymentsStore @Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val restClient: WooPaymentsRestClient,
    private val dao: WooPaymentsDepositsOverviewDao,
    private val mapper: WooPaymentsDepositsOverviewMapper
) {
    suspend fun fetchDepositsOverview(site: SiteModel): WooPayload<WooPaymentsDepositsOverview> =
        coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchDepositsOverview") {
            val result = restClient.fetchDepositsOverview(site)
            if (result.result != null) {
                WooPayload(mapper.mapApiResponseToModel(result.result))
            } else {
                WooPayload(result.error)
            }
        }

    suspend fun getDepositsOverviewAll(site: SiteModel): WooPaymentsDepositsOverview? =
        coroutineEngine.withDefaultContext(AppLog.T.API, this, "getDepositsOverviewAll") {
             dao.getOverviewComposed(site.localId()).let {
                 mapper.mapEntityToModel(it)
             }
        }

    suspend fun insertDepositsOverview(
        site: SiteModel,
        depositsOverview: WooPaymentsDepositsOverview
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "insertDepositsOverview") {
        dao.insertOverviewAll(
            localSiteId = site.localId(),
            overviewEntity = mapper.mapModelToEntity(depositsOverview, site),

            lastPaidDepositsEntities = depositsOverview.deposit?.lastPaid?.map {
                mapper.mapModelDepositToEntity(it, site, DepositType.LAST_PAID)
            }.orEmpty(),
            manualDepositEntities = depositsOverview.deposit?.lastManualDeposits?.map {
                mapper.mapModelManualDepositToEntity(it, site)
            }.orEmpty(),

            pendingBalancesEntities = depositsOverview.balance?.pending?.map {
                mapper.mapModelBalanceToEntity(it, site, BalanceType.PENDING)
            }.orEmpty(),
            availableBalancesEntities = depositsOverview.balance?.available?.map {
                mapper.mapModelBalanceToEntity(it, site, BalanceType.AVAILABLE)
            }.orEmpty(),
            instantBalancesEntities = depositsOverview.balance?.instant?.map {
                mapper.mapModelBalanceToEntity(it, site, BalanceType.INSTANT)
            }.orEmpty()
        )
    }

    suspend fun deleteDepositsOverview(site: SiteModel) =
        coroutineEngine.withDefaultContext(AppLog.T.API, this, "deleteDepositsOverview") {
            dao.delete(site.localId())
        }
}
