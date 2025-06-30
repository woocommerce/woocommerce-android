package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import javax.inject.Inject

class PluginSqlUtilsWrapper
@Inject constructor() {
    fun getSitePlugins(site: SiteModel): List<SitePluginModel> {
        return PluginSqlUtils.getSitePlugins(site)
    }

    fun insertOrReplaceSitePlugins(site: SiteModel, plugins: List<SitePluginModel>) {
        PluginSqlUtils.insertOrReplaceSitePlugins(site, plugins)
    }

    fun insertOrUpdateSitePlugin(site: SiteModel, plugin: SitePluginModel?): Int {
        return PluginSqlUtils.insertOrUpdateSitePlugin(site, plugin)
    }

    fun getSitePluginBySlug(site: SiteModel, slug: String?): SitePluginModel? {
        return PluginSqlUtils.getSitePluginBySlug(site, slug)
    }

    fun getSitePluginByName(site: SiteModel, pluginName: String?): SitePluginModel? {
        return PluginSqlUtils.getSitePluginByName(site, pluginName)
    }

    fun getSitePluginByNames(site: SiteModel, pluginNames: List<String?>?): List<SitePluginModel> {
        return PluginSqlUtils.getSitePluginByNames(site, pluginNames)
    }
}
