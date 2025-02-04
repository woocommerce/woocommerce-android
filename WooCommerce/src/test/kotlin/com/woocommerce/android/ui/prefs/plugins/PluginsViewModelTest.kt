package com.woocommerce.android.ui.prefs.plugins

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.plugins.PluginsViewState.Loaded
import com.woocommerce.android.ui.prefs.plugins.PluginsViewState.Loaded.Plugin
import com.woocommerce.android.ui.prefs.plugins.PluginsViewState.Loaded.Plugin.PluginStatus.Inactive
import com.woocommerce.android.ui.prefs.plugins.PluginsViewState.Loaded.Plugin.PluginStatus.Unknown
import com.woocommerce.android.ui.prefs.plugins.PluginsViewState.Loaded.Plugin.PluginStatus.UpToDate
import com.woocommerce.android.ui.prefs.plugins.PluginsViewState.Loaded.Plugin.PluginStatus.UpdateAvailable
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WCSystemPluginResponse.SystemPluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class PluginsViewModelTest : BaseUnitTest() {
    private val savedStateHandle: SavedStateHandle = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(mock())
    }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(R.string.plugin_state_update_available, "1.1.0") }
            .thenReturn("Update available to 1.1.0")
        on { getString(R.string.plugin_state_update_available, "2.1.0") }
            .thenReturn("Update available to 2.1.0")
        on { getString(R.string.plugin_state_inactive) }
            .thenReturn("Inactive")
    }

    private fun createViewModel() = PluginsViewModel(
        savedStateHandle,
        selectedSite,
        wooCommerceStore,
        resourceProvider,
        UnconfinedTestDispatcher()
    )

    @Test
    fun `given plugins are fetched successfully, when vm init, then viewState should be Loaded`() = testBlocking {
        // GIVEN
        val pluginsResponse = listOf(
            SystemPluginModel(
                name = "Plugin A",
                authorName = "Author A",
                plugin = "plugin.php",
                version = "1.0.0",
                versionLatest = "1.1.0",
                url = "https://example.com/plugins.php",
                isActive = true
            ),
            SystemPluginModel(
                name = "Plugin B",
                authorName = "Author B",
                plugin = "plugin.php",
                version = "2.0.0",
                versionLatest = "2.0.0",
                url = "https://example.com/plugins.php",
                isActive = false
            )
        )
        whenever(wooCommerceStore.fetchSystemPlugins(selectedSite.get()))
            .thenReturn(WooResult(pluginsResponse))

        // WHEN
        val viewModel = createViewModel()

        // THEN
        val values = viewModel.viewState.captureValues()
        assertThat(values.last()).isEqualTo(
            Loaded(
                plugins = listOf(
                    Plugin(
                        "Plugin A",
                        "Author A",
                        "1.0.0",
                        UpdateAvailable("Update available to 1.1.0", R.color.color_primary)
                    ),
                    Plugin(
                        "Plugin B",
                        "Author B",
                        "2.0.0",
                        Inactive("Inactive", R.color.color_on_surface_disabled)
                    )
                )
            )
        )
    }

    @Test
    fun `given plugins fetch fails, when vm init, then viewState should be Error`() = testBlocking {
        // GIVEN
        whenever(wooCommerceStore.fetchSystemPlugins(selectedSite.get())).thenReturn(
            WooResult(
                WooError(
                    type = WooErrorType.GENERIC_ERROR,
                    original = GenericErrorType.SERVER_ERROR
                )
            )
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        val values = viewModel.viewState.captureValues()
        assertThat(values.last()).isEqualTo(PluginsViewState.Error)
    }

    @Test
    fun `given plugin status is UpdateAvailable, when onPluginClicked is called, then navigate event is triggered`() =
        testBlocking {
            // GIVEN
            val plugin = Plugin(
                name = "Plugin A",
                authorName = "Author A",
                version = "1.0.0",
                status = UpdateAvailable("Update available to 1.1.0", R.color.color_primary)
            )
            val siteModel = mock<SiteModel> {
                on { adminUrl }.thenReturn("https://example.com/wp-admin")
            }
            whenever(selectedSite.get()).thenReturn(siteModel)
            val viewModel = createViewModel()

            // WHEN
            viewModel.onPluginClicked(plugin)

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                PluginsEvent.NavigateToPluginsWeb("https://example.com/wp-admin/plugins.php")
            )
        }

    @Test
    fun `when onBackPressed is called, then Exit event is triggered`() {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onBackPressed()

        // THEN
        assertThat(viewModel.event.value).isEqualTo(Exit)
    }

    @Test
    fun `given plugin status is Inactive, when onPluginClicked is called, then no event is triggered`() = testBlocking {
        // GIVEN
        val plugin = Plugin(
            name = "Plugin A",
            authorName = "Author A",
            version = "1.0.0",
            status = Inactive("Inactive", R.color.color_on_surface_disabled)
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onPluginClicked(plugin)

        // THEN
        assertThat(viewModel.event.value).isNull()
    }

    @Test
    fun `given plugin status is Unknown, when onPluginClicked is called, then no event is triggered`() = testBlocking {
        // GIVEN
        val plugin = Plugin(
            name = "Plugin A",
            authorName = "Author A",
            version = "1.2.3",
            status = Unknown
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onPluginClicked(plugin)

        // THEN
        assertThat(viewModel.event.value).isNull()
    }

    @Test
    fun `given plugin status is UpToDate, when onPluginClicked is called, then no event is triggered`() = testBlocking {
        // GIVEN
        val plugin = Plugin(
            name = "Plugin A",
            authorName = "Author A",
            version = "1.2.3",
            status = UpToDate("Up-to-date", R.color.color_info)
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onPluginClicked(plugin)

        // THEN
        assertThat(viewModel.event.value).isNull()
    }

    @Test
    fun `given store returns success, when onRetryClicked is called, then viewState should be Loaded`() = testBlocking {
        // GIVEN
        val initialResponse = listOf(
            SystemPluginModel(
                name = "Plugin A",
                authorName = "Author A",
                plugin = "plugin.php",
                version = "1.0.0",
                versionLatest = "1.1.0",
                url = "https://example.com/plugins.php",
                isActive = true
            )
        )
        whenever(wooCommerceStore.fetchSystemPlugins(selectedSite.get()))
            .thenReturn(WooResult(initialResponse))

        val viewModel = createViewModel()

        var values = viewModel.viewState.captureValues()

        val newResponse = listOf(
            SystemPluginModel(
                name = "Plugin B",
                authorName = "Author B",
                plugin = "plugin.php",
                version = "2.0.0",
                versionLatest = "2.1.0",
                url = "https://example.com/plugins.php",
                isActive = true
            )
        )
        whenever(wooCommerceStore.fetchSystemPlugins(selectedSite.get()))
            .thenReturn(WooResult(newResponse))

        // WHEN
        viewModel.onRetryClicked()

        // THEN
        values = viewModel.viewState.captureValues()
        assertThat(values.last()).isEqualTo(
            Loaded(
                plugins = listOf(
                    Plugin(
                        name = "Plugin B",
                        authorName = "Author B",
                        version = "2.0.0",
                        status = UpdateAvailable("Update available to 2.1.0", R.color.color_primary)
                    )
                )
            )
        )
    }

    @Test
    fun `given store returns error, when onRetryClicked is called, then viewState should be Error`() = testBlocking {
        // GIVEN
        // Return success for the initial load
        val initialResponse = listOf(
            SystemPluginModel(
                name = "Plugin A",
                authorName = "Author A",
                plugin = "plugin.php",
                version = "1.0.0",
                versionLatest = "1.1.0",
                url = "https://example.com/plugins.php",
                isActive = true
            )
        )
        whenever(wooCommerceStore.fetchSystemPlugins(selectedSite.get()))
            .thenReturn(WooResult(initialResponse))

        val viewModel = createViewModel()

        var values = viewModel.viewState.captureValues()

        whenever(wooCommerceStore.fetchSystemPlugins(selectedSite.get()))
            .thenReturn(
                WooResult(
                    WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = GenericErrorType.SERVER_ERROR
                    )
                )
            )

        // WHEN
        viewModel.onRetryClicked()

        // THEN
        values = viewModel.viewState.captureValues()
        assertThat(values.last()).isEqualTo(PluginsViewState.Error)
    }
}
