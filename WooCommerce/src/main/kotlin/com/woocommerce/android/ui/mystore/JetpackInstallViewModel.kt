package com.woocommerce.android.ui.mystore

import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.di.AppCoroutineScope
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.store.PluginStore.OnJetpackSitePluginFetched
import com.woocommerce.android.ui.mystore.PluginRepository.PluginStatus.*
import com.woocommerce.android.ui.mystore.JetpackInstallViewModel.InstallStatus.*
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.Dispatcher
import javax.inject.Inject

class JetpackInstallViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: PluginRepository,
    private val dispatcher: Dispatcher,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) : ScopedViewModel(savedState) {
    companion object {
        const val MAXIMUM_ATTEMPT = 3
        const val GENERIC_ERROR = "Unknown issue."
        const val CONNECTION_DELAY = 1000L
        const val JETPACK_SLUG = "jetpack"
        const val JETPACK_NAME = "jetpack/jetpack"
    }

    private var installAttemptCount: Int = 0
    private var activationAttemptCount: Int = 0

    val viewStateLiveData = LiveDataDelegate(savedState, JetpackInstallProgressViewState())
    private var viewState by viewStateLiveData

    init {
        dispatcher.register(this)
        viewState = viewState.copy(
            installStatus = Installing
        )

        // Start by checking Jetpack plugin's existence in the site.
        // Depending on the result, this view model will then try to either install or activate Jetpack.
        repository.fetchJetpackSitePlugin(JETPACK_NAME)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(this)
    }

    private fun installJetpackPlugin() {
        installAttemptCount++

        appCoroutineScope.launch {
            repository.installPlugin(JETPACK_SLUG).collect {
                when (it) {
                    is PluginInstalled -> {
                        viewState = viewState.copy(installStatus = Activating)
                    }

                    is PluginInstallFailed -> {
                        // Sometimes plugin installation can go through but the API returns failure. In this case,
                        // we fetch the plugin again, which depending on the result will then trigger another install or
                        // activation attempt.
                        if (installAttemptCount < MAXIMUM_ATTEMPT) {
                            repository.fetchJetpackSitePlugin(JETPACK_NAME)
                        } else {
                            viewState = viewState.copy(installStatus = Failed(it.error))
                        }
                    }

                    is PluginActivated -> {
                        simulateConnectingAndDoneSteps()
                    }

                    is PluginActivationFailed -> {
                        if (activationAttemptCount < MAXIMUM_ATTEMPT) {
                            activationAttemptCount++
                            repository.activatePlugin(JETPACK_NAME, JETPACK_SLUG)
                        } else {
                            viewState = viewState.copy(installStatus = Failed(it.error))
                        }
                    }
                }
            }
        }
    }

    private fun simulateConnectingAndDoneSteps() {
        viewState = viewState.copy(installStatus = Connecting)
        Handler(Looper.getMainLooper()).postDelayed(
            { viewState = viewState.copy(installStatus = Finished) },
            CONNECTION_DELAY
        )
    }

    @Parcelize
    data class JetpackInstallProgressViewState(
        val installStatus: InstallStatus? = null
    ) : Parcelable

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSitePluginFetched(event: OnJetpackSitePluginFetched) {
        if (!event.isError) {
            if (activationAttemptCount < MAXIMUM_ATTEMPT) {
                activationAttemptCount++
                viewState = viewState.copy(installStatus = Activating)
                repository.activatePlugin(JETPACK_NAME, JETPACK_SLUG)
            } else {
                viewState = viewState.copy(installStatus = Failed(GENERIC_ERROR))
            }
        } else {
            installJetpackPlugin()
        }
    }

    sealed class InstallStatus : Parcelable {
        @Parcelize
        object Installing : InstallStatus()

        @Parcelize
        object Activating : InstallStatus()

        @Parcelize
        object Connecting : InstallStatus()

        @Parcelize
        object Finished : InstallStatus()

        @Parcelize
        data class Failed(val error: String?) : InstallStatus()
    }
}
