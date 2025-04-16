package org.wordpress.android.fluxc.network.rest.wpapi.plugin

import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceWPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import org.wordpress.android.fluxc.store.PluginCoroutineStore.WPApiPluginsPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginWPAPIRestClient @Inject constructor(
    private val cookieNonceWPAPINetwork: CookieNonceWPAPINetwork,
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork,
    private val applicationPasswordsStore: ApplicationPasswordsStore
) {
    private fun getNetwork(
        site: SiteModel
    ): WPAPINetwork {
        return when {
            applicationPasswordsStore.hasCredentials(site)
                || site.username.isNullOrEmpty() || site.password.isNullOrEmpty() -> applicationPasswordsNetwork

            else -> cookieNonceWPAPINetwork
        }
    }

    suspend fun fetchPlugins(
        site: SiteModel,
        enableCaching: Boolean = false
    ): WPApiPluginsPayload<List<SitePluginModel>> {
        val response = getNetwork(site).executeGetGsonRequest(
            site = site,
            path = WPAPI.plugins.urlV2,
            clazz = Array<PluginResponseModel>::class.java,
            params = emptyMap(),
            enableCaching = enableCaching,
            cacheTimeToLive = 0,
            forced = false,
            requestTimeout = 0,
            retries = 0
        )

        return when (response) {
            is Success -> {
                val plugins = response.data?.map {
                    it.toDomainModel(site.id)
                }
                WPApiPluginsPayload(site, plugins)
            }

            is Error -> {
                WPApiPluginsPayload(response.error)
            }
        }
    }

    suspend fun fetchPlugin(
        site: SiteModel,
        pluginName: String
    ): WPApiPluginsPayload<SitePluginModel> {
        val response = getNetwork(site).executeGetGsonRequest(
            site = site,
            path = WPAPI.plugins.name(pluginName).urlV2,
            clazz = PluginResponseModel::class.java
        )
        return handleResponse(response, site)
    }

    suspend fun installPlugin(
        site: SiteModel,
        installedPluginSlug: String
    ): WPApiPluginsPayload<SitePluginModel> {
        val response = getNetwork(site).executePostGsonRequest(
            site = site,
            path = WPAPI.plugins.urlV2,
            clazz = PluginResponseModel::class.java,
            body = mapOf("slug" to installedPluginSlug)
        )
        return handleResponse(response, site)
    }

    suspend fun updatePlugin(
        site: SiteModel,
        updatedPlugin: String,
        active: Boolean
    ): WPApiPluginsPayload<SitePluginModel> {
        val response = getNetwork(site).executePutGsonRequest(
            site = site,
            path = WPAPI.plugins.name(updatedPlugin).urlV2,
            clazz = PluginResponseModel::class.java,
            body = mapOf("status" to if (active) "active" else "inactive")
        )
        return handleResponse(response, site)
    }

    suspend fun deletePlugin(
        site: SiteModel,
        deletedPlugin: String
    ): WPApiPluginsPayload<SitePluginModel> {
        val response = getNetwork(site).executeDeleteGsonRequest(
            site = site,
            path = WPAPI.plugins.name(deletedPlugin).urlV2,
            clazz = PluginResponseModel::class.java
        )
        return handleResponse(response, site)
    }

    private fun handleResponse(
        response: WPAPIResponse<PluginResponseModel>,
        site: SiteModel
    ) = when (response) {
        is Success -> {
            val plugin = response.data?.toDomainModel(site.id)
            WPApiPluginsPayload(site, plugin)
        }

        is Error -> {
            WPApiPluginsPayload(response.error)
        }
    }
}
