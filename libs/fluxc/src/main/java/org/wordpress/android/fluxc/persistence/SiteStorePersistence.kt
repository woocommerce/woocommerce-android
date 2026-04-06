package org.wordpress.android.fluxc.persistence

import android.database.sqlite.SQLiteConstraintException
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.dao.SiteDao
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteStorePersistence @Inject constructor(
    private val siteDao: SiteDao,
    private val siteMapper: SiteMapper,
    private val accountStorePersistence: AccountStorePersistence,
) {
    class DuplicateSiteException : Exception()

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
     *
     * **Return value:** `1` on success (both insert and update), `0` if
     * the WP.com account validation fails. The return value does not distinguish between
     * insert and update — callers like [SiteStore.createOrUpdateSites] use it only as a
     * success/failure indicator.
     *
     * **Side effect:** mutates [site]`.id` to the local ID assigned by the DB (on insert) or
     * to the existing row's local ID (on update). This is required because callers such as
     * [SiteStore.createOrUpdateSites] add the same [site] object to the `updatedSites` list
     * after this call, and downstream consumers expect `site.id` to carry the correct local ID.
     */
    @Suppress("LongMethod", "ComplexMethod", "ReturnCount")
    @Throws(DuplicateSiteException::class)
    suspend fun insertOrUpdateSite(site: SiteModel): Int {
        // If we're inserting or updating a WP.com REST API site, validate that we actually have a WordPress.com
        // account present
        // This prevents a late UPDATE_SITES action from re-populating the database after sign out from WordPress.com
        if (site.isUsingWpComRestApi) {
            val account = accountStorePersistence.getDefaultAccount()
            if (account == null || account.userId == 0L) {
                AppLog.w(AppLog.T.DB, "Can't insert WP.com site " + site.url + ", missing user account")
                return 0
            }
        }

        // If the site already exist and has an id, we want to update it.
        var existingSite = if (site.id > 0) siteDao.getByLocalId(site.id) else null
        if (existingSite != null) {
            AppLog.d(AppLog.T.DB, "Site found by (local) ID: " + site.id)
        }

        // Looks like a new site, make sure we don't already have it.
        if (existingSite == null) {
            if (site.siteId > 0) {
                // For WordPress.com and Jetpack sites, the WP.com ID is a unique enough identifier
                val siteResult = siteDao.getByRemoteId(site.siteId).firstOrNull()
                if (siteResult != null) {
                    existingSite = siteResult
                    AppLog.d(AppLog.T.DB, "Site found by SITE_ID: " + site.siteId)
                }
            } else {
                val siteResult = siteDao.getBySiteIdAndUrl(site.siteId, site.url).firstOrNull()
                if (siteResult != null) {
                    existingSite = siteResult
                    AppLog.d(AppLog.T.DB, "Site found by SITE_ID: " + site.siteId + " and URL: " + site.url)
                }
            }
        }

        // If the site is a self hosted, maybe it's already in the DB as a Jetpack site, and we don't want to create
        // a duplicate.
        if (existingSite == null) {
            val forcedHttpXmlRpcUrl = "http://" + UrlUtils.removeScheme(site.xmlRpcUrl)
            val forcedHttpsXmlRpcUrl = "https://" + UrlUtils.removeScheme(site.xmlRpcUrl)
            val siteResult = siteDao.getByXmlRpcUrl(forcedHttpXmlRpcUrl, forcedHttpsXmlRpcUrl).firstOrNull()
            if (siteResult != null) {
                AppLog.d(AppLog.T.DB, "Site found using XML-RPC url: " + site.xmlRpcUrl)
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
                if (siteResult.origin == SiteModel.ORIGIN_WPCOM_REST && site.origin == SiteModel.ORIGIN_WPCOM_REST) {
                    AppLog.d(
                        AppLog.T.DB,
                        "Duplicate WPCom sites with same URLs, it could be an Identity Crisis, insert both sites"
                    )
                } else if (siteResult.origin == SiteModel.ORIGIN_WPCOM_REST) {
                    AppLog.d(AppLog.T.DB, "Site is a duplicate")
                    throw DuplicateSiteException()
                } else {
                    existingSite = siteResult
                }
            }
        }
        return if (existingSite == null) {
            // No site with this local ID, REMOTE_ID + URL, or XMLRPC URL, then insert it
            AppLog.d(AppLog.T.DB, "Inserting site: " + site.url)
            val entity = siteMapper.toEntity(site)
            val newId = siteDao.insert(entity)
            site.id = newId.toInt()
            1
        } else {
            // Update old site
            AppLog.d(AppLog.T.DB, "Updating site: " + site.url)
            val entity = siteMapper.toEntity(site).copy(id = existingSite.id)
            try {
                siteDao.update(entity)
                site.id = existingSite.id
                1
            } catch (e: SQLiteConstraintException) {
                AppLog.e(
                    AppLog.T.DB,
                    "Error while updating site: siteId=${site.siteId} url=${site.url} " +
                        "xmlrpc=${site.xmlRpcUrl}",
                    e
                )
                throw DuplicateSiteException()
            }
        }
    }
}
