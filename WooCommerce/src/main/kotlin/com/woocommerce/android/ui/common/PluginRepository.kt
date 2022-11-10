package com.woocommerce.android.ui.common

import android.os.Parcelable
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginActivated
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginActivationFailed
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginInstallFailed
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginInstalled
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
import org.wordpress.android.fluxc.store.PluginStore.FetchJetpackSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.OnJetpackSitePluginFetched
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginInstalled
import javax.inject.Inject

class PluginRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    @Suppress("unused") private val pluginStore: PluginStore
) {
    companion object {
        const val GENERIC_ERROR = "Unknown issue."
        const val ATTEMPT_LIMIT = 2
    }

    fun installPlugin(slug: String, name: String): Flow<PluginStatus> = flow {
        val plugin = fetchJetpackSitePlugin(name)

        if (plugin != null) {
            // Plugin is already installed, proceed to activation
            emit(PluginInstalled(slug, selectedSite.get()))
            val payload = ConfigureSitePluginPayload(
                selectedSite.get(),
                name,
                slug,
                true,
                true
            )
            dispatcher.dispatch(PluginActionBuilder.newConfigureSitePluginAction(payload))
        } else {
            // This request will automatically proceed to activating the plugin after the installation
            val payload = InstallSitePluginPayload(selectedSite.get(), slug)
            dispatcher.dispatch(PluginActionBuilder.newInstallSitePluginAction(payload))
        }

        val installationEvents = dispatcher.observeEvents<OnSitePluginInstalled>()
            .map { event ->
                if (!event.isError) {
                    PluginInstalled(event.slug, event.site)
                } else {
                    throw OnChangedException(
                        event.error,
                        event.error.message
                    )
                }
            }
        val activationEvents = dispatcher.observeEvents<OnSitePluginConfigured>()
            .map { event ->
                if (!event.isError) {
                    // If there is no error, we can assume that the configuration (activating in our case) is successful.
                    PluginActivated(event.pluginName, event.site)
                } else {
                    throw OnChangedException(
                        event.error,
                        event.error.message
                    )
                }
            }

        emitAll(merge(installationEvents, activationEvents))
    }.retryWhen { cause, attempt ->
        cause is OnChangedException && attempt < ATTEMPT_LIMIT
    }.catch { cause ->
        if (cause !is OnChangedException) throw cause
        if (cause.error is InstallSitePluginError) {
            emit(
                PluginInstallFailed(
                    errorDescription = cause.message ?: GENERIC_ERROR,
                    errorType = cause.error.type.name
                )
            )
        } else if (cause.error is ConfigureSitePluginError) {
            emit(
                PluginActivationFailed(
                    errorDescription = cause.message ?: GENERIC_ERROR,
                    errorType = cause.error.type.name
                )
            )
        }
    }

    private suspend fun fetchJetpackSitePlugin(name: String): SitePluginModel? {
        return if (selectedSite.exists()) {
            val action = PluginActionBuilder.newFetchJetpackSitePluginAction(
                FetchJetpackSitePluginPayload(selectedSite.get(), name)
            )
            val event: OnJetpackSitePluginFetched = dispatcher.dispatchAndAwait(action)
            return when {
                !event.isError -> event.plugin
                else -> null
            }
        } else null
    }

    sealed class PluginStatus : Parcelable {
        @Parcelize
        data class PluginInstalled(val slug: String, val site: SiteModel) : PluginStatus()

        @Parcelize
        data class PluginInstallFailed(val errorDescription: String, val errorType: String) : PluginStatus()

        @Parcelize
        data class PluginActivated(val name: String, val site: SiteModel) : PluginStatus()

        @Parcelize
        data class PluginActivationFailed(val errorDescription: String, val errorType: String) : PluginStatus()
    }
}
