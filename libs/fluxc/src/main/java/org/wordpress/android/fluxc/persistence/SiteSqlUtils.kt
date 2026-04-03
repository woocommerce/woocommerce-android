package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.SiteModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteSqlUtils @Inject constructor() {
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
}
