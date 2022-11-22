package com.woocommerce.android.ui.common.wpcomwebview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.WCWebView
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun WPComWebViewScreen(
    navArgs: WPComWebViewFragmentArgs,
    wpcomWebViewAuthenticator: WPComWebViewAuthenticator,
    userAgent: UserAgent,
    onUrlLoaded: (String) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = { Toolbar(navArgs, onClose) }
    ) { paddingValues ->
        WCWebView(
            url = navArgs.urlToLoad,
            userAgent = userAgent,
            wpComAuthenticator = wpcomWebViewAuthenticator,
            onUrlLoaded = {
                TODO()
            },
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        )
    }
}

@Composable
private fun Toolbar(
    navArgs: WPComWebViewFragmentArgs,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = { Text(navArgs.title ?: "") },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    Filled.Clear,
                    contentDescription = stringResource(id = string.back)
                )
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}
