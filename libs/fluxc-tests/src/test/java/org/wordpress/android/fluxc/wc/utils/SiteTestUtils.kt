package org.wordpress.android.fluxc.wc.utils

import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.AccountSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils

object SiteTestUtils {
    fun insertTestAccountAndSiteIntoDb(): SiteModel {
        val account = AccountModel().apply { userId = 412 }
        AccountSqlUtils.insertOrUpdateAccount(account, 5124)

        val site = SiteModel()
        site.siteId = 6347

        SiteSqlUtils().insertOrUpdateSite(site)
        return site
    }
}
