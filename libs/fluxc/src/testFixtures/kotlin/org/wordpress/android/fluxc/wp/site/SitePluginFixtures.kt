package org.wordpress.android.fluxc.wp.site

import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.plugin.SitePluginModel

object SitePluginFixtures {
    fun createTestSitePlugin(
        siteId: Int = 1,
        name: String = "",
        version: String = "1.0.0",
        slug: String = name.lowercase().replace(" ", "-"),
        authorName: String = "",
        isActive: Boolean = true
    ) = SitePluginModel(
        siteId = LocalOrRemoteId.LocalId(siteId),
        name = name,
        version = version,
        slug = slug,
        authorName = authorName,
        isActive = isActive
    )
}
