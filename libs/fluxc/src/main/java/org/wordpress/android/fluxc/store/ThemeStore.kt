package org.wordpress.android.fluxc.store

import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ThemeAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeRestClient
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeStore @Inject constructor(
    dispatcher: Dispatcher,
    private val themeRestClient: ThemeRestClient,
    private val database: WPAndroidDatabase
) : Store(dispatcher) {

    // Payloads
    class FetchWPComThemesPayload(
        @JvmField val filter: String? = null,
        @JvmField val resultsLimit: Int
    ) : Payload<BaseNetworkError>()

    class FetchedCurrentThemePayload : Payload<ThemesError> {
        @JvmField
        val site: SiteModel
        @JvmField
        val theme: ThemeModel?

        constructor(site: SiteModel, error: ThemesError) : super() {
            this.site = site
            this.theme = null
            this.error = error
        }

        constructor(site: SiteModel, theme: ThemeModel) : super() {
            this.site = site
            this.theme = theme
        }
    }

    class FetchedWpComThemesPayload : Payload<ThemesError> {
        @JvmField
        val themes: List<ThemeModel>

        constructor(error: ThemesError) : super() {
            this.error = error
            this.themes = emptyList()
        }

        constructor(themes: List<ThemeModel>) : super() {
            this.themes = themes
        }
    }

    class SiteThemePayload(
        @JvmField val site: SiteModel,
        @JvmField val theme: ThemeModel
    ) : Payload<ThemesError>()

    enum class ThemeErrorType {
        GENERIC_ERROR,
        UNAUTHORIZED,
        NOT_AVAILABLE,
        THEME_NOT_FOUND,
        THEME_ALREADY_INSTALLED,
        UNKNOWN_THEME,
        MISSING_THEME;

        companion object {
            fun fromString(type: String): ThemeErrorType {
                return values().find { it.name.equals(type, ignoreCase = true) }
                    ?: GENERIC_ERROR
            }
        }
    }

    class ThemesError : OnChangedError {
        @JvmField
        val type: ThemeErrorType
        @JvmField
        val message: String?

        constructor(type: String, message: String?) {
            this.type = ThemeErrorType.fromString(type)
            this.message = message
        }

        constructor(type: ThemeErrorType) {
            this.type = type
            this.message = null
        }
    }

    class OnWpComThemesChanged : OnChanged<ThemesError>()

    class OnCurrentThemeFetched(
        @JvmField val site: SiteModel,
        @JvmField val theme: ThemeModel?
    ) : OnChanged<ThemesError>()

    class OnThemeActivated(
        @JvmField val site: SiteModel,
        @JvmField val theme: ThemeModel
    ) : OnChanged<ThemesError>()

    class OnThemeInstalled(
        @JvmField val site: SiteModel,
        @JvmField val theme: ThemeModel
    ) : OnChanged<ThemesError>()

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Suppress("UNCHECKED_CAST")
    override fun onAction(action: Action<*>) {
        val actionType = action.type
        if (actionType !is ThemeAction) {
            return
        }
        when (actionType) {
            ThemeAction.FETCH_WP_COM_THEMES ->
                fetchWpComThemes(action.payload as FetchWPComThemesPayload)
            ThemeAction.FETCHED_WP_COM_THEMES ->
                handleWpComThemesFetched(action.payload as FetchedWpComThemesPayload)
            ThemeAction.FETCH_CURRENT_THEME ->
                fetchCurrentTheme(action.payload as SiteModel)
            ThemeAction.FETCHED_CURRENT_THEME ->
                handleCurrentThemeFetched(action.payload as FetchedCurrentThemePayload)
            ThemeAction.ACTIVATE_THEME ->
                activateTheme(action.payload as SiteThemePayload)
            ThemeAction.ACTIVATED_THEME ->
                handleThemeActivated(action.payload as SiteThemePayload)
            ThemeAction.INSTALL_THEME ->
                installTheme(action.payload as SiteThemePayload)
            ThemeAction.INSTALLED_THEME ->
                handleThemeInstalled(action.payload as SiteThemePayload)
        }
    }

    override fun onRegister() {
        AppLog.d(T.API, "ThemeStore onRegister")
    }

    suspend fun getWpComThemes(themeIds: List<String>): List<ThemeModel> =
        database.themeDao().getWpComThemes(themeIds)

    fun getInstalledThemeByThemeId(siteModel: SiteModel, themeId: String): ThemeModel? {
        if (themeId.isEmpty()) {
            return null
        }
        return runBlocking {
            database.themeDao().getSiteThemeByThemeId(siteModel.localId(), themeId)
        }
    }

    suspend fun getWpComThemeByThemeId(themeId: String): ThemeModel? {
        if (themeId.isEmpty()) {
            return null
        }
        return database.themeDao().getWpComThemeByThemeId(themeId)
    }

    fun setActiveThemeForSite(site: SiteModel, theme: ThemeModel) = runBlocking {
        // Deactivate all currently active themes for the site
        val activeThemes = database.themeDao().getActiveThemesForSite(site.localId())
        database.themeDao().upsertThemes(activeThemes.map { it.copy(active = false) })

        // Insert or update the new active theme
        val activeTheme = theme.copy(
            siteId = site.localId(),
            isWpComTheme = false,
            active = true
        )
        database.themeDao().upsert(activeTheme)
    }

    private fun fetchWpComThemes(payload: FetchWPComThemesPayload) {
        themeRestClient.fetchWpComThemes(payload.filter, payload.resultsLimit)
    }

    private fun handleWpComThemesFetched(payload: FetchedWpComThemesPayload) {
        val event = OnWpComThemesChanged()
        if (payload.isError) {
            event.error = payload.error
        } else {
            runBlocking {
                val wpComThemes = payload.themes.map { it.copy(isWpComTheme = true) }
                database.themeDao().replaceAllWpComThemes(wpComThemes)
            }
        }
        emitChange(event)
    }

    private fun fetchCurrentTheme(site: SiteModel) {
        if (site.isUsingWpComRestApi) {
            themeRestClient.fetchCurrentTheme(site)
        } else {
            val error = ThemesError(ThemeErrorType.NOT_AVAILABLE)
            val payload = FetchedCurrentThemePayload(site, error)
            handleCurrentThemeFetched(payload)
        }
    }

    private fun handleCurrentThemeFetched(payload: FetchedCurrentThemePayload) {
        val event = OnCurrentThemeFetched(payload.site, payload.theme)
        if (payload.isError) {
            event.error = payload.error
        } else {
            if (payload.theme != null) {
                setActiveThemeForSite(payload.site, payload.theme)
            } else {
                AppLog.w(T.THEMES, "Fetched current theme payload theme is null.")
            }
        }
        emitChange(event)
    }

    private fun installTheme(payload: SiteThemePayload) {
        if (payload.site.isJetpackConnected && payload.site.isUsingWpComRestApi) {
            themeRestClient.installTheme(payload.site, payload.theme)
        } else {
            payload.error = ThemesError(ThemeErrorType.NOT_AVAILABLE)
            handleThemeInstalled(payload)
        }
    }

    private fun handleThemeInstalled(payload: SiteThemePayload) {
        val event = OnThemeInstalled(payload.site, payload.theme)
        if (payload.isError) {
            event.error = payload.error
        } else {
            runBlocking {
                val siteTheme = payload.theme.copy(
                    siteId = payload.site.localId(),
                    isWpComTheme = false
                )
                database.themeDao().upsert(siteTheme)
            }
        }
        emitChange(event)
    }

    private fun activateTheme(payload: SiteThemePayload) {
        if (payload.site.isUsingWpComRestApi) {
            themeRestClient.activateTheme(payload.site, payload.theme)
        } else {
            payload.error = ThemesError(ThemeErrorType.NOT_AVAILABLE)
            handleThemeActivated(payload)
        }
    }

    private fun handleThemeActivated(payload: SiteThemePayload) {
        val event = OnThemeActivated(payload.site, payload.theme)
        if (payload.isError) {
            event.error = payload.error
        } else {
            // payload theme doesn't have all the data so we grab a copy of the database theme and update active flag
            val activatedTheme = if (payload.site.isJetpackConnected) {
                getInstalledThemeByThemeId(payload.site, payload.theme.themeId)
            } else {
                runBlocking { getWpComThemeByThemeId(payload.theme.themeId) }
            }
            activatedTheme?.let {
                setActiveThemeForSite(payload.site, it)
            }
        }
        emitChange(event)
    }
}
