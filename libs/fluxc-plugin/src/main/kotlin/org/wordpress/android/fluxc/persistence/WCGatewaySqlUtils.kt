package org.wordpress.android.fluxc.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.wellsql.generated.WCGatewaysTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient.GatewayResponse

object WCGatewaySqlUtils {
    private val gson: Gson by lazy {
        val builder = GsonBuilder()
        builder.create()
    }

    fun insertOrUpdate(site: SiteModel, data: GatewayResponse) =
            insertOrUpdate(site, listOf(data))

    fun insertOrUpdate(site: SiteModel, data: List<GatewayResponse>) {
        data.forEach { item ->
            val json = gson.toJson(item)
            WellSql.delete(GatewaysTable::class.java)
                .where()
                .equals(WCGatewaysTable.LOCAL_SITE_ID, site.id)
                .equals(WCGatewaysTable.GATEWAY_ID, item.gatewayId)
                .endWhere()
                .execute()
            WellSql.insert(
                    GatewaysTable(
                            localSiteId = site.id,
                            gatewayId = item.gatewayId,
                            data = json
                    )
            ).execute()
        }
    }

    fun selectAllGateways(
        site: SiteModel
    ): List<GatewayResponse> {
        val models = WellSql.select(GatewaysTable::class.java)
                .where()
                .equals(WCGatewaysTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel
        return models.map { gson.fromJson(it.data, GatewayResponse::class.java) }
    }

    fun selectGateway(
        site: SiteModel,
        gatewayId: String
    ): GatewayResponse? {
        val model = WellSql.select(GatewaysTable::class.java)
                .where()
                .equals(WCGatewaysTable.LOCAL_SITE_ID, site.id)
                .equals(WCGatewaysTable.GATEWAY_ID, gatewayId)
                .endWhere()
                .asModel
                .firstOrNull()
        return model?.let { gson.fromJson(it.data, GatewayResponse::class.java) }
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
