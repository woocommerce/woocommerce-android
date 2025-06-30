package org.wordpress.android.fluxc.network.rest.wpcom.wc.system

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WCSystemPluginResponse.SystemPluginModel

data class WCSystemPluginResponse(
    @SerializedName("active_plugins") private val activePlugins: List<SystemPluginModel>?,
    @SerializedName("inactive_plugins") private val inactivePlugins: List<SystemPluginModel>?
) {
    val plugins: List<SystemPluginModel>
        get() = activePlugins.orEmpty().map { it.copy(isActive = true) } + inactivePlugins.orEmpty()

    data class SystemPluginModel(
        val plugin: String,
        val name: String,
        val version: String?,
        val url: String?,
        @SerializedName("version_latest") val versionLatest: String? = null,
        @SerializedName("author_name") val authorName: String? = null,
        @SerializedName("author_url") val authorUrl: String? = null,
        val isActive: Boolean = false
    )
}

/**
 * Maps a System endpoint plugin item to [SitePluginModel]
 *
 * This function follows the same logic as Core to retrieve the name and slug from the plugin's path,
 * Check class-wp-rest-plugins-controller.php and plugin.php in WPCore for more information.
 */
fun SystemPluginModel.toDomainModel(siteId: Int): SitePluginModel {
    return SitePluginModel().apply {
        localSiteId = siteId
        name = this@toDomainModel.plugin.substringBeforeLast(".")
        displayName = this@toDomainModel.name
        authorName = this@toDomainModel.authorName
        authorUrl = this@toDomainModel.authorUrl
        slug = this@toDomainModel.plugin.substringBeforeLast("/")
        version = this@toDomainModel.version
        setIsActive(this@toDomainModel.isActive)
    }
}
