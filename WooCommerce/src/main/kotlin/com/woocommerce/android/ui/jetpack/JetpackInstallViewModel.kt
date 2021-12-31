package com.woocommerce.android.ui.jetpack

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.ui.jetpack.PluginRepository.PluginStatus.*
import com.woocommerce.android.ui.jetpack.JetpackInstallViewModel.InstallStatus.*
import com.woocommerce.android.ui.jetpack.JetpackInstallViewModel.FailureType.*
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
        const val JETPACK_SLUG = "jetpack"
        const val JETPACK_NAME = "jetpack/jetpack"
        const val CONNECTION_ERROR = "Connection error."
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
                        viewState = viewState.copy(installStatus = Failed(INSTALLATION, it.errorDescription))
                    }

                    is PluginActivated -> {
                        AnalyticsTracker.track(Stat.JETPACK_INSTALL_SUCCEEDED)
                        checkJetpackConnection()
                    }

                    is PluginActivationFailed -> {
                        AnalyticsTracker.track(
                            Stat.JETPACK_INSTALL_FAILED,
                            errorContext = this@JetpackInstallViewModel.javaClass.simpleName,
                            errorType = it.errorType,
                            errorDescription = it.errorDescription
                        )
                        viewState = viewState.copy(installStatus = Failed(ACTIVATION, it.errorDescription))
                    }
                }
            }
        }
    }

    fun checkJetpackConnection() {
        launch {
            viewState = viewState.copy(installStatus = Connecting)
            val isJetpackConnected = repository.isJetpackConnectedAfterInstallation()
            viewState = if (isJetpackConnected) {
                viewState.copy(installStatus = Finished)
            } else {
                viewState.copy(installStatus = Failed(CONNECTION, CONNECTION_ERROR))
            }
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
        data class Failed(val errorType: FailureType, val errorDescription: String) : InstallStatus()
    }

    enum class FailureType {
        INSTALLATION,
        ACTIVATION,
        CONNECTION
    }
}
