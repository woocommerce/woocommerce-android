package com.woocommerce.android.ui.common

import android.os.Parcelable
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginActivated
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginActivationFailed
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginInstallFailed
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginInstalled
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.dispatchAndAwait
import com.woocommerce.android.util.observeEvents
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.transformWhile
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PluginActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType.SITE
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.PluginStore
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.FetchPluginDirectoryPayload
import org.wordpress.android.fluxc.store.PluginStore.FetchSitePluginErrorType
import org.wordpress.android.fluxc.store.PluginStore.FetchSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginErrorType
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginFetched
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginInstalled
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class PluginRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused") private val pluginStore: PluginStore,
    private val wooCommerceStore: WooCommerceStore
) {
    companion object {
        private const val GENERIC_ERROR = "Unknown issue."
        private const val ATTEMPT_LIMIT = 2
        private const val DELAY_BEFORE_RETRY = 500L
    }

    suspend fun fetchPlugin(site: SiteModel, name: String): Result<SitePluginModel?> {
        WooLog.d(WooLog.T.PLUGINS, "Fetching plugin $name")

        val action = PluginActionBuilder.newFetchSitePluginAction(
            FetchSitePluginPayload(site, name)
        )
        val event: OnSitePluginFetched = dispatcher.dispatchAndAwait(action)
        return when {
            !event.isError ||
                event.error?.type == FetchSitePluginErrorType.PLUGIN_DOES_NOT_EXIST -> {
                WooLog.d(
                    WooLog.T.PLUGINS,
                    "Fetching plugin succeeded, plugin is:" +
                        if (event.plugin != null) "Installed" else "Not Installed"
                )
                Result.success(event.plugin)
            }

            else -> {
                WooLog.w(
                    WooLog.T.PLUGINS,
                    "Fetching plugin failed, ${event.error.type} ${event.error.message}"
                )
                Result.failure(OnChangedException(event.error))
            }
        }
    }

    suspend fun fetchInstalledPlugins(site: SiteModel): Result<List<ImmutablePluginModel>> {
        val plugins = mutableListOf<ImmutablePluginModel>()
        var loadMore = false
        do {
            val payload = FetchPluginDirectoryPayload(SITE, site, loadMore)
            val action = PluginActionBuilder.newFetchPluginDirectoryAction(payload)
            val event: PluginStore.OnPluginDirectoryFetched = dispatcher.dispatchAndAwait(action)
            if (event.isError) {
                WooLog.w(WooLog.T.PLUGINS, "Fetching installed plugins failed, ${event.error.type} ${event.error.message}")
                return Result.failure(OnChangedException(event.error))
            } else {
                val list = pluginStore.getPluginDirectory(site, SITE)
                plugins.addAll(list)
                loadMore = !event.isError && event.canLoadMore
            }
        } while (loadMore)
        return Result.success(plugins)
    }

    @Suppress("LongMethod")
    fun installPlugin(site: SiteModel, slug: String, name: String): Flow<PluginStatus> {
        return flow {
            WooLog.d(WooLog.T.PLUGINS, "Installing plugin Slug: $slug, Name: $name")
            val plugin = fetchPlugin(site, name).getOrNull()

            if (plugin != null) {
                if (plugin.isActive) {
                    // Plugin is already active, nothing to do
                    WooLog.d(WooLog.T.PLUGINS, "Plugin $slug is already installed and activated")
                    emit(PluginInstalled(slug))
                    emit(PluginActivated(slug))
                    return@flow
                } else {
                    // Plugin is already installed, proceed to activation
                    WooLog.d(WooLog.T.PLUGINS, "Plugin $slug is already installed, proceed to activation")
                    emit(PluginInstalled(slug))
                    dispatchPluginActivationAction(site, slug, name)
                }
            } else {
                // This request will automatically proceed to activating the plugin after the installation
                val payload = InstallSitePluginPayload(site, slug)
                dispatcher.dispatch(PluginActionBuilder.newInstallSitePluginAction(payload))
            }

            val installationEvents = dispatcher.observeInstallationEvents(slug)
                .catch { exception ->
                    val installationError = (exception as? OnChangedException)?.error as? InstallSitePluginError
                    if (installationError?.type == InstallSitePluginErrorType.PLUGIN_ALREADY_INSTALLED) {
                        // The plugin is already installed, this can happen if the plugin fetch failed earlier
                        WooLog.d(WooLog.T.PLUGINS, "Plugin $slug is already installed, proceed to activation")
                        dispatchPluginActivationAction(site, slug, name)
                        emit(PluginInstalled(slug))
                    } else {
                        throw exception
                    }
                }
            val activationEvents = dispatcher.observeActivationEvents(slug)

            emitAll(merge(installationEvents, activationEvents))
        }.retryWhen { cause, attempt ->
            (cause is OnChangedException && attempt < ATTEMPT_LIMIT).also {
                if (it) {
                    delay(DELAY_BEFORE_RETRY)
                    WooLog.d(WooLog.T.PLUGINS, "Retry plugin installation")
                }
            }
        }.catch { cause ->
            if (cause !is OnChangedException) throw cause
            if (cause.error is InstallSitePluginError) {
                emit(
                    PluginInstallFailed(
                        errorDescription = cause.message ?: GENERIC_ERROR,
                        errorType = cause.error.type.name,
                        errorCode = cause.error.errorCode
                    )
                )
            } else if (cause.error is ConfigureSitePluginError) {
                emit(
                    PluginActivationFailed(
                        errorDescription = cause.message ?: GENERIC_ERROR,
                        errorType = cause.error.type.name,
                        errorCode = cause.error.errorCode
                    )
                )
            }
        }.transformWhile { status ->
            emit(status)
            // Finish the flow unless it's an intermediary event: Installation
            status is PluginInstalled
        }
    }

    suspend fun getPluginsInfo(site: SiteModel, plugins: List<WooCommerceStore.WooPlugin>): Map<String, WooPlugin> {
        val result = HashMap<String, WooPlugin>()
        val information = wooCommerceStore.getSitePlugins(site, plugins).associateBy { it.name }

        if (information.isEmpty()) {
            AnalyticsTracker.track(AnalyticsEvent.PLUGINS_NOT_SYNCED_YET)
            // return earlier, no plugins info in the database
            return result
        }

        plugins.associateByTo(
            destination = result,
            keySelector = { plugin -> plugin.pluginName },
            valueTransform = { plugin ->
                val info = information[plugin.pluginName]
                WooPlugin(info != null, info?.isActive ?: false, info?.version)
            }
        )
        return result
    }

    private fun dispatchPluginActivationAction(site: SiteModel, slug: String, name: String) {
        val payload = ConfigureSitePluginPayload(
            site,
            name,
            slug,
            true,
            true
        )
        dispatcher.dispatch(PluginActionBuilder.newConfigureSitePluginAction(payload))
    }

    private fun Dispatcher.observeInstallationEvents(slug: String): Flow<PluginStatus> =
        observeEvents<OnSitePluginInstalled>()
            .map { event ->
                if (!event.isError) {
                    WooLog.d(WooLog.T.PLUGINS, "Plugin $slug installed successfully")
                    PluginInstalled(event.slug)
                } else {
                    WooLog.w(WooLog.T.PLUGINS, "Installation failed for plugin $slug, ${event.error.type}")
                    throw OnChangedException(
                        event.error,
                        event.error.message
                    )
                }
            }

    private fun Dispatcher.observeActivationEvents(slug: String): Flow<PluginStatus> =
        observeEvents<OnSitePluginConfigured>()
            .map { event ->
                if (!event.isError) {
                    WooLog.d(WooLog.T.PLUGINS, "Plugin $slug activated successfully")
                    PluginActivated(event.pluginName)
                } else {
                    WooLog.w(WooLog.T.PLUGINS, "Activation failed for plugin $slug, ${event.error.type}")
                    throw OnChangedException(
                        event.error,
                        event.error.message
                    )
                }
            }

    sealed class PluginStatus : Parcelable {
        @Parcelize
        data class PluginInstalled(val slug: String) : PluginStatus()

        @Parcelize
        data class PluginInstallFailed(
            val errorDescription: String,
            val errorType: String,
            val errorCode: Int?
        ) : PluginStatus()

        @Parcelize
        data class PluginActivated(val name: String) : PluginStatus()

        @Parcelize
        data class PluginActivationFailed(
            val errorDescription: String,
            val errorType: String,
            val errorCode: Int?
        ) : PluginStatus()
    }
}
