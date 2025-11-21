package org.wordpress.android.fluxc.network.rest.wpcom.theme;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.WPComThemeResponse.WPComThemeListResponse;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedCurrentThemePayload;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedWpComThemesPayload;
import org.wordpress.android.fluxc.store.ThemeStore.SiteThemePayload;
import org.wordpress.android.fluxc.store.ThemeStore.ThemesError;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class ThemeRestClient extends BaseWPComRestClient {
    @Inject public ThemeRestClient(
            Context appContext,
            Dispatcher dispatcher,
            @Named("regular") RequestQueue requestQueue,
            AccessToken accessToken,
            UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    /**
     * [Undocumented!] Endpoint: v1.1/sites/$siteId/themes/$themeId/install
     */
    public void installTheme(@NonNull final SiteModel site, @NonNull final ThemeModel theme) {
        String themeId = theme.getThemeId();
        if (!site.isWPComAtomic()) {
            themeId = getThemeIdWithWpComSuffix(theme);
        }
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.theme(themeId).install.getUrlV1_1();
        add(WPComGsonRequest.buildPostRequest(url, null, JetpackThemeResponse.class,
                (response, headers) -> {
                    AppLog.d(AppLog.T.API, "Received response to Jetpack theme installation request.");
                    ThemeModel responseTheme = createThemeFromJetpackResponse(response);
                    SiteThemePayload payload = new SiteThemePayload(site, responseTheme);
                    mDispatcher.dispatch(ThemeActionBuilder.newInstalledThemeAction(payload));
                }, error -> {
                    AppLog.d(AppLog.T.API, "Received error response to Jetpack theme installation request.");
                    SiteThemePayload payload = new SiteThemePayload(site, theme);
                    payload.error = new ThemesError(error.apiError, error.message);
                    mDispatcher.dispatch(ThemeActionBuilder.newInstalledThemeAction(payload));
                }));
    }

    /**
     * Endpoint: v1.1/sites/$siteId/themes/mine
     *
     * @see <a href="https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/themes/mine/">Documentation</a>
     */
    public void activateTheme(@NonNull final SiteModel site, @NonNull final ThemeModel theme) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.mine.getUrlV1_1();
        Map<String, Object> params = new HashMap<>();
        params.put("theme", theme.getThemeId());

        add(WPComGsonRequest.buildPostRequest(url, params, WPComThemeResponse.class,
                (response, headers) -> {
                    AppLog.d(AppLog.T.API, "Received response to theme activation request.");
                    SiteThemePayload payload = new SiteThemePayload(site, theme);
                    payload.theme.setActive(StringUtils.equals(theme.getThemeId(), response.id));
                    mDispatcher.dispatch(ThemeActionBuilder.newActivatedThemeAction(payload));
                }, error -> {
                    AppLog.d(AppLog.T.API, "Received error response to theme activation request.");
                    SiteThemePayload payload = new SiteThemePayload(site, theme);
                    payload.error = new ThemesError(error.apiError, error.message);
                    mDispatcher.dispatch(ThemeActionBuilder.newActivatedThemeAction(payload));
                }));
    }

    /**
     * [Undocumented!] Endpoint: v1.2/themes
     *
     * @see <a href="https://developer.wordpress.com/docs/api/1.1/get/themes/">Previous version</a>
     */
    public void fetchWpComThemes(@Nullable String filter, int resultsLimit) {
        String url = WPCOMREST.themes.getUrlV1_2();
        Map<String, String> params = new HashMap<>();
        params.put("number", String.valueOf(resultsLimit));
        if (filter != null) {
            params.put("filter", filter);
        }
        add(WPComGsonRequest.buildGetRequest(url, params, WPComThemeListResponse.class,
                (response, headers) -> {
                    AppLog.d(AppLog.T.API, "Received response to WP.com themes fetch request.");
                    List<ThemeModel> themes = createThemeListFromArrayResponse(response);
                    FetchedWpComThemesPayload payload = new FetchedWpComThemesPayload(themes);
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpComThemesAction(payload));
                }, error -> {
                    AppLog.e(AppLog.T.API, "Received error response to WP.com themes fetch request.");
                    ThemesError themeError = new ThemesError(error.apiError, error.message);
                    FetchedWpComThemesPayload payload = new FetchedWpComThemesPayload(themeError);
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpComThemesAction(payload));
                }));
    }

    /**
     * Endpoint: v1.1/sites/$siteId/themes/mine; same endpoint for both Jetpack and WP.com sites!
     *
     * @see <a href="https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/themes/mine/">Documentation</a>
     */
    public void fetchCurrentTheme(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.mine.getUrlV1_1();
        add(WPComGsonRequest.buildGetRequest(url, null, WPComThemeResponse.class,
                (response, headers) -> {
                    AppLog.d(AppLog.T.API, "Received response to current theme fetch request.");
                    ThemeModel responseTheme = createThemeFromWPComResponse(response);
                    FetchedCurrentThemePayload payload = new FetchedCurrentThemePayload(site, responseTheme);
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedCurrentThemeAction(payload));
                }, error -> {
                    AppLog.e(AppLog.T.API, "Received error response to current theme fetch request.");
                    ThemesError themeError = new ThemesError(error.apiError, error.message);
                    FetchedCurrentThemePayload payload = new FetchedCurrentThemePayload(site, themeError);
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedCurrentThemeAction(payload));
                }));
    }

    @NonNull
    private static ThemeModel createThemeFromWPComResponse(@NonNull WPComThemeResponse response) {
        return new ThemeModel(
                response.id,
                response.name,
                response.demo_uri
        );
    }

    @NonNull
    private static ThemeModel createThemeFromJetpackResponse(@NonNull JetpackThemeResponse response) {
        return new ThemeModel(
                response.id,
                response.name,
                response.active
        );
    }

    @NonNull
    private static List<ThemeModel> createThemeListFromArrayResponse(@NonNull WPComThemeListResponse response) {
        final List<ThemeModel> themeList = new ArrayList<>();
        for (WPComThemeResponse item : response.themes) {
            themeList.add(createThemeFromWPComResponse(item));
        }
        return themeList;
    }

    /**
     * Must provide theme slug with -wpcom suffix to install a WP.com theme on a Jetpack site.
     *
     * @see <a href="https://developer.wordpress.com/docs/api/console/">Documentation</a>
     */
    @NonNull
    private String getThemeIdWithWpComSuffix(@NonNull ThemeModel theme) {
        if (theme.getThemeId().endsWith("-wpcom")) {
            return theme.getThemeId();
        }

        return theme.getThemeId() + "-wpcom";
    }
}
