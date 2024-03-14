package com.woocommerce.android.ui.prefs.plugins

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.ui.compose.component.ProgressIndicator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Loaded.Plugin.PluginStatus.INACTIVE
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Loaded.Plugin.PluginStatus.UPDATE_AVAILABLE
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Loaded.Plugin.PluginStatus.UP_TO_DATE

@Composable
fun PluginsScreen(viewModel: PluginsViewModel) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.settings_plugins),
                onNavigationButtonClick = viewModel::onBackPressed,
                navigationIcon = Filled.ArrowBack
            )
        },
        modifier = Modifier.background(MaterialTheme.colors.surface)
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            viewModel.viewState.observeAsState().value?.let { state ->
                PluginsScreen(state)
            }
        }
    }
}

@Composable
private fun PluginsScreen(state: ViewState) {
    when (state) {
        is ViewState.Loading -> {
            ProgressIndicator()
        }
        is ViewState.Error -> {
            Error()
        }
        is ViewState.Loaded -> {
            Plugins(state.plugins)
        }
    }
}

@Composable
private fun Plugins(plugins: List<ViewState.Loaded.Plugin>) {
    LazyColumn {
        items(plugins) { plugin ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.major_100))
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )

                    if (plugin.authorName.isNotNullOrEmpty()) {
                        Text(text = plugin.authorName!!, color = colorResource(id = R.color.color_on_surface_medium))
                    }
                }

                Column {
                    Row(modifier = Modifier.align(Alignment.End)) {
                        Text(
                            style = MaterialTheme.typography.subtitle1,
                            text = plugin.version,
                        )
                    }

                    Text(
                        text = stringResource(id = plugin.status.title),
                        color = colorResource(id = plugin.status.color),
                        style = MaterialTheme.typography.subtitle2
                    )
                }
            }

            if (plugins.last() != plugin) {
                Divider()
            }
        }
    }
}

@Composable
private fun Error() {
    // Error state
}

@LightDarkThemePreviews
@Composable
private fun PreviewPlugins() {
    PluginsScreen(
        ViewState.Loaded(
            plugins = listOf(
                ViewState.Loaded.Plugin("Plugin 1", "Automattic", "1.0", UP_TO_DATE),
                ViewState.Loaded.Plugin("Plugin 2", null, "2.0", UPDATE_AVAILABLE),
                ViewState.Loaded.Plugin("Plugin 3", "Gutenberg", "3.0", INACTIVE),
            )
        )
    )
}
