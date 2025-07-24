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

    /**
     * @param pluginName Name of the plugin excluding directory path.
     */
    @JvmStatic
    fun getSitePluginByName(site: SiteModel, pluginName: String?): SitePluginModel? {
        // Get all plugins for the site and filter by plugin name using last '/' segment.
        // 💡 We are purposefully not using the [ConditionClauseBuilder::endsWith] in the SQL query builder below because
        // it's has bug in its implementation 🐛.
        val allPlugins = WellSql.select(SitePluginModel::class.java)
            .where()
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .asModel

        return allPlugins.firstOrNull { plugin ->
            val extractedPluginName = plugin.name.substringAfterLast('/')
            extractedPluginName == pluginName
        }
    }

    /**
     * Get an active site plugin by name.
     * @param pluginName Name of the plugin excluding directory path.
     * @return The active plugin with the specified name, or null if no active plugin is found.
     */
    @JvmStatic
    fun getActiveSitePluginByName(site: SiteModel, pluginName: String?): SitePluginModel? {
        val allPlugins = WellSql.select(SitePluginModel::class.java)
            .where()
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .equals(SitePluginModelTable.IS_ACTIVE, true)
            .endWhere()
            .asModel

        return allPlugins.firstOrNull { plugin ->
            val extractedPluginName = plugin.name.substringAfterLast('/')
            extractedPluginName == pluginName
        }
    }

    /**
     * @param pluginNames Names of the plugin excluding directory path.
     */
    @JvmStatic
    fun getSitePluginByNames(site: SiteModel, pluginNames: List<String?>?): List<SitePluginModel> {
        // Get all plugins for the site and filter by plugin names using last '/' segment
        val allPlugins = WellSql.select(SitePluginModel::class.java)
            .where()
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .asModel

        return allPlugins.filter { plugin ->
            val extractedPluginName = plugin.name.substringAfterLast('/')
            pluginNames?.contains(extractedPluginName) == true
        }
    }
}
