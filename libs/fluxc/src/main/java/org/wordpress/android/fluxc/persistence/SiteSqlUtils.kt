package org.wordpress.android.fluxc.persistence

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import com.wellsql.generated.SiteModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.DB
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteSqlUtils @Inject constructor(
    private val accountStorePersistence: AccountStorePersistence,
) {
    object DuplicateSiteException : Exception() {
        private const val serialVersionUID = -224883903136726226L
    }

    fun getSiteWithLocalId(id: LocalId): SiteModel? = WellSql.select(SiteModel::class.java)
            .where()
            .equals(SiteModelTable.ID, id.value)
            .endWhere()
            .asModel
            .firstOrNull()

    fun getSitesWithLocalId(id: Int): List<SiteModel> {
        return WellSql.select(SiteModel::class.java)
                .where().equals(SiteModelTable.ID, id).endWhere().asModel
    }

    fun getSitesWithRemoteId(id: Long): List<SiteModel> {
        return WellSql.select(SiteModel::class.java)
                .where().equals(SiteModelTable.SITE_ID, id).endWhere().asModel
    }

    fun getSitesWith(field: String?, value: Boolean): SelectQuery<SiteModel> {
        return WellSql.select(SiteModel::class.java)
                .where().equals(field, value).endWhere()
    }

    fun getSitesAccessedViaWPComRestByNameOrUrlMatching(searchString: String?): List<SiteModel> {
        // Note: by default SQLite "LIKE" operator is case insensitive, and that's what we're looking for.
        return WellSql.select(SiteModel::class.java).where() // ORIGIN = ORIGIN_WPCOM_REST AND (x in url OR x in name)
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_WPCOM_REST)
                .beginGroup()
                .contains(SiteModelTable.URL, searchString)
                .or().contains(SiteModelTable.NAME, searchString)
                .endGroup().endWhere().asModel
    }

    fun getSitesByNameOrUrlMatching(searchString: String?): List<SiteModel> {
        return WellSql.select(SiteModel::class.java).where()
                .contains(SiteModelTable.URL, searchString)
                .or().contains(SiteModelTable.NAME, searchString)
                .endWhere().asModel
    }

    fun getSites(): List<SiteModel> = WellSql.select(SiteModel::class.java).asModel

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

    fun deleteSite(site: SiteModel?): Int {
        return if (site == null) {
            0
        } else WellSql.delete(SiteModel::class.java)
                .where().equals(SiteModelTable.ID, site.id).endWhere()
                .execute()
    }

    fun deleteAllSites(): Int {
        return WellSql.delete(SiteModel::class.java).execute()
    }

    fun setSiteVisibility(site: SiteModel?, visible: Boolean): Int {
        return if (site == null) {
            0
        } else WellSql.update(SiteModel::class.java)
                .whereId(site.id)
                .where().equals(SiteModelTable.IS_WPCOM, true).endWhere()
                .put(visible, { item ->
                    val cv = ContentValues()
                    cv.put(SiteModelTable.IS_VISIBLE, item)
                    cv
                }).execute()
    }

    /**
     * @return A selectQuery to get all the sites accessed via the XMLRPC, this includes: pure self hosted sites,
     * but also Jetpack sites connected via XMLRPC.
     */
    val sitesAccessedViaXMLRPC: SelectQuery<SiteModel>
        get() = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_XMLRPC)
                .endGroup().endWhere()
    val sitesAccessedViaWPComRest: SelectQuery<SiteModel>
        get() = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_WPCOM_REST)
                .endGroup().endWhere()

    /**
     * Removes all sites from local database with the following criteria:
     * 1. Site is a WP.com -or- Jetpack connected site
     * 2. Site has no local-only data (posts/pages/drafts)
     * 3. Remote site ID does not match a site ID found in given sites list
     *
     * @param sites
     * list of sites to keep in local database
     */
    @Suppress("NestedBlockDepth")
    fun removeWPComRestSitesAbsentFromList(sites: List<SiteModel>): Int {
        // get all local WP.com+Jetpack sites
        val localSites = WellSql.select(SiteModel::class.java)
                .where()
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_WPCOM_REST)
                .endWhere().asModel
        if (localSites.size > 0) {
            // iterate through all local WP.com+Jetpack sites
            val localIterator = localSites.iterator()
            while (localIterator.hasNext()) {
                val localSite = localIterator.next()

                // don't remove local site if the remote ID matches a given site's ID
                for (site in sites) {
                    if (site.siteId == localSite.siteId) {
                        localIterator.remove()
                        break
                    }
                }
            }

            // delete applicable sites
            for (site in localSites) {
                deleteSite(site)
            }
        }
        return localSites.size
    }

    fun isWPComSiteVisibleByLocalId(id: Int): Boolean {
        return WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .equals(SiteModelTable.IS_WPCOM, true)
                .equals(SiteModelTable.IS_VISIBLE, true)
                .endGroup().endWhere()
                .exists()
    }

    /**
     * Given a (remote) site id, returns the corresponding (local) id.
     */
    fun getLocalIdForRemoteSiteId(siteId: Long): Int {
        val sites = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.SITE_ID, siteId)
                .or()
                .equals(SiteModelTable.SELF_HOSTED_SITE_ID, siteId)
                .endGroup().endWhere()
                .getAsModel(this::toSiteModel)
        return if (sites.size > 0) {
            sites[0].id
        } else 0
    }

    private fun toSiteModel(cursor: Cursor): SiteModel {
        val siteModel = SiteModel()
        siteModel.id = cursor.getInt(cursor.getColumnIndexOrThrow(SiteModelTable.ID))
        return siteModel
    }

    /**
     * Given a (remote) self-hosted site id and XML-RPC url, returns the corresponding (local) id.
     */
    fun getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(selfHostedSiteId: Long, xmlRpcUrl: String?): Int {
        val sites = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.SELF_HOSTED_SITE_ID, selfHostedSiteId)
                .equals(SiteModelTable.XMLRPC_URL, xmlRpcUrl)
                .endGroup().endWhere()
                .getAsModel(this::toSiteModel)
        return if (sites.size > 0) {
            sites[0].id
        } else 0
    }

    /**
     * Given a (local) id, returns the (remote) site id. Searches first for .COM and Jetpack, then looks for self-hosted
     * sites.
     */
    fun getSiteIdForLocalId(id: Int): Long {
        val result = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .endGroup().endWhere()
                .getAsModel { cursor ->
                    val siteModel = SiteModel()
                    siteModel.siteId = cursor.getInt(
                            cursor.getColumnIndexOrThrow(SiteModelTable.SITE_ID)
                    ).toLong()
                    siteModel.selfHostedSiteId = cursor.getLong(
                            cursor.getColumnIndexOrThrow(SiteModelTable.SELF_HOSTED_SITE_ID)
                    )
                    siteModel
                }
        if (result.isEmpty()) {
            return 0
        }
        return if (result[0].siteId > 0) {
            result[0].siteId
        } else {
            result[0].selfHostedSiteId
        }
    }
}
