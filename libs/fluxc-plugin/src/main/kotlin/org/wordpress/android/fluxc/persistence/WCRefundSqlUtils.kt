package org.wordpress.android.fluxc.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.wellsql.generated.WCRefundsTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient.RefundResponse

object WCRefundSqlUtils {
    private val gson: Gson by lazy {
        val builder = GsonBuilder()
        builder.create()
    }

    fun insertOrUpdate(site: SiteModel, orderId: Long, data: RefundResponse) =
            insertOrUpdate(site, orderId, listOf(data))

    fun insertOrUpdate(site: SiteModel, orderId: Long, data: List<RefundResponse>) {
        data.forEach { item ->
            val json = gson.toJson(item)
            WellSql.delete(RefundBuilder::class.java)
                .where()
                .equals(WCRefundsTable.LOCAL_SITE_ID, site.id)
                .equals(WCRefundsTable.ORDER_ID, orderId)
                .equals(WCRefundsTable.REFUND_ID, item.refundId)
                .endWhere()
                .execute()
            WellSql.insert(
                    RefundBuilder(
                            localSiteId = site.id,
                            orderId = orderId,
                            refundId = item.refundId,
                            data = json
                    )
            ).execute()
        }
    }

    fun selectAllRefunds(
        site: SiteModel,
        orderId: Long
    ): List<RefundResponse> {
        val models = WellSql.select(RefundBuilder::class.java)
                .where()
                .equals(WCRefundsTable.LOCAL_SITE_ID, site.id)
                .equals(WCRefundsTable.ORDER_ID, orderId)
                .endWhere()
                .asModel
        return models.map { gson.fromJson(it.data, RefundResponse::class.java) }
    }

    fun selectRefund(
        site: SiteModel,
        orderId: Long,
        refundId: Long
    ): RefundResponse? {
        val model = WellSql.select(RefundBuilder::class.java)
                .where()
                .equals(WCRefundsTable.LOCAL_SITE_ID, site.id)
                .equals(WCRefundsTable.ORDER_ID, orderId)
                .equals(WCRefundsTable.REFUND_ID, refundId)
                .endWhere()
                .asModel
                .firstOrNull()
        return model?.let { gson.fromJson(it.data, RefundResponse::class.java) }
    }

    @Table(name = "WCRefunds")
    data class RefundBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var localSiteId: Int,
        @Column var orderId: Long,
        @Column var refundId: Long,
        @Column var data: String
    ) : Identifiable {
        constructor() : this(-1, -1, -1, -1, "")

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }
}
