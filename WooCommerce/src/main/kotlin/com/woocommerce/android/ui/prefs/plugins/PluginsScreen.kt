package com.woocommerce.android.ui.prefs.plugins

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.prefs.plugins.PluginsViewState.Loaded.Plugin
import com.woocommerce.android.ui.prefs.plugins.PluginsViewState.Loaded.Plugin.PluginStatus.Inactive
import com.woocommerce.android.ui.prefs.plugins.PluginsViewState.Loaded.Plugin.PluginStatus.Unknown
import com.woocommerce.android.ui.prefs.plugins.PluginsViewState.Loaded.Plugin.PluginStatus.UpToDate
import com.woocommerce.android.ui.prefs.plugins.PluginsViewState.Loaded.Plugin.PluginStatus.UpdateAvailable

@Composable
fun PluginsScreen(viewModel: PluginsViewModel) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.settings_plugins),
                onNavigationButtonClick = viewModel::onBackPressed,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack
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
                PluginsScreen(
                    state = state,
                    onRetryTapped = viewModel::onRetryClicked,
                    onPluginClicked = viewModel::onPluginClicked,
                )
            }
        }
    }
}

@Composable
private fun PluginsScreen(
    state: PluginsViewState,
    onRetryTapped: () -> Unit,
    onPluginClicked: (Plugin) -> Unit,
) {
    Crossfade(targetState = state, label = "") {
        when (it) {
            is PluginsViewState.Loading -> {
                ShimmerPluginsList()
            }

            is PluginsViewState.Error -> {
                Error(onRetryTapped)
            }

            is PluginsViewState.Loaded -> {
                Plugins(
                    it.plugins,
                    onPluginClicked,
                )
            }
        }
    }
}

@Composable
private fun Plugins(
    plugins: List<Plugin>,
    onPluginClicked: (Plugin) -> Unit
) {
    LazyColumn {
        items(plugins) { plugin ->
            PluginItem(plugin, onPluginClicked)

            if (plugins.last() != plugin) {
                Divider()
            }
        }
    }
}

@Composable
private fun PluginItem(
    plugin: Plugin,
    onPluginClicked: (Plugin) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onPluginClicked(plugin) })
            .padding(dimensionResource(R.dimen.major_100))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = plugin.name,
                fontWeight = FontWeight.Bold
            )

            if (plugin.authorName.isNotNullOrEmpty()) {
                Text(text = plugin.authorName!!, color = colorResource(id = R.color.color_on_surface_medium))
            }
        }

        Column {
            Row(modifier = Modifier.align(Alignment.End)) {
                Text(
                    text = plugin.version,
                )
            }

            when (plugin.status) {
                is Inactive -> Text(
                    text = plugin.status.title,
                    color = colorResource(id = plugin.status.color),
                    fontWeight = FontWeight.Bold
                )

                is UpToDate -> Text(
                    text = plugin.status.title,
                    color = colorResource(id = plugin.status.color),
                    fontWeight = FontWeight.Bold
                )

                is UpdateAvailable -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = plugin.status.title,
                            tint = colorResource(id = plugin.status.color),
                            modifier = Modifier
                                .size(24.dp)
                        )

                        Text(
                            text = plugin.status.title,
                            color = colorResource(id = plugin.status.color),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Unknown -> {}
            }
        }
    }
}

@Composable
fun ShimmerPluginsList() {
    val colors = listOf(
        Color.LightGray.copy(alpha = 0.9f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.9f)
    )

    val transition = rememberInfiniteTransition(label = "")
    val animation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing, delayMillis = 500),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(animation.value - 500f, animation.value - 500f),
        end = Offset(animation.value, animation.value)
    )

    LazyColumn {
        items(10) {
            ShimmerPluginItem(brush = brush)

            if (it < 10) {
                Divider()
            }
        }
    }
}

@Composable
fun ShimmerPluginItem(brush: Brush) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.major_100))
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush)
                    .height(18.dp)
                    .width(200.dp)
            )
            Box(
                modifier = Modifier
                    .background(brush)
                    .height(18.dp)
                    .width(150.dp)
            )
        }

        Column(
            modifier = Modifier
                .width(100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .background(brush)
                    .height(18.dp)
                    .width(40.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .background(brush)
                    .height(16.dp)
                    .width(70.dp)
            )
        }
    }
}

@Composable
private fun Error(onRetryTapped: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.major_100)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.plugins_error_message),
            modifier = Modifier.padding(dimensionResource(R.dimen.major_150)),
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center
        )
        WCColoredButton(onClick = onRetryTapped, text = stringResource(id = R.string.retry))
    }
}

@LightDarkThemePreviews
@Composable
private fun PreviewPlugins() {
    WooThemeWithBackground {
        PluginsScreen(
            PluginsViewState.Loaded(
                plugins = listOf(
                    Plugin("Plugin 1", "Automattic", "1.0", UpToDate("Up-to-date", R.color.color_info)),
                    Plugin(
                        "Plugin 2",
                        "Something",
                        "2.0",
                        UpdateAvailable("Update available (4.9)", R.color.color_primary)
                    ),
                    Plugin("Plugin 3", "Gutenberg", "3.0", Inactive("Inactive", R.color.color_on_surface_disabled)),
                    Plugin("Plugin 5", "Blabla", "5.0", Unknown)
                )
            ),
            onRetryTapped = {},
            onPluginClicked = {},
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun PreviewError() {
    WooThemeWithBackground {
        PluginsScreen(
            PluginsViewState.Error,
            onRetryTapped = {},
            onPluginClicked = {},
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun PreviewLoading() {
    WooThemeWithBackground {
        PluginsScreen(
            PluginsViewState.Loading,
            onRetryTapped = {},
            onPluginClicked = {},
        )
    }
}
