package com.woocommerce.android.ui.jetpack

import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.mystore.PluginRepository.PluginStatus.*
import com.woocommerce.android.ui.jetpack.JetpackInstallViewModel.InstallStatus.*
import com.woocommerce.android.ui.mystore.PluginRepository
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
        const val CONNECTION_DELAY = 1000L
        const val JETPACK_SLUG = "jetpack"
        const val EXAMPLE_SLUG = "photonic"
    }

    val viewStateLiveData = LiveDataDelegate(savedState, JetpackInstallProgressViewState())
    private var viewState by viewStateLiveData

    init {
        viewState = viewState.copy(
            installStatus = Installing
        )
    }

    override fun onCleared() {
        super.onCleared()
        repository.onCleanup()
    }

    fun installJetpackPlugin() {
        launch {
            repository.installPlugin(EXAMPLE_SLUG).collect {
                when (it) {
                    is PluginInstalled -> {
                        viewState = viewState.copy(installStatus = Activating)
                    }

                    is PluginInstallFailed -> {
                        viewState = viewState.copy(installStatus = Failed(it.error))
                    }

                    is PluginActivated -> {
                        simulateConnectingAndDoneSteps()
                    }

                    is PluginActivationFailed -> {
                        viewState = viewState.copy(installStatus = Failed(it.error))
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
