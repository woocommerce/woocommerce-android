package com.woocommerce.android.ui.jetpack

import android.os.Parcelable
import com.woocommerce.android.AppConstants
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PluginActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject
import com.woocommerce.android.ui.jetpack.PluginRepository.PluginStatus.PluginInstalled
import com.woocommerce.android.ui.jetpack.PluginRepository.PluginStatus.PluginInstallFailed
import com.woocommerce.android.ui.jetpack.PluginRepository.PluginStatus.PluginActivated
import com.woocommerce.android.ui.jetpack.PluginRepository.PluginStatus.PluginActivationFailed
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.PluginStore
import org.wordpress.android.fluxc.store.PluginStore.*

class PluginRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    @Suppress("unused") private val pluginStore: PluginStore
) {
    companion object {
        const val GENERIC_ERROR = "Unknown issue."
    }

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    private var continuationFetchJetpackSitePlugin = ContinuationWrapper<SitePluginModel?>(WooLog.T.WP)

    // Note that the `newInstallSitePluginAction` action automatically tries to activate the plugin after
    // installation is successful, so when using this function, there's no need to call `activateJetpackPlugin()
    // separately.
    fun installPlugin(slug: String) = callbackFlow<PluginStatus> {
        val listener = PluginActionListener(this)
        dispatcher.register(listener)

        val payload = InstallSitePluginPayload(selectedSite.get(), slug)
        dispatcher.dispatch(PluginActionBuilder.newInstallSitePluginAction(payload))

        awaitClose {
            dispatcher.unregister(listener)
        }
    }

    suspend fun fetchJetpackSitePlugin(name: String): SitePluginModel? {
        return if (selectedSite.exists()) {
            val result = continuationFetchJetpackSitePlugin.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
                val payload = FetchJetpackSitePluginPayload(selectedSite.get(), name)
                dispatcher.dispatch(PluginActionBuilder.newFetchJetpackSitePluginAction(payload))
            }
            return when (result) {
                is ContinuationWrapper.ContinuationResult.Cancellation -> null
                is ContinuationWrapper.ContinuationResult.Success -> result.value
            }
        } else null
    }

    fun activatePlugin(name: String, slug: String, enableAutoUpdate: Boolean = true) {
        val payload = ConfigureSitePluginPayload(
            selectedSite.get(),
            name,
            slug,
            true,
            enableAutoUpdate
        )
        dispatcher.dispatch(PluginActionBuilder.newConfigureSitePluginAction(payload))
    }

    private inner class PluginActionListener(private val producerScope: ProducerScope<PluginStatus>) {
        @Suppress("unused")
        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onSitePluginInstalled(event: OnSitePluginInstalled) {
            if (!event.isError) {
                producerScope.trySendBlocking(
                    PluginInstalled(event.slug, event.site)
                )
            } else {
                producerScope.trySendBlocking(
                    PluginInstallFailed(event.error.message ?: GENERIC_ERROR)
                )
            }
        }

        @Suppress("unused")
        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onSitePluginConfigured(event: OnSitePluginConfigured) {
            if (!event.isError) {
                // If there is no error, we can assume that the configuration (activating in our case) is successful.
                producerScope.trySendBlocking(
                    PluginActivated(event.pluginName, event.site)
                )
            } else {
                producerScope.trySendBlocking(
                    PluginActivationFailed(event.error.message ?: GENERIC_ERROR)
                )
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSitePluginFetched(event: OnJetpackSitePluginFetched) {
        if (!event.isError) {
            continuationFetchJetpackSitePlugin.continueWith(event.plugin)
        } else {
            continuationFetchJetpackSitePlugin.continueWith(null)
        }
    }

    sealed class PluginStatus : Parcelable {
        @Parcelize
        data class PluginInstalled(val slug: String, val site: SiteModel) : PluginStatus()

        @Parcelize
        data class PluginInstallFailed(val error: String) : PluginStatus()

        @Parcelize
        data class PluginActivated(val name: String, val site: SiteModel) : PluginStatus()

        @Parcelize
        data class PluginActivationFailed(val error: String) : PluginStatus()
    }
}
