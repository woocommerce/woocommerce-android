package com.woocommerce.android.ui.jetpack

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jetpack.JetpackInstallViewModel.FailureType.*
import com.woocommerce.android.ui.jetpack.JetpackInstallViewModel.InstallStatus.*
import com.woocommerce.android.ui.jetpack.PluginRepository.PluginStatus
import com.woocommerce.android.ui.jetpack.PluginRepository.PluginStatus.*
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class JetpackInstallViewModelTest : BaseUnitTest() {
    private val savedState: SavedStateHandle = SavedStateHandle()
    private val installationStateFlow = MutableSharedFlow<PluginStatus>(extraBufferCapacity = Int.MAX_VALUE)
    private val pluginRepository: PluginRepository = mock {
        on { installPlugin(any(), any()) } doReturn installationStateFlow
    }
    private lateinit var siteModelMock: SiteModel
    private lateinit var selectedSiteMock: SelectedSite
    private val wooCommerceStore: WooCommerceStore = mock()
    private lateinit var exampleResult: WooResult<List<SiteModel>>
    private lateinit var viewModel: JetpackInstallViewModel

    companion object {
        const val EXAMPLE_SLUG = "plugin-slug"
        const val EXAMPLE_NAME = "plugin-name"
        const val EXAMPLE_ERROR = "error-message"
        const val CONNECTION_ERROR = "Connection error."
        const val SITE_ID = 1337L
    }

    @Before
    fun setup() {
        siteModelMock = mock {
            on { siteId }.doReturn(SITE_ID)
            on { isJetpackConnected }.doReturn(true)
        }
        selectedSiteMock = mock {
            on { get() }.doReturn(siteModelMock)
        }
        exampleResult = WooResult(model = listOf(siteModelMock))

        viewModel = JetpackInstallViewModel(
            savedState,
            pluginRepository,
            selectedSiteMock,
            wooCommerceStore
        )
    }

    @Test
    fun `when installation is successful, then set proper install states`() = testBlocking {
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        doReturn(true).whenever(siteModelMock).hasWooCommerce
        doReturn(exampleResult).whenever(wooCommerceStore).fetchWooCommerceSites()

        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        installationStateFlow.tryEmit(PluginInstalled(EXAMPLE_SLUG, siteModelMock))
        installationStateFlow.tryEmit(PluginActivated(EXAMPLE_NAME, siteModelMock))
        advanceUntilIdle()

        Assertions.assertThat(installStates).containsExactly(
            Installing,
            Activating,
            Connecting(false),
            Finished
        )
    }

    @Test
    fun `when installation is failed, then set failed state`() = testBlocking {
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        installationStateFlow.tryEmit(PluginInstallFailed(EXAMPLE_ERROR, EXAMPLE_ERROR))
        advanceUntilIdle()

        Assertions.assertThat(installStates).contains(
            Failed(INSTALLATION, EXAMPLE_ERROR)
        )
    }

    @Test
    fun `when activation is failed, then set failed state`() = testBlocking {
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        installationStateFlow.tryEmit(PluginInstalled(EXAMPLE_NAME, siteModelMock))
        installationStateFlow.tryEmit(PluginActivationFailed(EXAMPLE_ERROR, EXAMPLE_ERROR))
        advanceUntilIdle()

        Assertions.assertThat(installStates).contains(
            Installing,
            Activating,
            Failed(ACTIVATION, EXAMPLE_ERROR)
        )
    }

    @Test
    fun `when a plugin to install already exists, then set activating state`() = testBlocking {
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        installationStateFlow.tryEmit(PluginInstalled(EXAMPLE_NAME, siteModelMock))
        advanceUntilIdle()

        Assertions.assertThat(installStates).containsExactly(
            Installing,
            Activating
        )
    }

    @Test
    fun `when connecting is failed, then set failed state`() = testBlocking {
        val installStates = mutableListOf<JetpackInstallViewModel.InstallStatus>()
        doReturn(false).whenever(siteModelMock).hasWooCommerce
        doReturn(exampleResult).whenever(wooCommerceStore).fetchWooCommerceSites()

        viewModel.viewStateLiveData.observeForever { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) { installStates.add(it) }
        }

        installationStateFlow.tryEmit(PluginInstalled(EXAMPLE_SLUG, siteModelMock))
        installationStateFlow.tryEmit(PluginActivated(EXAMPLE_NAME, siteModelMock))
        advanceUntilIdle()

        Assertions.assertThat(installStates).containsExactly(
            Installing,
            Activating,
            Connecting(false),
            Failed(CONNECTION, CONNECTION_ERROR)
        )
    }
}
