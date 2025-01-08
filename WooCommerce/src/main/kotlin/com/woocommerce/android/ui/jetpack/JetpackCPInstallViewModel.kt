package com.woocommerce.android.ui.jetpack

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginActivated
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginActivationFailed
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginInstallFailed
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginInstalled
import com.woocommerce.android.ui.jetpack.JetpackCPInstallViewModel.FailureType.ACTIVATION
import com.woocommerce.android.ui.jetpack.JetpackCPInstallViewModel.FailureType.CONNECTION
import com.woocommerce.android.ui.jetpack.JetpackCPInstallViewModel.FailureType.INSTALLATION
import com.woocommerce.android.ui.jetpack.JetpackCPInstallViewModel.InstallStatus.Activating
import com.woocommerce.android.ui.jetpack.JetpackCPInstallViewModel.InstallStatus.Connecting
import com.woocommerce.android.ui.jetpack.JetpackCPInstallViewModel.InstallStatus.Failed
import com.woocommerce.android.ui.jetpack.JetpackCPInstallViewModel.InstallStatus.Finished
import com.woocommerce.android.ui.jetpack.JetpackCPInstallViewModel.InstallStatus.Installing
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

@HiltViewModel
class JetpackCPInstallViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: PluginRepository,
    private val selectedSite: SelectedSite,
    private val siteStore: SiteStore
) : ScopedViewModel(savedState) {
    companion object {
        const val JETPACK_SLUG = "jetpack"
        const val JETPACK_NAME = "jetpack/jetpack"
        const val CONNECTION_ERROR = "Connection error."
        const val ATTEMPT_LIMIT = 2
        const val SYNC_CHECK_DELAY = 3000L
    }

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateLiveData = LiveDataDelegate(savedState, JetpackInstallProgressViewState())
    private var viewState by viewStateLiveData

    init {
        viewState = viewState.copy(
            installStatus = Installing
        )

        installJetpackPlugin()
    }

    private fun installJetpackPlugin() {
        launch {
            repository.installPlugin(selectedSite.get(), JETPACK_SLUG, JETPACK_NAME).collect {
                when (it) {
                    is PluginInstalled -> {
                        viewState = viewState.copy(installStatus = Activating)
                    }

                    is PluginInstallFailed -> {
                        AnalyticsTracker.track(
                            AnalyticsEvent.JETPACK_INSTALL_FAILED,
                            errorContext = this@JetpackCPInstallViewModel.javaClass.simpleName,
                            errorType = it.errorType,
                            errorDescription = it.errorDescription
                        )
                        viewState = viewState.copy(installStatus = Failed(INSTALLATION, it.errorDescription))
                    }

                    is PluginActivated -> {
                        AnalyticsTracker.track(AnalyticsEvent.JETPACK_INSTALL_SUCCEEDED)
                        checkJetpackConnection()
                    }

                    is PluginActivationFailed -> {
                        AnalyticsTracker.track(
                            AnalyticsEvent.JETPACK_INSTALL_FAILED,
                            errorContext = this@JetpackCPInstallViewModel.javaClass.simpleName,
                            errorType = it.errorType,
                            errorDescription = it.errorDescription
                        )
                        viewState = viewState.copy(installStatus = Failed(ACTIVATION, it.errorDescription))
                    }
                }
            }
        }
    }

    fun checkJetpackConnection(retry: Boolean = false) {
        launch {
            viewState = viewState.copy(installStatus = Connecting(retry))
            val isJetpackConnected = isJetpackConnectedAfterInstallation()
            viewState = if (isJetpackConnected) {
                viewState.copy(installStatus = Finished)
            } else {
                viewState.copy(installStatus = Failed(CONNECTION, CONNECTION_ERROR))
            }
        }
    }

    // After Jetpack-the-plugin is installed and activated on the site via the app, it will do a site sync.
    // The app needs the sync to be finished before the entire installation is considered finished and the site
    // can be used as a full WooCommerce site in the app.
    private suspend fun isJetpackConnectedAfterInstallation(): Boolean {
        var attempt = 0
        while (attempt < ATTEMPT_LIMIT) {
            val siteId = selectedSite.get().siteId
            val resultIsNotError = siteStore.fetchSite(selectedSite.get()).let { !it.isError }
            if (resultIsNotError) {
                val syncedSite = siteStore.getSiteBySiteId(siteId)
                if (syncedSite?.isJetpackConnected == true && syncedSite.hasWooCommerce) {
                    selectedSite.set(syncedSite)
                    return true
                } else {
                    attempt++
                    delay(SYNC_CHECK_DELAY)
                }
            } else {
                attempt++
            }
        }
        return false
    }

    @Parcelize
    data class JetpackInstallProgressViewState(
        val installStatus: InstallStatus? = null
    ) : Parcelable

    sealed class InstallStatus : Parcelable {
        @Parcelize
        object Installing : InstallStatus()

        @Parcelize
        object Activating : InstallStatus()

        @Parcelize
        data class Connecting(val retry: Boolean = false) : InstallStatus()

        @Parcelize
        object Finished : InstallStatus()

        @Parcelize
        data class Failed(val errorType: FailureType, val errorDescription: String) : InstallStatus()
    }

    enum class FailureType {
        INSTALLATION,
        ACTIVATION,
        CONNECTION
    }
}
