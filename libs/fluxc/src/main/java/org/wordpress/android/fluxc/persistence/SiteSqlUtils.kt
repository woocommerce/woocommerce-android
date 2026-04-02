package org.wordpress.android.fluxc.persistence

import android.content.ContentValues
import android.database.Cursor
import com.wellsql.generated.SiteModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteSqlUtils @Inject constructor() {
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
