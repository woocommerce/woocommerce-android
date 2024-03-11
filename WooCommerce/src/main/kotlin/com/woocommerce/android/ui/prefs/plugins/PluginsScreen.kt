package com.woocommerce.android.ui.prefs.plugins

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProgressIndicator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.major_100))
            ) {
                Text(
                    text = plugin.name,
                    style = MaterialTheme.typography.subtitle1
                )
                Text(text = plugin.version)
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
                ViewState.Loaded.Plugin("Plugin 1", "1.0"),
                ViewState.Loaded.Plugin("Plugin 2", "2.0"),
                ViewState.Loaded.Plugin("Plugin 3", "3.0")
            )
        )
    )
}
