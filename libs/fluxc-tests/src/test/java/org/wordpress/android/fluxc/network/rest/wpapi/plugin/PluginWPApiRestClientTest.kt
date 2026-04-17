package org.wordpress.android.fluxc.network.rest.wpapi.plugin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceWPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import org.wordpress.android.fluxc.network.rest.wpapi.plugin.PluginResponseModel.Description
import org.wordpress.android.fluxc.test

@RunWith(Parameterized::class)
class PluginWPApiRestClientTest(private val site: SiteModel) {
    private val cookieNonceWPAPINetwork: CookieNonceWPAPINetwork = mock()
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork = mock()
    private val applicationPasswordsStore: ApplicationPasswordsStore = mock()
    private val expectedNetwork
        get() = if (site.username.isNullOrEmpty() || site.password.isNullOrEmpty()) {
            applicationPasswordsNetwork
        } else {
            cookieNonceWPAPINetwork
        }

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var bodyCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: PluginWPAPIRestClient

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        bodyCaptor = argumentCaptor()
        restClient = PluginWPAPIRestClient(
            cookieNonceWPAPINetwork = cookieNonceWPAPINetwork,
            applicationPasswordsNetwork = applicationPasswordsNetwork,
            applicationPasswordsStore = applicationPasswordsStore
        )
    }

    @Test
    fun `installs a plugin`() = test {
        initInstallPluginResponse(testPlugin)
        val installedPluginSlug = "plugin_slug"
        val responseModel = restClient.installPlugin(site, installedPluginSlug)
        assertMappedPlugin(responseModel.data!!, testPlugin)
        assertThat(urlCaptor.lastValue)
            .isEqualTo("/wp/v2/plugins/")
        assertThat(bodyCaptor.lastValue).isEqualTo(mapOf("slug" to installedPluginSlug))
    }

    @Test
    fun `sets plugin as active`() = test {
        initConfigurePluginResponse(testPlugin)
        val installedPluginSlug = "plugin_slug"
        val active = true
        val responseModel = restClient.updatePlugin(site, installedPluginSlug, active)
        assertMappedPlugin(responseModel.data!!, testPlugin)
        assertThat(urlCaptor.lastValue)
            .isEqualTo("/wp/v2/plugins/$installedPluginSlug/")
        assertThat(bodyCaptor.lastValue).isEqualTo(mapOf("status" to "active"))
    }

    @Test
    fun `sets plugin as inactive`() = test {
        initConfigurePluginResponse(testPlugin)
        val installedPluginSlug = "plugin_slug"
        val active = false
        val responseModel = restClient.updatePlugin(site, installedPluginSlug, active)
        assertMappedPlugin(responseModel.data!!, testPlugin)
        assertThat(urlCaptor.lastValue)
            .isEqualTo("/wp/v2/plugins/$installedPluginSlug/")
        assertThat(bodyCaptor.lastValue).isEqualTo(mapOf("status" to "inactive"))
    }

    private fun assertMappedPlugin(
        responseModel: SitePluginModel,
        plugin: PluginResponseModel
    ) {
        assertThat(responseModel.isActive).isEqualTo(plugin.status == "active")
        assertThat(responseModel.authorName).isEqualTo(plugin.author)
        assertThat(responseModel.name).isEqualTo(plugin.plugin)
        assertThat(responseModel.version).isEqualTo(plugin.version)
        assertThat(responseModel.slug).isEqualTo(plugin.textDomain)
    }

    private suspend fun initInstallPluginResponse(
        data: PluginResponseModel? = null,
        error: WPAPINetworkError? = null
    ): WPAPIResponse<PluginResponseModel> {
        val response =
            if (error != null) WPAPIResponse.Error(error) else WPAPIResponse.Success(data ?: mock(), emptyList())
        whenever(
            expectedNetwork.executePostGsonRequest(
                eq(site),
                urlCaptor.capture(),
                eq(PluginResponseModel::class.java),
                bodyCaptor.capture()
            )
        ).thenReturn(response)
        return response
    }

    private suspend fun initConfigurePluginResponse(
        data: PluginResponseModel? = null,
        error: WPAPINetworkError? = null
    ): WPAPIResponse<PluginResponseModel> {
        val response =
            if (error != null) WPAPIResponse.Error(error) else WPAPIResponse.Success(data ?: mock(), emptyList())
        whenever(
            expectedNetwork.executePutGsonRequest(
                eq(site),
                urlCaptor.capture(),
                eq(PluginResponseModel::class.java),
                bodyCaptor.capture()
            )
        ).thenReturn(response)
        return response
    }

    companion object {
        private val testPlugin = PluginResponseModel(
            "test-plugin/test-plugin",
            "status",
            "name",
            "pluginUri",
            "author",
            "authorUri",
            Description("raw", "renderd"),
            "1.2.3",
            false,
            "",
            "",
            "plugin"
        )

        private const val SITE_URL = "http://site.com"

        @Parameters(name = "{index}: {0}")
        @JvmStatic
        fun data(): List<Any> {
            return listOf(
                SiteModel().apply {
                    url = SITE_URL
                    username = "username"
                    password = "password"
                },
                SiteModel().apply {
                    url = SITE_URL
                }
            )
        }
    }
}
