package org.wordpress.android.fluxc.persistence

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.plugin.SitePluginModel

@Dao
abstract class SitePluginDao {
    @Query("SELECT * FROM SitePluginEntity WHERE siteId = :siteId ORDER BY name ASC")
    abstract suspend fun getSitePlugins(siteId: LocalId): List<SitePluginModel>

    @Upsert
    abstract suspend fun upsert(plugin: SitePluginModel)

    @Upsert
    protected abstract suspend fun upsertAll(plugins: List<SitePluginModel>)

    @Query("DELETE FROM SitePluginEntity WHERE siteId = :siteId")
    protected abstract suspend fun deleteBySite(siteId: LocalId)

    @Transaction
    open suspend fun replaceAllSitePlugins(siteId: LocalId, plugins: List<SitePluginModel>) {
        deleteBySite(siteId)
        upsertAll(plugins)
    }

    @Query("SELECT * FROM SitePluginEntity WHERE siteId = :siteId AND slug = :slug LIMIT 1")
    abstract suspend fun getSitePluginBySlug(siteId: LocalId, slug: String): SitePluginModel?

    @Query("SELECT * FROM SitePluginEntity WHERE siteId = :siteId AND name = :name LIMIT 1")
    abstract suspend fun getSitePluginByName(siteId: LocalId, name: String): SitePluginModel?

    @Query("SELECT * FROM SitePluginEntity WHERE siteId = :siteId AND name IN (:names)")
    abstract suspend fun getSitePluginsByNames(siteId: LocalId, names: List<String>): List<SitePluginModel>

    @Query("SELECT * FROM SitePluginEntity WHERE siteId = :siteId AND isActive = 1")
    abstract suspend fun getActiveSitePlugins(siteId: LocalId): List<SitePluginModel>

    // Non-suspend blocking versions for Java interop (deprecated)
    @Deprecated("Use suspend version from Kotlin code")
    @Upsert
    abstract fun upsertBlocking(plugin: SitePluginModel)
}
