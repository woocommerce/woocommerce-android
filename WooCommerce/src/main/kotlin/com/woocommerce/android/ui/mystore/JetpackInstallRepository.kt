package com.woocommerce.android.ui.mystore

import android.os.Parcelable
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PluginActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PluginStore
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginInstalled
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured
import javax.inject.Inject
import com.woocommerce.android.ui.mystore.JetpackInstallRepository.PluginStatus.PluginInstalled
import com.woocommerce.android.ui.mystore.JetpackInstallRepository.PluginStatus.PluginInstallFailed
import com.woocommerce.android.ui.mystore.JetpackInstallRepository.PluginStatus.PluginActivated
import com.woocommerce.android.ui.mystore.JetpackInstallRepository.PluginStatus.PluginActivationFailed
import kotlinx.coroutines.channels.awaitClose
import kotlinx.parcelize.Parcelize

class JetpackInstallRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite
) {
    companion object {
        const val JETPACK_SLUG = "wp-category-meta" // example plugin, should be jetpack
        const val JETPACK_NAME = "jetpack/jetpack"
        const val GENERIC_ERROR = "Unknown issue."
    }

    fun installJetpackPlugin() = callbackFlow<PluginStatus> {
        val payload = PluginStore.InstallSitePluginPayload(selectedSite.get(), JETPACK_SLUG)
        dispatcher.dispatch(PluginActionBuilder.newInstallSitePluginAction(payload))

        val listener = PluginActionListener(this)
        dispatcher.register(listener)

        awaitClose {
            dispatcher.unregister(listener)
        }
    }

    fun fetchJetpackPlugin() {
        val payload = PluginStore.FetchJetpackSitePluginPayload(selectedSite.get(), JETPACK_NAME)
        dispatcher.dispatch(PluginActionBuilder.newFetchJetpackSitePluginAction(payload))
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

    sealed class PluginStatus : Parcelable {
        @Parcelize
        data class PluginInstalled(val slug: String, val site: SiteModel) : PluginStatus()
        @Parcelize
        data class PluginInstallFailed(val error: String) : PluginStatus()
        @Parcelize
        data class PluginActivated(val name: String, val site: SiteModel) : PluginStatus()
        @Parcelize
        data class PluginActivationFailed(val error: String) : PluginStatus()
        @Parcelize
        object PluginInstallStopped: PluginStatus()
    }
}
