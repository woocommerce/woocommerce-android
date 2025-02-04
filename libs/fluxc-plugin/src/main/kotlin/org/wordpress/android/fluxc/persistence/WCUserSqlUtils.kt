package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCUserModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.user.WCUserModel

object WCUserSqlUtils {
    fun getUsersBySite(
        localSiteId: Int
    ): List<WCUserModel> {
        return WellSql.select(WCUserModel::class.java)
                .where()
                .equals(WCUserModelTable.LOCAL_SITE_ID, localSiteId)
                .endWhere()
                .asModel
    }

    fun getUserBySiteAndEmail(
        localSiteId: Int,
        userEmail: String
    ): WCUserModel? {
        return WellSql.select(WCUserModel::class.java)
                .where()
                .equals(WCUserModelTable.LOCAL_SITE_ID, localSiteId)
                .equals(WCUserModelTable.EMAIL, userEmail)
                .endWhere()
                .asModel.firstOrNull()
    }

    fun insertOrUpdateUser(user: WCUserModel): Int {
        val result = WellSql.select(WCUserModel::class.java)
                .where().beginGroup()
                .equals(WCUserModelTable.ID, user.id)
                .or()
                .beginGroup()
                .equals(WCUserModelTable.LOCAL_SITE_ID, user.localSiteId)
                .equals(WCUserModelTable.EMAIL, user.email)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return if (result == null) {
            // Insert
            WellSql.insert(user).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = result.id
            WellSql.update(WCUserModel::class.java).whereId(oldId)
                    .put(user, UpdateAllExceptId(WCUserModel::class.java)).execute()
        }
    }
}
