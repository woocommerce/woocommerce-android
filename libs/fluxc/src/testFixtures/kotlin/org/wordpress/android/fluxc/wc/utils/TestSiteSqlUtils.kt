package org.wordpress.android.fluxc.wc.utils

import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.AccountMapper
import org.wordpress.android.fluxc.persistence.SiteMapper
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase

object TestSiteSqlUtils {
    private val siteMapper = SiteMapper()
    private val accountMapper = AccountMapper()

    @Suppress("MagicNumber")
    fun insertTestAccountAndSiteIntoDb(wpDatabase: WPAndroidDatabase): SiteModel {
        val account = AccountModel().apply { userId = 412 }
        runBlocking { wpDatabase.accountDao().upsert(accountMapper.toEntity(account)) }

        val site = SiteModel().apply { siteId = 6347 }
        runBlocking { wpDatabase.siteDao().insert(siteMapper.toEntity(site)) }
        return site
    }
}
