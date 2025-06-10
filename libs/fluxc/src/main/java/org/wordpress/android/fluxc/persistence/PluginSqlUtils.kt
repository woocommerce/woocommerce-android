package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.SitePluginModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel

object PluginSqlUtils {
    @JvmStatic
    fun getSitePlugins(site: SiteModel): List<SitePluginModel> =
        WellSql.select(SitePluginModel::class.java)
            .where()
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .orderBy(SitePluginModelTable.DISPLAY_NAME, SelectQuery.ORDER_ASCENDING)
            .asModel

    @JvmStatic
    fun insertOrReplaceSitePlugins(site: SiteModel, plugins: List<SitePluginModel>) {
        // Remove previous plugins for this site
        removeSitePlugins(site)
        // Insert new plugins for this site
        for (sitePluginModel in plugins) {
            sitePluginModel.localSiteId = site.id
        }
        WellSql.insert(plugins).asSingleTransaction(true).execute()
    }

    private fun removeSitePlugins(site: SiteModel) =
        WellSql.delete(SitePluginModel::class.java)
            .where()
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .endWhere().execute()

    @JvmStatic
    fun insertOrUpdateSitePlugin(site: SiteModel, plugin: SitePluginModel?): Int {
        if (plugin == null) {
            return 0
        }
        val oldPlugin = getSitePluginBySlug(site, plugin.slug)
        // Make sure the site id is set (if the plugin is retrieved from network)
        plugin.localSiteId = site.id
        return if (oldPlugin == null) {
            WellSql.insert(plugin).execute()
            1
        } else {
            val oldId = oldPlugin.id
            WellSql.update(SitePluginModel::class.java)
                .whereId(oldId)
                .put(plugin, UpdateAllExceptId(SitePluginModel::class.java))
                .execute()
        }
    }

    @JvmStatic
    fun getSitePluginBySlug(site: SiteModel, slug: String?): SitePluginModel? {
        val result = WellSql.select(SitePluginModel::class.java).where()
            .equals(SitePluginModelTable.SLUG, slug)
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .asModel
        return if (result.isEmpty()) null else result.first()
    }

    @JvmStatic
    fun getSitePluginByName(site: SiteModel, pluginName: String?): SitePluginModel? {
        val result = WellSql.select(SitePluginModel::class.java)
            .where()
            .equals(SitePluginModelTable.NAME, pluginName)
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .asModel
        return if (result.isEmpty()) null else result.first()
    }

    @JvmStatic
    fun getSitePluginByNames(site: SiteModel, pluginNames: List<String?>?): List<SitePluginModel> =
        WellSql.select(SitePluginModel::class.java)
            .where()
            .isIn(SitePluginModelTable.NAME, pluginNames)
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .asModel
}
