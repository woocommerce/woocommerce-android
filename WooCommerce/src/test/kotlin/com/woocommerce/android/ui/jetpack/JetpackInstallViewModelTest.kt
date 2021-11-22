package com.woocommerce.android.ui.jetpack

import androidx.lifecycle.SavedStateHandle
import org.mockito.kotlin.any
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.jetpack.JetpackInstallViewModel.InstallStatus.*
import com.woocommerce.android.ui.jetpack.PluginRepository.PluginStatus.*
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class JetpackInstallViewModelTest : BaseUnitTest() {
    private val savedState: SavedStateHandle = SavedStateHandle()
    private val pluginRepository: PluginRepository = mock()
    private val siteModel: SiteModel = mock()
    private lateinit var viewModel: JetpackInstallViewModel

    companion object {
        const val EXAMPLE_SLUG = "plugin-slug"
        const val EXAMPLE_NAME = "plugin-name"
        const val EXAMPLE_ERROR = "error-message"
        const val CONNECTION_DELAY = 1000L
    }

    @Before
    fun setup() {
        viewModel = spy(
            JetpackInstallViewModel(
                savedState,
                pluginRepository
            )
        )
    }

    @Test
    fun `when installation is successful, then set proper install states`() = testBlocking {
        whenever(pluginRepository.installPlugin(any()))
            .thenReturn(
                flow {
                    emit(PluginInstalled(EXAMPLE_SLUG, siteModel))
                    emit(PluginActivated(EXAMPLE_NAME, siteModel))
                }
            )
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        viewModel.installJetpackPlugin()

        // delay needed because between Connecting -> Finished steps there's an introduced delay.
        delay(CONNECTION_DELAY)

        Assertions.assertThat(installStates).containsExactly(
            Installing,
            Activating,
            Connecting,
            Finished
        )
    }

    @Test
    fun `when installation is failed, then set failed state`() = testBlocking {
        whenever(pluginRepository.installPlugin(any()))
            .thenReturn(
                flow {
                    emit(PluginInstallFailed(EXAMPLE_ERROR))
                }
            )
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        viewModel.installJetpackPlugin()

        Assertions.assertThat(installStates).contains(
            Failed(EXAMPLE_ERROR)
        )
    }

    @Test
    fun `when activation is failed, then set failed state`() = testBlocking {
        whenever(pluginRepository.installPlugin(any()))
            .thenReturn(
                flow {
                    emit(PluginInstalled(EXAMPLE_SLUG, siteModel))
                    emit(PluginActivationFailed(EXAMPLE_ERROR))
                }
            )
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        viewModel.installJetpackPlugin()

        Assertions.assertThat(installStates).contains(
            Failed(EXAMPLE_ERROR)
        )
    }
}
