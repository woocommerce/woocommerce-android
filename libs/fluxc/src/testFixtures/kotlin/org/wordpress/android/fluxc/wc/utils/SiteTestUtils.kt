package org.wordpress.android.fluxc.wc.utils

import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.AccountMapper
import org.wordpress.android.fluxc.persistence.AccountStorePersistence
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase

@Suppress("MagicNumber")
object SiteTestUtils {
    fun insertTestAccountAndSiteIntoDb(
        wpDatabase: WPAndroidDatabase,
        mapper: AccountMapper = AccountMapper()
    ): SiteModel {
        val account = AccountModel().apply { userId = 412 }
        runBlocking { wpDatabase.accountDao().upsert(mapper.toEntity(account)) }

        val accountStorePersistence = AccountStorePersistence(wpDatabase, mapper)
        val site = SiteModel()
        site.siteId = 6347

        SiteSqlUtils(accountStorePersistence).insertOrUpdateSite(site)
        return site
    }
}
