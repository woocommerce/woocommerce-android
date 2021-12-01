package com.woocommerce.android.ui.jetpack

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.jetpack.JetpackInstallViewModel.InstallStatus.*
import com.woocommerce.android.ui.jetpack.PluginRepository.PluginStatus
import com.woocommerce.android.ui.jetpack.PluginRepository.PluginStatus.*
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class JetpackInstallViewModelTest : BaseUnitTest() {
    private val savedState: SavedStateHandle = SavedStateHandle()
    private val installationStateFlow = MutableSharedFlow<PluginStatus>(extraBufferCapacity = Int.MAX_VALUE)
    private val pluginRepository: PluginRepository = mock {
        on { installPlugin(any(), any()) } doReturn installationStateFlow
    }
    private val siteModel: SiteModel = mock()
    private lateinit var viewModel: JetpackInstallViewModel

    companion object {
        const val EXAMPLE_SLUG = "plugin-slug"
        const val EXAMPLE_NAME = "plugin-name"
        const val EXAMPLE_ERROR = "error-message"
    }

    @Before
    fun setup() {
        viewModel = JetpackInstallViewModel(
            savedState,
            pluginRepository
        )
    }

    @Test
    fun `when installation is successful, then set proper install states`() = testBlocking {
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        installationStateFlow.tryEmit(PluginInstalled(EXAMPLE_SLUG, siteModel))
        installationStateFlow.tryEmit(PluginActivated(EXAMPLE_NAME, siteModel))
        advanceUntilIdle()

        Assertions.assertThat(installStates).containsExactly(
            Installing,
            Activating,
            Connecting,
            Finished
        )
    }

    @Test
    fun `when installation is failed, then set failed state`() = testBlocking {
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        installationStateFlow.tryEmit(PluginInstallFailed(EXAMPLE_ERROR))
        advanceUntilIdle()

        Assertions.assertThat(installStates).contains(
            Failed(EXAMPLE_ERROR)
        )
    }

    @Test
    fun `when activation is failed, then set failed state`() = testBlocking {
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        installationStateFlow.tryEmit(PluginActivationFailed(EXAMPLE_ERROR))
        advanceUntilIdle()

        Assertions.assertThat(installStates).contains(
            Failed(EXAMPLE_ERROR)
        )
    }

    @Test
    fun `when a plugin to install already exists, then set activating state`() = testBlocking {
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        installationStateFlow.tryEmit(PluginInstalled(EXAMPLE_NAME, siteModel))
        advanceUntilIdle()

        Assertions.assertThat(installStates).containsExactly(
            Installing,
            Activating
        )
    }

    @Test
    fun `given failed plugin install, when install succeeds after retry, then set success state`() = testBlocking {
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        // Assuming successful retry, the install flow does not return any failed state, so the emitted values are
        // similar to successful install without retry.
        installationStateFlow.tryEmit(PluginInstalled(EXAMPLE_SLUG, siteModel))
        installationStateFlow.tryEmit(PluginActivated(EXAMPLE_NAME, siteModel))
        advanceUntilIdle()

        Assertions.assertThat(installStates).containsExactly(
            Installing,
            Activating,
            Connecting,
            Finished
        )
    }

    @Test
    fun `given failed plugin install, when install fails after retries, then set failed state`() = testBlocking {
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        // If installation fails after multiple retry, the install flow only outputs PluginInstallFailed
        installationStateFlow.tryEmit(PluginInstallFailed(EXAMPLE_ERROR))
        advanceUntilIdle()

        Assertions.assertThat(installStates).contains(
            Failed(EXAMPLE_ERROR)
        )
    }

    @Test
    fun `given failed plugin activation, when activation fails after retries, then set failed state`() = testBlocking {
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }
        installationStateFlow.tryEmit(PluginInstalled(EXAMPLE_NAME, siteModel))
        installationStateFlow.tryEmit(PluginActivationFailed(EXAMPLE_ERROR))
        advanceUntilIdle()

        Assertions.assertThat(installStates).containsExactly(
            Installing,
            Activating,
            Failed(EXAMPLE_ERROR)
        )
    }
}
