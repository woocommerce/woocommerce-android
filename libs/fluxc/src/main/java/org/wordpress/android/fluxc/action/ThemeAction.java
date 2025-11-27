package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.ThemeStore.FetchWPComThemesPayload;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedCurrentThemePayload;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedWpComThemesPayload;
import org.wordpress.android.fluxc.store.ThemeStore.SiteThemePayload;

@ActionEnum
public enum ThemeAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchWPComThemesPayload.class)
    FETCH_WP_COM_THEMES,
    @Action(payloadType = SiteModel.class)
    FETCH_CURRENT_THEME,
    @Action(payloadType = SiteThemePayload.class)
    ACTIVATE_THEME,
    @Action(payloadType = SiteThemePayload.class)
    INSTALL_THEME,

    // Remote responses
    @Action(payloadType = FetchedWpComThemesPayload.class)
    FETCHED_WP_COM_THEMES,
    @Action(payloadType = FetchedCurrentThemePayload.class)
    FETCHED_CURRENT_THEME,
    @Action(payloadType = SiteThemePayload.class)
    ACTIVATED_THEME,
    @Action(payloadType = SiteThemePayload.class)
    INSTALLED_THEME
}
