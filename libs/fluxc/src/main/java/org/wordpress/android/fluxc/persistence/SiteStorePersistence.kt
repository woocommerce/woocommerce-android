package org.wordpress.android.fluxc.persistence

import android.database.sqlite.SQLiteConstraintException
import com.wellsql.generated.SiteModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.DB
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteStorePersistence @Inject constructor(
    private val accountStorePersistence: AccountStorePersistence,
) {
    object DuplicateSiteException : Exception() {
        private const val serialVersionUID = -224883903136726226L
    }

    /**
     * Inserts the given SiteModel into the DB, or updates an existing entry where sites match.
     *
     * Possible cases:
     * 1. Exists in the DB already and matches by local id (simple update) -> UPDATE
     * 2. Exists in the DB, is a Jetpack or WordPress site and matches by remote id (SITE_ID) -> UPDATE
     * 3. Exists in the DB, is a pure self hosted and matches by remote id (SITE_ID) + URL -> UPDATE
     * 4. Exists in the DB, originally a WP.com REST site, and matches by XMLRPC_URL -> THROW a DuplicateSiteException
     * 5. Exists in the DB, originally an XML-RPC site, and matches by XMLRPC_URL -> UPDATE
     * 6. Not matching any previous cases -> INSERT
     */
    @Suppress("LongMethod", "ComplexMethod")
    @Throws(DuplicateSiteException::class)
    fun insertOrUpdateSite(site: SiteModel?): Int {
        if (site == null) {
            return 0
        }

        // If we're inserting or updating a WP.com REST API site, validate that we actually have a WordPress.com
        // account present
        // This prevents a late UPDATE_SITES action from re-populating the database after sign out from WordPress.com
        if (site.isUsingWpComRestApi) {
            val account = accountStorePersistence.getDefaultAccount()
            if (account == null || account.userId == 0L) {
                AppLog.w(DB, "Can't insert WP.com site " + site.url + ", missing user account")
                return 0
            }
        }

        // If the site already exist and has an id, we want to update it.
        var siteResult = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.ID, site.id)
                .endGroup().endWhere().asModel
        if (!siteResult.isEmpty()) {
            AppLog.d(DB, "Site found by (local) ID: " + site.id)
        }

        // Looks like a new site, make sure we don't already have it.
        if (siteResult.isEmpty()) {
            if (site.siteId > 0) {
                // For WordPress.com and Jetpack sites, the WP.com ID is a unique enough identifier
                siteResult = WellSql.select(SiteModel::class.java)
                        .where().beginGroup()
                        .equals(SiteModelTable.SITE_ID, site.siteId)
                        .endGroup().endWhere().asModel
                if (!siteResult.isEmpty()) {
                    AppLog.d(DB, "Site found by SITE_ID: " + site.siteId)
                }
            } else {
                siteResult = WellSql.select(SiteModel::class.java)
                        .where().beginGroup()
                        .equals(SiteModelTable.SITE_ID, site.siteId)
                        .equals(SiteModelTable.URL, site.url)
                        .endGroup().endWhere().asModel
                if (!siteResult.isEmpty()) {
                    AppLog.d(DB, "Site found by SITE_ID: " + site.siteId + " and URL: " + site.url)
                }
            }
        }

        // If the site is a self hosted, maybe it's already in the DB as a Jetpack site, and we don't want to create
        // a duplicate.
        if (siteResult.isEmpty()) {
            val forcedHttpXmlRpcUrl = "http://" + UrlUtils.removeScheme(site.xmlRpcUrl)
            val forcedHttpsXmlRpcUrl = "https://" + UrlUtils.removeScheme(site.xmlRpcUrl)
            siteResult = WellSql.select(SiteModel::class.java)
                    .where()
                    .beginGroup()
                    .equals(SiteModelTable.XMLRPC_URL, forcedHttpXmlRpcUrl)
                    .or().equals(SiteModelTable.XMLRPC_URL, forcedHttpsXmlRpcUrl)
                    .endGroup()
                    .endWhere()
                    .asModel
            if (siteResult.isNotEmpty()) {
                AppLog.d(DB, "Site found using XML-RPC url: " + site.xmlRpcUrl)
                // Four possibilities here:
                // 1. DB site is WP.com, new site is WP.com with different siteIds:
                // The site could be having an "Identity Crisis", while this should be fixed on the site itself,
                // it shouldn't block sign-in -> proceed
                // 2. DB site is WP.com, new site is XML-RPC:
                // It looks like an existing Jetpack-connected site over the REST API was added again as an XML-RPC
                // Wed don't allow this --> DuplicateSiteException
                // 3. DB site is XML-RPC, new site is WP.com:
                // Upgrading a self-hosted site to Jetpack --> proceed
                // 4. DB site is XML-RPC, new site is XML-RPC:
                // An existing self-hosted site was logged-into again, and we couldn't identify it by URL or
                // by WP.com site ID + URL --> proceed
                if (siteResult[0].origin == SiteModel.ORIGIN_WPCOM_REST && site.origin == SiteModel.ORIGIN_WPCOM_REST) {
                    AppLog.d(
                        DB,
                        "Duplicate WPCom sites with same URLs, it could be an Identity Crisis, insert both sites"
                    )
                    siteResult = emptyList()
                } else if (siteResult[0].origin == SiteModel.ORIGIN_WPCOM_REST) {
                    AppLog.d(DB, "Site is a duplicate")
                    throw DuplicateSiteException
                }
            }
        }
        return if (siteResult.isEmpty()) {
            // No site with this local ID, REMOTE_ID + URL, or XMLRPC URL, then insert it
            AppLog.d(DB, "Inserting site: " + site.url)
            WellSql.insert(site).asSingleTransaction(true).execute()
            1
        } else {
            // Update old site
            AppLog.d(DB, "Updating site: " + site.url)
            val oldId = siteResult[0].id
            try {
                WellSql.update(SiteModel::class.java).whereId(oldId)
                        .put(site, UpdateAllExceptId(SiteModel::class.java)).execute()
            } catch (e: SQLiteConstraintException) {
                AppLog.e(
                        DB,
                        "Error while updating site: siteId=${site.siteId} url=${site.url} " +
                                "xmlrpc=${site.xmlRpcUrl}",
                        e
                )
                throw DuplicateSiteException
            }
        }
    }
}
