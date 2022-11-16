package com.woocommerce.android.ui.common

import android.os.Parcelable
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginActivated
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginActivationFailed
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginInstallFailed
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginInstalled
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.dispatchAndAwait
import com.woocommerce.android.util.observeEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.retryWhen
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PluginActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.PluginStore
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.FetchSitePluginErrorType
import org.wordpress.android.fluxc.store.PluginStore.FetchSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginErrorType
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginFetched
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginInstalled
import javax.inject.Inject

class PluginRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused") private val pluginStore: PluginStore
) {
    companion object {
        const val GENERIC_ERROR = "Unknown issue."
        const val ATTEMPT_LIMIT = 2
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

    fun installPlugin(site: SiteModel, slug: String, name: String): Flow<PluginStatus> {
        return flow {
            WooLog.d(WooLog.T.PLUGINS, "Installing plugin Slug: $slug, Name: $name")
            val plugin = fetchPlugin(site, name).getOrNull()

            if (plugin != null) {
                if (plugin.isActive) {
                    // Plugin is already active, nothing to do
                    WooLog.d(WooLog.T.PLUGINS, "Plugin $slug is already installed and activated")
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
                if (it) WooLog.d(WooLog.T.PLUGINS, "Retry plugin installation")
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
        }
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
