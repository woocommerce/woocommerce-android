package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCGatewaysTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel

object WCGatewaySqlUtils {
    fun insertOrUpdate(site: SiteModel, data: GatewaysTable) =
            insertOrUpdate(site, listOf(data))

    fun insertOrUpdate(site: SiteModel, data: List<GatewaysTable>) {
        data.forEach { item ->
            WellSql.delete(GatewaysTable::class.java)
                .where()
                .equals(WCGatewaysTable.LOCAL_SITE_ID, site.id)
                .equals(WCGatewaysTable.GATEWAY_ID, item.gatewayId)
                .endWhere()
                .execute()
            WellSql.insert(item).execute()
        }
    }

    fun selectAllGateways(
        site: SiteModel
    ): List<GatewaysTable> {
        val models = WellSql.select(GatewaysTable::class.java)
                .where()
                .equals(WCGatewaysTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel
        return models
    }

    fun selectGateway(
        site: SiteModel,
        gatewayId: String
    ): GatewaysTable? {
        val model = WellSql.select(GatewaysTable::class.java)
                .where()
                .equals(WCGatewaysTable.LOCAL_SITE_ID, site.id)
                .equals(WCGatewaysTable.GATEWAY_ID, gatewayId)
                .endWhere()
                .asModel
                .firstOrNull()
        return model
    }

    @Table(name = "WCGateways")
    data class GatewaysTable(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var localSiteId: Int,
        @Column var gatewayId: String,
        @Column var data: String
    ) : Identifiable {
        constructor() : this(-1, -1, "", "")

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }
}
