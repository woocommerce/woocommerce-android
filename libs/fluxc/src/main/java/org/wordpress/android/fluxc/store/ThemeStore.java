package org.wordpress.android.fluxc.store;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.ThemeAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeRestClient;
import org.wordpress.android.fluxc.persistence.ThemeSqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ThemeStore extends Store {
    public static final String MOBILE_FRIENDLY_CATEGORY_BLOG = "starting-blog";
    public static final String MOBILE_FRIENDLY_CATEGORY_WEBSITE = "starting-website";
    public static final String MOBILE_FRIENDLY_CATEGORY_PORTFOLIO = "starting-portfolio";

    // A high number to ensure we get all themes in one request
    private static final int DEFAULT_LIMIT_OF_THEME_RESULTS = 500;

    // Payloads
    public static class FetchWPComThemesPayload extends Payload<BaseNetworkError> {
        @Nullable public String filter;
        public int resultsLimit = DEFAULT_LIMIT_OF_THEME_RESULTS;

        public FetchWPComThemesPayload() {}

        public FetchWPComThemesPayload(@Nullable String filter) {
            this.filter = filter;
        }

        public FetchWPComThemesPayload(@Nullable String filter, int resultsLimit) {
            this.filter = filter;
            this.resultsLimit = resultsLimit;
        }
    }

    public static class FetchedCurrentThemePayload extends Payload<ThemesError> {
        @NonNull public SiteModel site;
        @Nullable public ThemeModel theme;

        public FetchedCurrentThemePayload(@NonNull SiteModel site, @NonNull ThemesError error) {
            this.site = site;
            this.error = error;
        }

        public FetchedCurrentThemePayload(@NonNull SiteModel site, @NonNull ThemeModel theme) {
            this.site = site;
            this.theme = theme;
        }
    }

    public static class FetchedWpComThemesPayload extends Payload<ThemesError> {
        @NonNull public List<ThemeModel> themes;

        public FetchedWpComThemesPayload(@NonNull ThemesError error) {
            this.error = error;
            this.themes = new ArrayList<>();
        }

        public FetchedWpComThemesPayload(@NonNull List<ThemeModel> themes) {
            this.themes = themes;
        }
    }

    public static class SiteThemePayload extends Payload<ThemesError> {
        @NonNull public SiteModel site;
        @NonNull public ThemeModel theme;

        public SiteThemePayload(@NonNull SiteModel site, @NonNull ThemeModel theme) {
            this.site = site;
            this.theme = theme;
        }
    }

    public enum ThemeErrorType {
        GENERIC_ERROR,
        UNAUTHORIZED,
        NOT_AVAILABLE,
        THEME_NOT_FOUND,
        THEME_ALREADY_INSTALLED,
        UNKNOWN_THEME,
        MISSING_THEME;

        @NonNull
        public static ThemeErrorType fromString(@NonNull String type) {
            for (ThemeErrorType v : ThemeErrorType.values()) {
                if (type.equalsIgnoreCase(v.name())) {
                    return v;
                }
            }
            return GENERIC_ERROR;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class ThemesError implements OnChangedError {
        @NonNull public ThemeErrorType type;
        @Nullable public String message;

        public ThemesError(@NonNull String type, @Nullable String message) {
            this.type = ThemeErrorType.fromString(type);
            this.message = message;
        }

        public ThemesError(@NonNull ThemeErrorType type) {
            this.type = type;
        }
    }

    public static class OnWpComThemesChanged extends OnChanged<ThemesError> {
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnCurrentThemeFetched extends OnChanged<ThemesError> {
        @NonNull public SiteModel site;
        @Nullable public ThemeModel theme;

        public OnCurrentThemeFetched(@NonNull SiteModel site, @Nullable ThemeModel theme) {
            this.site = site;
            this.theme = theme;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnThemeActivated extends OnChanged<ThemesError> {
        @NonNull public SiteModel site;
        @NonNull public ThemeModel theme;

        public OnThemeActivated(@NonNull SiteModel site, @NonNull ThemeModel theme) {
            this.site = site;
            this.theme = theme;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnThemeRemoved extends OnChanged<ThemesError> {
        @NonNull public SiteModel site;
        @NonNull public ThemeModel theme;

        public OnThemeRemoved(@NonNull SiteModel site, @NonNull ThemeModel theme) {
            this.site = site;
            this.theme = theme;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnThemeInstalled extends OnChanged<ThemesError> {
        @NonNull public SiteModel site;
        @NonNull public ThemeModel theme;

        public OnThemeInstalled(@NonNull SiteModel site, @NonNull ThemeModel theme) {
            this.site = site;
            this.theme = theme;
        }
    }

    private final ThemeRestClient mThemeRestClient;

    @Inject public ThemeStore(Dispatcher dispatcher, ThemeRestClient themeRestClient) {
        super(dispatcher);
        mThemeRestClient = themeRestClient;
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    @SuppressWarnings("rawtypes")
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof ThemeAction)) {
            return;
        }
        switch ((ThemeAction) actionType) {
            case FETCH_WP_COM_THEMES:
                fetchWpComThemes((FetchWPComThemesPayload) action.getPayload());
                break;
            case FETCHED_WP_COM_THEMES:
                handleWpComThemesFetched((FetchedWpComThemesPayload) action.getPayload());
                break;
            case FETCH_CURRENT_THEME:
                fetchCurrentTheme((SiteModel) action.getPayload());
                break;
            case FETCHED_CURRENT_THEME:
                handleCurrentThemeFetched((FetchedCurrentThemePayload) action.getPayload());
                break;
            case ACTIVATE_THEME:
                activateTheme((SiteThemePayload) action.getPayload());
                break;
            case ACTIVATED_THEME:
                handleThemeActivated((SiteThemePayload) action.getPayload());
                break;
            case INSTALL_THEME:
                installTheme((SiteThemePayload) action.getPayload());
                break;
            case INSTALLED_THEME:
                handleThemeInstalled((SiteThemePayload) action.getPayload());
                break;
        }
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.API, "ThemeStore onRegister");
    }

    @NonNull
    public List<ThemeModel> getWpComThemes() {
        return ThemeSqlUtils.getWpComThemes();
    }

    @NonNull
    public List<ThemeModel> getWpComThemes(@NonNull List<String> themeIds) {
        return ThemeSqlUtils.getWpComThemes(themeIds);
    }

    @NonNull
    public List<ThemeModel> getWpComMobileFriendlyThemes(@NonNull String categorySlug) {
        return ThemeSqlUtils.getWpComMobileFriendlyThemes(categorySlug);
    }

    @NonNull
    public List<ThemeModel> getThemesForSite(@NonNull SiteModel site) {
        return ThemeSqlUtils.getThemesForSite(site);
    }

    @Nullable
    public ThemeModel getInstalledThemeByThemeId(@NonNull SiteModel siteModel, @NonNull String themeId) {
        if (TextUtils.isEmpty(themeId)) {
            return null;
        }
        return ThemeSqlUtils.getSiteThemeByThemeId(siteModel, themeId);
    }

    @Nullable
    @SuppressWarnings("WeakerAccess")
    public ThemeModel getWpComThemeByThemeId(@NonNull String themeId) {
        if (TextUtils.isEmpty(themeId)) {
            return null;
        }
        return ThemeSqlUtils.getWpComThemeByThemeId(themeId);
    }

    @Nullable
    public ThemeModel getActiveThemeForSite(@NonNull SiteModel site) {
        List<ThemeModel> activeTheme = ThemeSqlUtils.getActiveThemeForSite(site);
        return activeTheme.isEmpty() ? null : activeTheme.get(0);
    }

    public void setActiveThemeForSite(@NonNull SiteModel site, @NonNull ThemeModel theme) {
        ThemeSqlUtils.insertOrReplaceActiveThemeForSite(site, theme);
    }

    private void fetchWpComThemes(@NonNull FetchWPComThemesPayload payload) {
        mThemeRestClient.fetchWpComThemes(payload.filter, payload.resultsLimit);
    }

    private void handleWpComThemesFetched(@NonNull FetchedWpComThemesPayload payload) {
        OnWpComThemesChanged event = new OnWpComThemesChanged();
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            ThemeSqlUtils.insertOrReplaceWpComThemes(payload.themes);
        }
        emitChange(event);
    }

    private void fetchCurrentTheme(@NonNull SiteModel site) {
        if (site.isUsingWpComRestApi()) {
            mThemeRestClient.fetchCurrentTheme(site);
        } else {
            ThemesError error = new ThemesError(ThemeErrorType.NOT_AVAILABLE);
            FetchedCurrentThemePayload payload = new FetchedCurrentThemePayload(site, error);
            handleCurrentThemeFetched(payload);
        }
    }

    private void handleCurrentThemeFetched(@NonNull FetchedCurrentThemePayload payload) {
        OnCurrentThemeFetched event = new OnCurrentThemeFetched(payload.site, payload.theme);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            if (payload.theme != null) {
                ThemeSqlUtils.insertOrReplaceActiveThemeForSite(payload.site, payload.theme);
            } else {
                AppLog.w(AppLog.T.THEMES, "Fetched current theme payload theme is null.");
            }
        }
        emitChange(event);
    }

    private void installTheme(@NonNull SiteThemePayload payload) {
        if (payload.site.isJetpackConnected() && payload.site.isUsingWpComRestApi()) {
            mThemeRestClient.installTheme(payload.site, payload.theme);
        } else {
            payload.error = new ThemesError(ThemeErrorType.NOT_AVAILABLE);
            handleThemeInstalled(payload);
        }
    }

    private void handleThemeInstalled(@NonNull SiteThemePayload payload) {
        OnThemeInstalled event = new OnThemeInstalled(payload.site, payload.theme);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            ThemeSqlUtils.insertOrUpdateSiteTheme(payload.site, payload.theme);
        }
        emitChange(event);
    }

    private void activateTheme(@NonNull SiteThemePayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mThemeRestClient.activateTheme(payload.site, payload.theme);
        } else {
            payload.error = new ThemesError(ThemeErrorType.NOT_AVAILABLE);
            handleThemeActivated(payload);
        }
    }

    private void handleThemeActivated(@NonNull SiteThemePayload payload) {
        OnThemeActivated event = new OnThemeActivated(payload.site, payload.theme);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            ThemeModel activatedTheme;
            // payload theme doesn't have all the data so we grab a copy of the database theme and update active flag
            if (payload.site.isJetpackConnected()) {
                activatedTheme = getInstalledThemeByThemeId(payload.site, payload.theme.getThemeId());
            } else {
                activatedTheme = getWpComThemeByThemeId(payload.theme.getThemeId());
            }
            if (activatedTheme != null) {
                setActiveThemeForSite(payload.site, activatedTheme);
            }
        }
        emitChange(event);
    }
}
