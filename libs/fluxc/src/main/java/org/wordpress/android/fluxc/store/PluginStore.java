package org.wordpress.android.fluxc.store;

import androidx.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.PluginAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginJetpackTunnelRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient;
import org.wordpress.android.fluxc.persistence.PluginSqlUtilsWrapper;
import org.wordpress.android.util.AppLog;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PluginStore extends Store {
    // Request payloads
    @SuppressWarnings("WeakerAccess")
    public static class ConfigureSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public String pluginName;
        public String slug;
        public boolean isActive;
        public boolean isAutoUpdateEnabled;

        public ConfigureSitePluginPayload(SiteModel site, String pluginName, String slug, boolean isActive,
                                          boolean isAutoUpdateEnabled) {
            this.site = site;
            this.pluginName = pluginName;
            this.slug = slug;
            this.isActive = isActive;
            this.isAutoUpdateEnabled = isAutoUpdateEnabled;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class FetchSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public String pluginName;

        public FetchSitePluginPayload(SiteModel site, String pluginName) {
            this.site = site;
            this.pluginName = pluginName;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class InstallSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public String slug;

        public InstallSitePluginPayload(SiteModel site, String slug) {
            this.site = site;
            this.slug = slug;
        }
    }

    // Response payloads

    @SuppressWarnings("WeakerAccess")
    public static class ConfiguredSitePluginPayload extends Payload<ConfigureSitePluginError> {
        public SiteModel site;
        public String pluginName;
        public String slug;
        public SitePluginModel plugin;

        public ConfiguredSitePluginPayload(SiteModel site, SitePluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
            this.pluginName = this.plugin.getName();
            this.slug = this.plugin.getSlug();
        }

        public ConfiguredSitePluginPayload(SiteModel site, String pluginName, ConfigureSitePluginError error) {
            this.site = site;
            this.pluginName = pluginName;
            this.error = error;
        }

        public ConfiguredSitePluginPayload(SiteModel site, String pluginName, String slug,
                                           ConfigureSitePluginError error) {
            this.site = site;
            this.pluginName = pluginName;
            this.slug = slug;
            this.error = error;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class FetchedSitePluginPayload extends Payload<FetchSitePluginError> {
        public SitePluginModel plugin;
        public String pluginName;

        public FetchedSitePluginPayload(SitePluginModel plugin) {
            this.plugin = plugin;
        }

        public FetchedSitePluginPayload(String pluginName, FetchSitePluginError error) {
            this.pluginName = pluginName;
            this.error = error;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class InstalledSitePluginPayload extends Payload<InstallSitePluginError> {
        public SiteModel site;
        public String slug;
        public SitePluginModel plugin;

        public InstalledSitePluginPayload(SiteModel site, SitePluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
            this.slug = this.plugin.getSlug();
        }

        public InstalledSitePluginPayload(SiteModel site, String slug, InstallSitePluginError error) {
            this.site = site;
            this.slug = slug;
            this.error = error;
        }
    }

    // Errors

    public static class ConfigureSitePluginError implements OnChangedError {
        public ConfigureSitePluginErrorType type;
        @Nullable public Integer errorCode;
        @Nullable public String message;

        ConfigureSitePluginError(ConfigureSitePluginErrorType type) {
            this.type = type;
        }

        public ConfigureSitePluginError(String type, @Nullable String message) {
            this.type = ConfigureSitePluginErrorType.fromString(type);
            this.message = message;
        }

        public ConfigureSitePluginError(BaseNetworkError error, boolean isActivating) {
            this.type = ConfigureSitePluginErrorType.fromGenericErrorType(error.type, isActivating);
            this.message = error.message;
            if (error.hasVolleyError() && error.volleyError.networkResponse != null) {
                this.errorCode = error.volleyError.networkResponse.statusCode;
            }
        }
    }

    public static class FetchSitePluginError implements OnChangedError {
        public FetchSitePluginErrorType type;
        @Nullable public String message;

        public FetchSitePluginError(FetchSitePluginErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
        }

        public FetchSitePluginError(GenericErrorType type, @Nullable String message) {
            this.type = FetchSitePluginErrorType.fromGenericErrorType(type);
            this.message = message;
        }
    }

    public static class InstallSitePluginError implements OnChangedError {
        public InstallSitePluginErrorType type;
        @Nullable public Integer errorCode;
        @Nullable public String message;

        InstallSitePluginError(InstallSitePluginErrorType type) {
            this(type, null);
        }

        InstallSitePluginError(InstallSitePluginErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
        }

        public InstallSitePluginError(String type, @Nullable String message) {
            this.type = InstallSitePluginErrorType.fromString(type);
            this.message = message;
        }

        public InstallSitePluginError(BaseNetworkError error) {
            this.type = InstallSitePluginErrorType.fromNetworkError(error);
            this.message = error.message;
            if (error.hasVolleyError() && error.volleyError.networkResponse != null) {
                this.errorCode = error.volleyError.networkResponse.statusCode;
            }
        }
    }

    // Error types

    public enum ConfigureSitePluginErrorType {
        GENERIC_ERROR,
        ACTIVATION_ERROR,
        DEACTIVATION_ERROR,
        NOT_AVAILABLE, // Return for non-jetpack sites
        UNAUTHORIZED,
        UNKNOWN_PLUGIN;

        public static ConfigureSitePluginErrorType fromString(String string) {
            if (string != null) {
                for (ConfigureSitePluginErrorType v : ConfigureSitePluginErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }

        public static ConfigureSitePluginErrorType fromGenericErrorType(GenericErrorType genericErrorType,
                                                                        boolean isActivating) {
            if (genericErrorType != null) {
                switch (genericErrorType) {
                    case TIMEOUT:
                    case NO_CONNECTION:
                    case NETWORK_ERROR:
                    case SERVER_ERROR:
                        if (isActivating) {
                            return ACTIVATION_ERROR;
                        } else {
                            return DEACTIVATION_ERROR;
                        }
                    case NOT_FOUND:
                    case CENSORED:
                        return UNKNOWN_PLUGIN;
                    case INVALID_SSL_CERTIFICATE:
                    case HTTP_AUTH_ERROR:
                    case AUTHORIZATION_REQUIRED:
                    case NOT_AUTHENTICATED:
                        return UNAUTHORIZED;
                    case INVALID_RESPONSE:
                    case PARSE_ERROR:
                    case UNKNOWN:
                        return GENERIC_ERROR;
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum FetchSitePluginErrorType {
        UNAUTHORIZED,
        NOT_AVAILABLE,
        EMPTY_RESPONSE,
        GENERIC_ERROR,
        PLUGIN_DOES_NOT_EXIST;

        public static FetchSitePluginErrorType fromGenericErrorType(GenericErrorType genericErrorType) {
            if (genericErrorType != null) {
                switch (genericErrorType) {
                    case INVALID_SSL_CERTIFICATE:
                    case HTTP_AUTH_ERROR:
                    case AUTHORIZATION_REQUIRED:
                    case NOT_AUTHENTICATED:
                        return UNAUTHORIZED;
                    case NOT_FOUND:
                        return PLUGIN_DOES_NOT_EXIST;
                    case NO_CONNECTION:
                    case TIMEOUT:
                    case NETWORK_ERROR:
                    case SERVER_ERROR:
                    case CENSORED:
                    case INVALID_RESPONSE:
                    case PARSE_ERROR:
                    case UNKNOWN:
                        return GENERIC_ERROR;
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum InstallSitePluginErrorType {
        GENERIC_ERROR,
        INSTALL_FAILURE,
        LOCAL_FILE_DOES_NOT_EXIST,
        NO_PACKAGE,
        NO_PLUGIN_INSTALLED,
        NOT_AVAILABLE, // Return for non-jetpack sites
        PLUGIN_ALREADY_INSTALLED,
        UNAUTHORIZED;

        private static final String PLUGIN_ALREADY_EXISTS = "Destination folder already exists.";

        public static InstallSitePluginErrorType fromString(String string) {
            if (string != null) {
                if (string.equalsIgnoreCase("local-file-does-not-exist")) {
                    return LOCAL_FILE_DOES_NOT_EXIST;
                }
                for (InstallSitePluginErrorType v : InstallSitePluginErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }

        public static InstallSitePluginErrorType fromNetworkError(BaseNetworkError error) {
            if (PLUGIN_ALREADY_EXISTS.equalsIgnoreCase(error.message)) {
                return PLUGIN_ALREADY_INSTALLED;
            }
            GenericErrorType genericErrorType = error.type;
            if (genericErrorType != null) {
                switch (genericErrorType) {
                    case TIMEOUT:
                    case NO_CONNECTION:
                    case NETWORK_ERROR:
                    case SERVER_ERROR:
                        return INSTALL_FAILURE;
                    case NOT_FOUND:
                    case CENSORED:
                        return NO_PLUGIN_INSTALLED;
                    case INVALID_SSL_CERTIFICATE:
                    case HTTP_AUTH_ERROR:
                    case AUTHORIZATION_REQUIRED:
                    case NOT_AUTHENTICATED:
                        return UNAUTHORIZED;
                    case INVALID_RESPONSE:
                    case PARSE_ERROR:
                    case UNKNOWN:
                        return GENERIC_ERROR;
                }
            }
            return GENERIC_ERROR;
        }
    }

    // OnChanged Events

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginConfigured extends OnChanged<ConfigureSitePluginError> {
        public SiteModel site;
        public String pluginName;
        public String slug;

        public OnSitePluginConfigured(SiteModel site, String pluginName, String slug) {
            this.site = site;
            this.pluginName = pluginName;
            this.slug = slug;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginInstalled extends OnChanged<InstallSitePluginError> {
        public SiteModel site;
        public String slug;

        public OnSitePluginInstalled(SiteModel site, String slug) {
            this.site = site;
            this.slug = slug;
        }
    }

    public static class OnSitePluginFetched extends OnChanged<FetchSitePluginError> {
        public SitePluginModel plugin;
        public String pluginName;

        public OnSitePluginFetched(FetchedSitePluginPayload payload) {
            this.plugin = payload.plugin;
            this.pluginName = payload.pluginName;
        }
    }

    private final PluginRestClient mPluginRestClient;
    private final PluginCoroutineStore mPluginCoroutineStore;
    private final PluginJetpackTunnelRestClient mPluginJetpackTunnelRestClient;

    private final PluginSqlUtilsWrapper mPluginSqlUtils;

    @Inject public PluginStore(Dispatcher dispatcher,
                               PluginRestClient pluginRestClient,
                               PluginCoroutineStore pluginCoroutineStore,
                               PluginJetpackTunnelRestClient pluginJetpackTunnelRestClient,
                               PluginSqlUtilsWrapper pluginSqlUtils) {
        super(dispatcher);
        mPluginRestClient = pluginRestClient;
        mPluginCoroutineStore = pluginCoroutineStore;
        mPluginJetpackTunnelRestClient = pluginJetpackTunnelRestClient;
        mPluginSqlUtils = pluginSqlUtils;
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.API, "PluginStore onRegister");
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof PluginAction)) {
            return;
        }
        switch ((PluginAction) actionType) {
            // Remote actions
            case CONFIGURE_SITE_PLUGIN:
                configureSitePlugin((ConfigureSitePluginPayload) action.getPayload());
                break;
            case FETCH_SITE_PLUGIN:
                fetchSitePlugin((FetchSitePluginPayload) action.getPayload());
                break;
            case INSTALL_SITE_PLUGIN:
                installSitePlugin((InstallSitePluginPayload) action.getPayload());
                break;
            // Network callbacks
            case CONFIGURED_SITE_PLUGIN:
                configuredSitePlugin((ConfiguredSitePluginPayload) action.getPayload());
                break;
            case FETCHED_SITE_PLUGIN:
                fetchedSitePlugin((FetchedSitePluginPayload) action.getPayload());
                break;
            case INSTALLED_SITE_PLUGIN:
                installedSitePlugin((InstalledSitePluginPayload) action.getPayload());
                break;
            case INSTALLED_JP_FOR_INDIVIDUAL_PLUGIN_SITE:
                installedJPForIndividualPluginSite((InstalledSitePluginPayload) action.getPayload());
                break;
        }
    }

    // Remote actions

    private void configureSitePlugin(ConfigureSitePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.configureSitePlugin(payload.site, payload.pluginName, payload.slug, payload.isActive,
                    payload.isAutoUpdateEnabled);
        } else if (payload.site.isJetpackCPConnected()) {
            mPluginJetpackTunnelRestClient.configurePlugin(payload.site, payload.pluginName, payload.isActive);
        } else if (!payload.site.isUsingWpComRestApi()) {
            mPluginCoroutineStore.configureSitePlugin(payload.site, payload.pluginName, payload.slug, payload.isActive);
        } else {
            ConfigureSitePluginError error = new ConfigureSitePluginError(ConfigureSitePluginErrorType.NOT_AVAILABLE);
            ConfiguredSitePluginPayload errorPayload = new ConfiguredSitePluginPayload(payload.site, payload.slug,
                    payload.pluginName, error);
            mDispatcher.dispatch(PluginActionBuilder.newConfiguredSitePluginAction(errorPayload));
        }
    }

    /* Fetch a single plugin from a site, to get its information and whether it exists or not.
       Currently this is only supported on sites connected using Jetpack plugin or Jetpack Connection Package.
     */
    private void fetchSitePlugin(FetchSitePluginPayload payload) {
        if (payload.site.isJetpackConnected() || payload.site.isJetpackCPConnected()) {
            mPluginJetpackTunnelRestClient.fetchPlugin(payload.site, payload.pluginName);
        } else if (!payload.site.isUsingWpComRestApi()) {
            mPluginCoroutineStore.fetchWPApiPlugin(payload.site, payload.pluginName);
        } else {
            FetchSitePluginError error = new FetchSitePluginError(FetchSitePluginErrorType.NOT_AVAILABLE, null);
            FetchedSitePluginPayload errorPayload =
                    new FetchedSitePluginPayload(payload.pluginName, error);
            mDispatcher.dispatch(PluginActionBuilder.newFetchedSitePluginAction(errorPayload));
        }
    }

    private void installSitePlugin(InstallSitePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.installSitePlugin(payload.site, payload.slug);
        } else if (payload.site.isJetpackCPConnected()) {
            mPluginJetpackTunnelRestClient.installPlugin(payload.site, payload.slug);
        } else if (!payload.site.isUsingWpComRestApi()) {
            mPluginCoroutineStore.installSitePlugin(payload.site, payload.slug);
        } else {
            InstallSitePluginError error = new InstallSitePluginError(InstallSitePluginErrorType.NOT_AVAILABLE);
            InstalledSitePluginPayload errorPayload = new InstalledSitePluginPayload(payload.site,
                    payload.slug, error);
            mDispatcher.dispatch(PluginActionBuilder.newInstalledSitePluginAction(errorPayload));
        }
    }

    // Network callbacks

    private void configuredSitePlugin(ConfiguredSitePluginPayload payload) {
        OnSitePluginConfigured event = new OnSitePluginConfigured(payload.site, payload.pluginName, payload.slug);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            mPluginSqlUtils.insertOrUpdateSitePlugin(payload.site, payload.plugin);
        }
        emitChange(event);
    }

    private void fetchedSitePlugin(FetchedSitePluginPayload payload) {
        OnSitePluginFetched event = new OnSitePluginFetched(payload);
        if (payload.isError()) {
            event.error = payload.error;
        }
        emitChange(event);
    }

    private void installedSitePlugin(InstalledSitePluginPayload payload) {
        emitPluginInstalledEvent(payload);
        // Once the plugin is installed activate it and enable auto-updates
        if (!payload.isError() && payload.plugin != null) {
            try {
                // Give a second to the server as otherwise the following configure call may fail
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // https://www.javaspecialists.eu/archive/Issue056-Shutting-down-Threads-Cleanly.html
                Thread.currentThread().interrupt();
            }

            ConfigureSitePluginPayload configurePayload = new ConfigureSitePluginPayload(payload.site,
                    payload.plugin.getName(), payload.plugin.getSlug(), true, true);
            mDispatcher.dispatch(PluginActionBuilder.newConfigureSitePluginAction(configurePayload));
        }
    }

    private void emitPluginInstalledEvent(InstalledSitePluginPayload payload) {
        OnSitePluginInstalled event = new OnSitePluginInstalled(payload.site, payload.slug);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            mPluginSqlUtils.insertOrUpdateSitePlugin(payload.site, payload.plugin);
        }
        emitChange(event);
    }

    private void installedJPForIndividualPluginSite(InstalledSitePluginPayload payload) {
        emitPluginInstalledEvent(payload);
    }
}
