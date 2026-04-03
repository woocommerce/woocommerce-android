package org.wordpress.android.fluxc.plugin

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SitePluginDao
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule
import org.wordpress.android.fluxc.wp.site.SitePluginFixtures.createTestSitePlugin

@RunWith(RobolectricTestRunner::class)
class SitePluginDaoTest {
    @Rule
    @JvmField
    val wpDatabaseRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var sitePluginDao: SitePluginDao

    @Before
    fun setUp() {
        sitePluginDao = wpDatabaseRule.db.sitePluginDao()
    }

    @Test
    fun `when no plugin is inserted, then site plugins list is empty`() = runTest {
        val site = getTestSite()

        assertThat(sitePluginDao.getSitePlugins(site.localId())).isEmpty()
    }

    @Test
    fun `when plugin is inserted, then it is retrieved correctly`() = runTest {
        val site = getTestSite()
        val slug = "test-plugin-slug"
        val plugin = createTestPluginBySlug(slug)

        sitePluginDao.upsert(plugin)
        val sitePlugins = sitePluginDao.getSitePlugins(site.localId())

        assertThat(sitePlugins).isEqualTo(listOf(plugin))
    }

    @Test
    fun `when plugin is updated, then changes are persisted`() = runTest {
        val site = getTestSite()
        val slug = "test-slug"
        val author = "Original Author"

        val plugin = createTestPluginBySlug(slug).copy(authorName = author)
        sitePluginDao.upsert(plugin)
        val newAuthor = "Updated Author"
        val updatedPlugin = plugin.copy(authorName = newAuthor)
        sitePluginDao.upsert(updatedPlugin)

        val updatedSitePluginList = sitePluginDao.getSitePlugins(site.localId())
        assertThat(updatedSitePluginList).isEqualTo(listOf(updatedPlugin))
    }

    @Test
    fun `when multiple plugins are inserted, then all are retrieved`() = runTest {
        val site = getTestSite()

        insertBasicTestPlugins(SMALL_TEST_POOL)
        val sitePlugins = sitePluginDao.getSitePlugins(site.localId())

        assertThat(sitePlugins).hasSize(SMALL_TEST_POOL)
        assertThat(sitePlugins.map { it.slug }).containsExactly(
            "test-plugin-0",
            "test-plugin-1",
            "test-plugin-2",
            "test-plugin-3",
            "test-plugin-4",
            "test-plugin-5",
            "test-plugin-6",
            "test-plugin-7",
            "test-plugin-8",
            "test-plugin-9"
        )
    }

    @Test
    fun `when site plugins are replaced, then only new plugins remain`() = runTest {
        val site = getTestSite()

        insertBasicTestPlugins(SMALL_TEST_POOL)
        val newSitePluginSlug = "new-replacement-plugin"
        val singleSitePlugin = createTestPluginBySlug(newSitePluginSlug)
        sitePluginDao.replaceAllSitePlugins(site.localId(), listOf(singleSitePlugin))

        val updatedSitePluginList = sitePluginDao.getSitePlugins(site.localId())
        assertThat(updatedSitePluginList).containsOnly(singleSitePlugin)
    }

    @Test
    fun `when getting plugin by slug, then correct plugin is returned`() = runTest {
        val site = getTestSite()
        val pluginSlug1 = "woocommerce-payments"
        val pluginSlug2 = "jetpack"

        val plugin1 = createTestPluginBySlug(pluginSlug1)
        val plugin2 = createTestPluginBySlug(pluginSlug2)
        sitePluginDao.upsert(plugin1)
        sitePluginDao.upsert(plugin2)

        val pluginBySlug1 = sitePluginDao.getSitePluginBySlug(site.localId(), pluginSlug1)
        assertThat(pluginBySlug1).isEqualTo(plugin1)

        val pluginBySlug2 = sitePluginDao.getSitePluginBySlug(site.localId(), pluginSlug2)
        assertThat(pluginBySlug2).isEqualTo(plugin2)
    }

    private fun createTestPluginBySlug(slug: String) = createTestSitePlugin(siteId = TEST_LOCAL_SITE_ID, slug = slug)

    private suspend fun insertBasicTestPlugins(numberOfPlugins: Int) {
        for (i in 0 until numberOfPlugins) {
            val slug = "test-plugin-$i"
            sitePluginDao.upsert(createTestPluginBySlug(slug))
        }
    }

    private fun getTestSite(): SiteModel {
        return SiteModel().apply {
            id = TEST_LOCAL_SITE_ID
        }
    }

    companion object {
        private const val TEST_LOCAL_SITE_ID = 1
        private const val SMALL_TEST_POOL = 10
    }
}
