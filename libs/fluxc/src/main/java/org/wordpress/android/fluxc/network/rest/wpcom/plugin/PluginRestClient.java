package org.wordpress.android.fluxc.network.rest.wpcom.plugin;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginError;
import org.wordpress.android.fluxc.store.PluginStore.ConfiguredSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginError;
import org.wordpress.android.fluxc.store.PluginStore.InstalledSitePluginPayload;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class PluginRestClient extends BaseWPComRestClient {
    @Inject public PluginRestClient(Context appContext, Dispatcher dispatcher,
                            @Named("regular") RequestQueue requestQueue,
                            AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    public void configureSitePlugin(@NonNull final SiteModel site, @NonNull final String pluginName,
                                    @NonNull final String slug, boolean isActive, boolean isAutoUpdatesEnabled) {
        String url = WPCOMREST.sites.site(site.getSiteId()).plugins.name(getEncodedPluginName(pluginName)).getUrlV1_2();
        Map<String, Object> params = new HashMap<>();
        params.put("active", isActive);
        params.put("autoupdate", isAutoUpdatesEnabled);
        final WPComGsonRequest<PluginWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, params,
                PluginWPComRestResponse.class,
                new Listener<PluginWPComRestResponse>() {
                    @Override
                    public void onResponse(PluginWPComRestResponse response) {
                        SitePluginModel pluginFromResponse = pluginModelFromResponse(site, response);
                        mDispatcher.dispatch(PluginActionBuilder.newConfiguredSitePluginAction(
                                new ConfiguredSitePluginPayload(site, pluginFromResponse)));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError networkError) {
                        ConfigureSitePluginError configurePluginError = new ConfigureSitePluginError(
                                networkError.apiError, networkError.message);
                        if (networkError.hasVolleyError() && networkError.volleyError.networkResponse != null) {
                            configurePluginError.errorCode = networkError.volleyError.networkResponse.statusCode;
                        }
                        ConfiguredSitePluginPayload payload =
                                new ConfiguredSitePluginPayload(site, pluginName, slug, configurePluginError);
                        mDispatcher.dispatch(PluginActionBuilder.newConfiguredSitePluginAction(payload));
                    }
                }
        );
        add(request);
    }

    public void installSitePlugin(@NonNull final SiteModel site, final String pluginSlug) {
        String url = WPCOMREST.sites.site(site.getSiteId()).plugins.slug(pluginSlug).install.getUrlV1_2();
        final WPComGsonRequest<PluginWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                PluginWPComRestResponse.class,
                new Listener<PluginWPComRestResponse>() {
                    @Override
                    public void onResponse(PluginWPComRestResponse response) {
                        SitePluginModel pluginFromResponse = pluginModelFromResponse(site, response);
                        mDispatcher.dispatch(PluginActionBuilder.newInstalledSitePluginAction(
                                new InstalledSitePluginPayload(site, pluginFromResponse)));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError networkError) {
                        InstallSitePluginError installPluginError = new InstallSitePluginError(
                                networkError.apiError, networkError.message);
                        if (networkError.hasVolleyError() && networkError.volleyError.networkResponse != null) {
                            installPluginError.errorCode = networkError.volleyError.networkResponse.statusCode;
                        }
                        InstalledSitePluginPayload payload = new InstalledSitePluginPayload(site, pluginSlug,
                                installPluginError);
                        mDispatcher.dispatch(PluginActionBuilder.newInstalledSitePluginAction(payload));
                    }
                }
        );
        add(request);
    }

    private SitePluginModel pluginModelFromResponse(SiteModel siteModel, PluginWPComRestResponse response) {
        SitePluginModel sitePluginModel = new SitePluginModel();
        sitePluginModel.setLocalSiteId(siteModel.getId());
        sitePluginModel.setName(response.name);
        sitePluginModel.setDisplayName(StringEscapeUtils.unescapeHtml4(response.display_name));
        sitePluginModel.setAuthorName(StringEscapeUtils.unescapeHtml4(response.author));
        sitePluginModel.setAuthorUrl(response.author_url);
        sitePluginModel.setDescription(StringEscapeUtils.unescapeHtml4(response.description));
        sitePluginModel.setIsActive(response.active);
        sitePluginModel.setIsAutoUpdateEnabled(response.autoupdate);
        sitePluginModel.setPluginUrl(response.plugin_url);
        sitePluginModel.setSlug(response.slug);
        sitePluginModel.setVersion(response.version);
        if (response.action_links != null) {
            sitePluginModel.setSettingsUrl(response.action_links.settings);
        }
        return sitePluginModel;
    }

    private String getEncodedPluginName(String pluginName) {
        try {
            // We need to encode plugin name otherwise names like "akismet/akismet" would fail
            return URLEncoder.encode(pluginName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return pluginName;
        }
    }
}
