package org.wordpress.android.fluxc.network.rest.wpcom.theme

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ThemeActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.theme.WPComThemeResponse.WPComThemeListResponse
import org.wordpress.android.fluxc.store.ThemeStore.FetchedCurrentThemePayload
import org.wordpress.android.fluxc.store.ThemeStore.FetchedWpComThemesPayload
import org.wordpress.android.fluxc.store.ThemeStore.SiteThemePayload
import org.wordpress.android.fluxc.store.ThemeStore.ThemesError
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.StringUtils
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ThemeRestClient @Inject constructor(
    appContext: Context,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {

    /**
     * [Undocumented!] Endpoint: v1.1/sites/$siteId/themes/$themeId/install
     */
    fun installTheme(site: SiteModel, theme: ThemeModel) {
        val themeId = if (!site.isWPComAtomic) {
            getThemeIdWithWpComSuffix(theme)
        } else {
            theme.themeId
        }
        val url = WPCOMREST.sites.site(site.siteId).themes.theme(themeId).install.getUrlV1_1()
        add(
            WPComGsonRequest.buildPostRequest(
                url, null, JetpackThemeResponse::class.java,
                { response, _ ->
                    AppLog.d(T.API, "Received response to Jetpack theme installation request.")
                    val responseTheme = createThemeFromJetpackResponse(response)
                    val payload = SiteThemePayload(site, responseTheme)
                    mDispatcher.dispatch(ThemeActionBuilder.newInstalledThemeAction(payload))
                },
                { error ->
                    AppLog.d(T.API, "Received error response to Jetpack theme installation request.")
                    val payload = SiteThemePayload(site, theme)
                    payload.error = ThemesError(error.apiError, error.message)
                    mDispatcher.dispatch(ThemeActionBuilder.newInstalledThemeAction(payload))
                }
            )
        )
    }

    /**
     * Endpoint: v1.1/sites/$siteId/themes/mine
     *
     * @see <a href="https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/themes/mine/">Documentation</a>
     */
    fun activateTheme(site: SiteModel, theme: ThemeModel) {
        val url = WPCOMREST.sites.site(site.siteId).themes.mine.getUrlV1_1()
        val params = mapOf("theme" to theme.themeId)

        add(
            WPComGsonRequest.buildPostRequest(
                url, params, WPComThemeResponse::class.java,
                { response, _ ->
                    AppLog.d(T.API, "Received response to theme activation request.")
                    val activeTheme = theme.copy(active = StringUtils.equals(theme.themeId, response.id))
                    val payload = SiteThemePayload(site, activeTheme)
                    mDispatcher.dispatch(ThemeActionBuilder.newActivatedThemeAction(payload))
                },
                { error ->
                    AppLog.d(T.API, "Received error response to theme activation request.")
                    val payload = SiteThemePayload(site, theme)
                    payload.error = ThemesError(error.apiError, error.message)
                    mDispatcher.dispatch(ThemeActionBuilder.newActivatedThemeAction(payload))
                }
            )
        )
    }

    /**
     * [Undocumented!] Endpoint: v1.2/themes
     *
     * @see <a href="https://developer.wordpress.com/docs/api/1.1/get/themes/">Previous version</a>
     */
    fun fetchWpComThemes(filter: String?, resultsLimit: Int) {
        val url = WPCOMREST.themes.getUrlV1_2()
        val params = mutableMapOf("number" to resultsLimit.toString())
        filter?.let { params["filter"] = it }

        add(
            WPComGsonRequest.buildGetRequest(
                url, params, WPComThemeListResponse::class.java,
                { response, _ ->
                    AppLog.d(T.API, "Received response to WP.com themes fetch request.")
                    val themes = createThemeListFromArrayResponse(response)
                    val payload = FetchedWpComThemesPayload(themes)
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpComThemesAction(payload))
                },
                { error ->
                    AppLog.e(T.API, "Received error response to WP.com themes fetch request.")
                    val themeError = ThemesError(error.apiError, error.message)
                    val payload = FetchedWpComThemesPayload(themeError)
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpComThemesAction(payload))
                }
            )
        )
    }

    /**
     * Endpoint: v1.1/sites/$siteId/themes/mine; same endpoint for both Jetpack and WP.com sites!
     *
     * @see <a href="https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/themes/mine/">Documentation</a>
     */
    fun fetchCurrentTheme(site: SiteModel) {
        val url = WPCOMREST.sites.site(site.siteId).themes.mine.getUrlV1_1()
        add(
            WPComGsonRequest.buildGetRequest(
                url, null, WPComThemeResponse::class.java,
                { response, _ ->
                    AppLog.d(T.API, "Received response to current theme fetch request.")
                    val responseTheme = createThemeFromWPComResponse(response)
                    val payload = FetchedCurrentThemePayload(site, responseTheme)
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedCurrentThemeAction(payload))
                },
                { error ->
                    AppLog.e(T.API, "Received error response to current theme fetch request.")
                    val themeError = ThemesError(error.apiError, error.message)
                    val payload = FetchedCurrentThemePayload(site, themeError)
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedCurrentThemeAction(payload))
                }
            )
        )
    }

    private fun createThemeFromWPComResponse(response: WPComThemeResponse): ThemeModel {
        return ThemeModel(
            siteId = LocalId(0),
            themeId = response.id,
            name = response.name,
            demoUrl = response.demo_uri,
            active = false,
            isWpComTheme = false
        )
    }

    private fun createThemeFromJetpackResponse(response: JetpackThemeResponse): ThemeModel {
        return ThemeModel(
            siteId = LocalId(0),
            themeId = response.id,
            name = response.name,
            demoUrl = null,
            active = response.active,
            isWpComTheme = false
        )
    }

    private fun createThemeListFromArrayResponse(response: WPComThemeListResponse): List<ThemeModel> {
        return response.themes.map { createThemeFromWPComResponse(it) }
    }

    /**
     * Must provide theme slug with -wpcom suffix to install a WP.com theme on a Jetpack site.
     *
     * @see <a href="https://developer.wordpress.com/docs/api/console/">Documentation</a>
     */
    private fun getThemeIdWithWpComSuffix(theme: ThemeModel): String {
        return if (theme.themeId.endsWith("-wpcom")) {
            theme.themeId
        } else {
            theme.themeId + "-wpcom"
        }
    }
}
