package org.wordpress.android.fluxc.wc.utils

import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.AccountMapper
import org.wordpress.android.fluxc.persistence.AccountStorePersistence
import org.wordpress.android.fluxc.persistence.SiteStorePersistence
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase

object TestSiteSqlUtils {
    private val accountStorePersistence: AccountStorePersistence = mock {
        on { getDefaultAccount() } doReturn AccountModel().apply { userId = 1L }
    }
    val siteStorePersistence = SiteStorePersistence(accountStorePersistence)

    @Suppress("MagicNumber")
    fun insertTestAccountAndSiteIntoDb(
        wpDatabase: WPAndroidDatabase,
        mapper: AccountMapper = AccountMapper()
    ): SiteModel {
        val account = AccountModel().apply { userId = 412 }
        runBlocking { wpDatabase.accountDao().upsert(mapper.toEntity(account)) }

        val accountStorePersistence = AccountStorePersistence(wpDatabase, mapper)
        val site = SiteModel()
        site.siteId = 6347

        SiteStorePersistence(accountStorePersistence).insertOrUpdateSite(site)
        return site
    }
}
