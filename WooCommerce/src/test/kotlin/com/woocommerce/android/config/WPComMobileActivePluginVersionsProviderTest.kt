package com.woocommerce.android.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin

class WPComMobileActivePluginVersionsProviderTest {
    private val sut = WPComMobileActivePluginVersionsProvider()

    @Test
    fun `given active Woo core with version, when building versions, then return Woo plugin path`() {
        // GIVEN
        val plugin = createPlugin(version = WOO_VERSION)

        // WHEN
        val activePluginVersions = sut.buildActivePluginVersions(listOf(plugin))

        // THEN
        assertThat(activePluginVersions).containsExactlyEntriesOf(
            mapOf("woocommerce/woocommerce.php" to WOO_VERSION)
        )
    }

    @Test
    fun `given inactive Woo core, when building versions, then return empty map`() {
        // GIVEN
        val plugin = createPlugin(version = WOO_VERSION, isActive = false)

        // WHEN
        val activePluginVersions = sut.buildActivePluginVersions(listOf(plugin))

        // THEN
        assertThat(activePluginVersions).isEmpty()
    }

    @Test
    fun `given blank Woo core version, when building versions, then return empty map`() {
        // GIVEN
        val plugin = createPlugin(version = " ")

        // WHEN
        val activePluginVersions = sut.buildActivePluginVersions(listOf(plugin))

        // THEN
        assertThat(activePluginVersions).isEmpty()
    }

    @Test
    fun `given non Woo core plugin, when building versions, then return empty map`() {
        // GIVEN
        val plugin = createPlugin(name = WooPlugin.WOO_PAYMENTS.pluginName, version = WOO_VERSION)

        // WHEN
        val activePluginVersions = sut.buildActivePluginVersions(listOf(plugin))

        // THEN
        assertThat(activePluginVersions).isEmpty()
    }

    private fun createPlugin(
        name: String = WooPlugin.WOO_CORE.pluginName,
        version: String,
        isActive: Boolean = true
    ) = SitePluginModel(
        siteId = LocalId(1),
        name = name,
        version = version,
        slug = name.substringBefore("/"),
        authorName = "",
        isActive = isActive
    )

    private companion object {
        const val WOO_VERSION = "10.9.2"
    }
}
