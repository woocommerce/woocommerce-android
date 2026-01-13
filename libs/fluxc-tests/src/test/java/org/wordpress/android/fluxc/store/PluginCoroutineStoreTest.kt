package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
import org.wordpress.android.fluxc.network.rest.wpapi.plugin.PluginWPAPIRestClient
import org.wordpress.android.fluxc.persistence.SitePluginDao
import org.wordpress.android.fluxc.store.PluginCoroutineStore.WPApiPluginsPayload
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginErrorType
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginErrorType
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class PluginCoroutineStoreTest {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var pluginWPAPIRestClient: PluginWPAPIRestClient
    @Mock lateinit var sitePluginDao: SitePluginDao
    private lateinit var store: PluginCoroutineStore
    private val site: SiteModel = SiteModel().apply {
        id = TEST_SITE_ID
        url = "site.com"
        username = "username"
    }
    private lateinit var onConfiguredEventCaptor: KArgumentCaptor<OnSitePluginConfigured>

    @Before
    fun setUp() {
        store = PluginCoroutineStore(
            initCoroutineEngine(),
            dispatcher,
            pluginWPAPIRestClient,
            sitePluginDao
        )
        onConfiguredEventCaptor = argumentCaptor()
    }

    companion object {
        private const val TEST_SITE_ID = 1
    }

    @Test
    fun `configures a plugin with success`() = test {
        val pluginName = "plugin_name"
        val slug = "plugin_slug"
        val sitePluginModel = SitePluginModel(
            siteId = LocalId(TEST_SITE_ID),
            name = pluginName,
            version = "1.0",
            slug = slug,
            authorName = "author",
            isActive = false
        )
        whenever(sitePluginDao.getSitePluginBySlug(site.localId(), slug)).thenReturn(sitePluginModel)
        val active = true
        whenever(pluginWPAPIRestClient.updatePlugin(site, pluginName, active)).thenReturn(
            WPApiPluginsPayload(
                site,
                sitePluginModel
            )
        )

        val result = store.syncConfigureSitePlugin(site, pluginName, slug, active)

        assertThat(result.isError).isFalse
        assertThat(result.pluginName).isEqualTo(pluginName)
        assertThat(result.slug).isEqualTo(slug)
        verify(sitePluginDao).upsert(sitePluginModel)
    }

    @Test
    fun `configures a plugin and emits an event`() = test {
        val pluginName = "plugin_name"
        val slug = "plugin_slug"
        val sitePluginModel = SitePluginModel(
            siteId = LocalId(TEST_SITE_ID),
            name = pluginName,
            version = "1.0",
            slug = slug,
            authorName = "author",
            isActive = false
        )
        whenever(sitePluginDao.getSitePluginBySlug(site.localId(), slug)).thenReturn(sitePluginModel)
        val active = true
        whenever(pluginWPAPIRestClient.updatePlugin(site, pluginName, active)).thenReturn(
            WPApiPluginsPayload(
                site,
                sitePluginModel
            )
        )

        store.configureSitePlugin(site, pluginName, slug, active)

        verify(dispatcher).emitChange(onConfiguredEventCaptor.capture())
        assertThat(onConfiguredEventCaptor.lastValue.isError).isFalse
        assertThat(onConfiguredEventCaptor.lastValue.pluginName).isEqualTo(pluginName)
        assertThat(onConfiguredEventCaptor.lastValue.slug).isEqualTo(slug)
        verify(sitePluginDao).upsert(sitePluginModel)
    }

    @Test
    fun `does not configure a plugin with a failure`() = test {
        val pluginName = "plugin_name"
        val slug = "plugin_slug"
        val sitePluginModel = SitePluginModel(
            siteId = LocalId(TEST_SITE_ID),
            name = pluginName,
            version = "1.0",
            slug = slug,
            authorName = "author",
            isActive = false
        )
        whenever(sitePluginDao.getSitePluginBySlug(site.localId(), slug)).thenReturn(sitePluginModel)
        val active = true
        whenever(pluginWPAPIRestClient.updatePlugin(site, pluginName, active)).thenReturn(
            WPApiPluginsPayload(
                BaseNetworkError(HTTP_AUTH_ERROR)
            )
        )

        val result = store.syncConfigureSitePlugin(site, pluginName, slug, active)

        assertThat(result.isError).isTrue
        assertThat(result.error.type).isEqualTo(ConfigureSitePluginErrorType.UNAUTHORIZED)
        verify(sitePluginDao, never()).upsert(any())
    }

    @Test
    fun `installs and activates a plugin with success`() = test {
        val slug = "plugin_slug"
        val name = "plugin_name"
        val sitePluginModel = SitePluginModel(
            siteId = LocalId(TEST_SITE_ID),
            name = name,
            version = "1.0",
            slug = slug,
            authorName = "author",
            isActive = false
        )
        whenever(pluginWPAPIRestClient.installPlugin(site, slug)).thenReturn(
            WPApiPluginsPayload(
                site,
                sitePluginModel
            )
        )
        whenever(sitePluginDao.getSitePluginBySlug(site.localId(), slug)).thenReturn(sitePluginModel)
        whenever(pluginWPAPIRestClient.updatePlugin(site, name, true)).thenReturn(
            WPApiPluginsPayload(
                site,
                sitePluginModel
            )
        )

        val result = store.syncInstallSitePlugin(site, slug)

        assertThat(result.isError).isFalse
        assertThat(result.slug).isEqualTo(slug)
        verify(sitePluginDao, times(2)).upsert(sitePluginModel)
        verify(pluginWPAPIRestClient).updatePlugin(site, name, true)
        verify(dispatcher, times(2)).emitChange(any())
    }

    @Test
    fun `does not activate plugin on install failure`() = test {
        val slug = "plugin_slug"
        val name = "plugin_name"
        val sitePluginModel = SitePluginModel(
            siteId = LocalId(TEST_SITE_ID),
            name = name,
            version = "1.0",
            slug = slug,
            authorName = "author",
            isActive = false
        )
        whenever(
            pluginWPAPIRestClient.installPlugin(
                site,

                slug
            )
        ).thenReturn(WPApiPluginsPayload(BaseNetworkError(HTTP_AUTH_ERROR)))

        val result = store.syncInstallSitePlugin(site, slug)

        assertThat(result.isError).isTrue
        assertThat(result.slug).isEqualTo(slug)
        assertThat(result.error.type).isEqualTo(InstallSitePluginErrorType.UNAUTHORIZED)
        verifyNoInteractions(sitePluginDao)
        verify(pluginWPAPIRestClient, never()).updatePlugin(eq(site), any(), any())
        verify(dispatcher, times(1)).emitChange(any())
    }
}
