package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.ConfiguredSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.FetchSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.FetchedSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.InstalledSitePluginPayload;

@ActionEnum
public enum PluginAction implements IAction {
    // Remote actions
    @Action(payloadType = ConfigureSitePluginPayload.class)
    CONFIGURE_SITE_PLUGIN,
    @Action(payloadType = FetchSitePluginPayload.class)
    FETCH_SITE_PLUGIN,
    @Action(payloadType = InstallSitePluginPayload.class)
    INSTALL_SITE_PLUGIN,

    // Remote responses
    @Action(payloadType = ConfiguredSitePluginPayload.class)
    CONFIGURED_SITE_PLUGIN,
    @Action(payloadType = FetchedSitePluginPayload.class)
    FETCHED_SITE_PLUGIN,
    @Action(payloadType = InstalledSitePluginPayload.class)
    INSTALLED_SITE_PLUGIN,
    @Action(payloadType = InstalledSitePluginPayload.class)
    INSTALLED_JP_FOR_INDIVIDUAL_PLUGIN_SITE,
}
