package org.wordpress.android.fluxc.network.rest.wpapi.plugin

import com.android.volley.VolleyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceWPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.plugin.PluginResponseModel.Description
import org.wordpress.android.fluxc.test

class PluginWPApiRestClientTest {
    private val cookieNonceWPAPINetwork: CookieNonceWPAPINetwork = mock()
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var bodyCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: PluginWPAPIRestClient

    private val siteUrl = "http://site.com"
    val site = SiteModel().apply {
        url = siteUrl
        username = "username"
    }

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        bodyCaptor = argumentCaptor()
        restClient = PluginWPAPIRestClient(cookieNonceWPAPINetwork)
    }

    @Test
    fun `fetches plugins`() = test {
        initFetchPluginsResponse(arrayOf(testPlugin))
        val responseModel = restClient.fetchPlugins(site, false)
        assertThat(responseModel.data).isNotNull()
        assertMappedPlugin(responseModel.data!![0], testPlugin)
        assertThat(urlCaptor.lastValue)
            .isEqualTo("wp/v2/plugins/")
        assertThat(paramsCaptor.lastValue).isEqualTo(emptyMap<String, String>())
    }

    @Test
    fun `returns error response on fetch`() = test {
        val errorMessage = "message"
        val error = WPAPINetworkError(
            BaseNetworkError(
                NETWORK_ERROR,
                errorMessage,
                VolleyError(errorMessage)
            )
        )
        initFetchPluginsResponse(
            error = error
        )
        val responseModel = restClient.fetchPlugins(site, false)
        assertThat(responseModel.error).isEqualTo(error)
    }

    @Test
    fun `installs a plugin`() = test {
        initInstallPluginResponse(testPlugin)
        val installedPluginSlug = "plugin_slug"
        val responseModel = restClient.installPlugin(site, installedPluginSlug)
        assertMappedPlugin(responseModel.data!!, testPlugin)
        assertThat(urlCaptor.lastValue)
            .isEqualTo("wp/v2/plugins/")
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
            .isEqualTo("wp/v2/plugins/$installedPluginSlug/")
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
            .isEqualTo("wp/v2/plugins/$installedPluginSlug/")
        assertThat(bodyCaptor.lastValue).isEqualTo(mapOf("status" to "inactive"))
    }

    @Test
    fun `deletes a plugin`() = test {
        initDeletePluginResponse(testPlugin)
        val installedPluginSlug = "plugin_slug"
        val responseModel = restClient.deletePlugin(site, installedPluginSlug)
        assertMappedPlugin(responseModel.data!!, testPlugin)
        assertThat(urlCaptor.lastValue)
            .isEqualTo("wp/v2/plugins/$installedPluginSlug/")
        assertThat(bodyCaptor.lastValue).isEqualTo(emptyMap<String, String>())
    }

    private fun assertMappedPlugin(
        responseModel: SitePluginModel,
        plugin: PluginResponseModel
    ) {
        assertThat(responseModel.isActive).isEqualTo(plugin.status == "active")
        assertThat(responseModel.authorUrl).isEqualTo(plugin.authorUri)
        assertThat(responseModel.authorName).isEqualTo(plugin.author)
        assertThat(responseModel.description).isEqualTo(plugin.description!!.raw)
        assertThat(responseModel.displayName).isEqualTo(plugin.name)
        assertThat(responseModel.name).isEqualTo(plugin.plugin)
        assertThat(responseModel.pluginUrl).isEqualTo(plugin.pluginUri)
        assertThat(responseModel.version).isEqualTo(plugin.version)
        assertThat(responseModel.slug).isEqualTo(plugin.textDomain)
    }

    private suspend fun initFetchPluginsResponse(
        data: Array<PluginResponseModel>? = null,
        error: WPAPINetworkError? = null
    ): WPAPIResponse<Array<PluginResponseModel>> {
        return initSyncGetResponse(data ?: arrayOf(mock()), Array<PluginResponseModel>::class.java, error)
    }

    private suspend fun <T : Any> initSyncGetResponse(
        data: T,
        clazz: Class<T>,
        error: WPAPINetworkError? = null,
        cachingEnabled: Boolean = false
    ): WPAPIResponse<T> {
        val response = if (error != null) WPAPIResponse.Error(error) else WPAPIResponse.Success(data)
        whenever(
            cookieNonceWPAPINetwork.executeGetGsonRequest(
                site = eq(site),
                path = urlCaptor.capture(),
                clazz = eq(clazz),
                params = paramsCaptor.capture(),
                enableCaching = eq(cachingEnabled),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
            )
        ).thenReturn(response)
        return response
    }

    private suspend fun initInstallPluginResponse(
        data: PluginResponseModel? = null,
        error: WPAPINetworkError? = null
    ): WPAPIResponse<PluginResponseModel> {
        val response = if (error != null) WPAPIResponse.Error(error) else WPAPIResponse.Success(data ?: mock())
        whenever(
            cookieNonceWPAPINetwork.executePostGsonRequest(
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
        val response = if (error != null) WPAPIResponse.Error(error) else WPAPIResponse.Success(data ?: mock())
        whenever(
            cookieNonceWPAPINetwork.executePutGsonRequest(
                eq(site),
                urlCaptor.capture(),
                eq(PluginResponseModel::class.java),
                bodyCaptor.capture()
            )
        ).thenReturn(response)
        return response
    }

    private suspend fun initDeletePluginResponse(
        data: PluginResponseModel? = null,
        error: WPAPINetworkError? = null
    ): WPAPIResponse<PluginResponseModel> {
        val response = if (error != null) WPAPIResponse.Error(error) else WPAPIResponse.Success(data ?: mock())
        whenever(
            cookieNonceWPAPINetwork.executeDeleteGsonRequest(
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
    }
}
