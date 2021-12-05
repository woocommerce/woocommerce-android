package com.woocommerce.android.ui.jetpack

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.ui.jetpack.PluginRepository.PluginStatus.*
import com.woocommerce.android.ui.jetpack.JetpackInstallViewModel.InstallStatus.*
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class JetpackInstallViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: PluginRepository
) : ScopedViewModel(savedState) {
    companion object {
        const val CONNECTION_DELAY = 1000L
        const val JETPACK_SLUG = "jetpack"
        const val JETPACK_NAME = "jetpack/jetpack"
    }

    val viewStateLiveData = LiveDataDelegate(savedState, JetpackInstallProgressViewState())
    private var viewState by viewStateLiveData

    init {
        viewState = viewState.copy(
            installStatus = Installing
        )

        installJetpackPlugin()
    }

    override fun onCleared() {
        super.onCleared()
        repository.onCleanup()
    }

    private fun installJetpackPlugin() {
        launch {
            repository.installPlugin(JETPACK_SLUG, JETPACK_NAME).collect {
                when (it) {
                    is PluginInstalled -> {
                        viewState = viewState.copy(installStatus = Activating)
                    }

                    is PluginInstallFailed -> {
                        AnalyticsTracker.track(
                            Stat.JETPACK_INSTALL_FAILED,
                            errorContext = this@JetpackInstallViewModel.javaClass.simpleName,
                            errorType = it.errorType,
                            errorDescription = it.errorDescription
                        )
                        viewState = viewState.copy(installStatus = Failed(it.errorType, it.errorDescription))
                    }

                    is PluginActivated -> {
                        AnalyticsTracker.track(Stat.JETPACK_INSTALL_SUCCEEDED)
                        simulateConnectingAndFinishedSteps()
                    }

                    is PluginActivationFailed -> {
                        AnalyticsTracker.track(
                            Stat.JETPACK_INSTALL_FAILED,
                            errorContext = this@JetpackInstallViewModel.javaClass.simpleName,
                            errorType = it.errorType,
                            errorDescription = it.errorDescription
                        )
                        viewState = viewState.copy(installStatus = Failed(it.errorType, it.errorDescription))
                    }
                }
            }
        }
    }

    private fun simulateConnectingAndFinishedSteps() {
        launch {
            viewState = viewState.copy(installStatus = Connecting)
            delay(CONNECTION_DELAY)
            viewState = viewState.copy(installStatus = Finished)
        }
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
        object Connecting : InstallStatus()

        @Parcelize
        object Finished : InstallStatus()

        @Parcelize
        data class Failed(val errorType: String, val errorDescription: String) : InstallStatus()
    }
}
