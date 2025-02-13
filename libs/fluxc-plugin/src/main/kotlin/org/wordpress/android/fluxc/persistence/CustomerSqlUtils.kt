package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCCustomerModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerModel

object CustomerSqlUtils {
    // region get
    fun getCustomerByRemoteId(site: SiteModel, remoteCustomerId: Long): WCCustomerModel? {
        return WellSql.select(WCCustomerModel::class.java)
                .where()
                .beginGroup()
                .equals(WCCustomerModelTable.REMOTE_CUSTOMER_ID, remoteCustomerId)
                .equals(WCCustomerModelTable.LOCAL_SITE_ID, site.id)
                .endGroup()
                .endWhere()
                .asModel.firstOrNull()
    }

    fun getCustomersForSite(site: SiteModel): List<WCCustomerModel> {
        return WellSql.select(WCCustomerModel::class.java)
                .where()
                .equals(WCCustomerModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel
    }

    fun getCustomerByRemoteIds(site: SiteModel, remoteCustomerId: List<Long>): List<WCCustomerModel> {
        return WellSql.select(WCCustomerModel::class.java)
                .where().beginGroup()
                .isIn(WCCustomerModelTable.REMOTE_CUSTOMER_ID, remoteCustomerId)
                .equals(WCCustomerModelTable.LOCAL_SITE_ID, site.id)
                .endGroup().endWhere()
                .asModel
    }
    // endregion

    // region insert-update
    fun insertOrUpdateCustomer(customer: WCCustomerModel): Int {
        val customerResult = WellSql.select(WCCustomerModel::class.java)
                .where().beginGroup()
                .equals(WCCustomerModelTable.ID, customer.id)
                .or()
                .beginGroup()
                .equals(WCCustomerModelTable.REMOTE_CUSTOMER_ID, customer.remoteCustomerId)
                .equals(WCCustomerModelTable.LOCAL_SITE_ID, customer.localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return if (customerResult == null) {
            // Insert
            WellSql.insert(customer).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            WellSql.update(WCCustomerModel::class.java)
                    .where().beginGroup()
                    .equals(WCCustomerModelTable.REMOTE_CUSTOMER_ID, customerResult.remoteCustomerId)
                    .equals(WCCustomerModelTable.LOCAL_SITE_ID, customerResult.localSiteId)
                    .endGroup().endWhere()
                    .put(customer, UpdateAllExceptId(WCCustomerModel::class.java)).execute()
        }
    }

    fun insertOrUpdateCustomers(customers: List<WCCustomerModel>): Int {
        return customers.sumOf { insertOrUpdateCustomer(it) }
    }

    fun insertCustomers(customers: List<WCCustomerModel>) {
        customers.forEach {
            WellSql.insert(it).asSingleTransaction(true).execute()
        }
    }
    // endregion

    // region delete
    fun deleteCustomersForSite(site: SiteModel): Int {
        return WellSql.delete(WCCustomerModel::class.java)
                .where().beginGroup()
                .equals(WCCustomerModelTable.LOCAL_SITE_ID, site.id)
                .endGroup()
                .endWhere()
                .execute()
    }
    // endregion
}
