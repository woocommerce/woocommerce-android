package com.woocommerce.android.ui.login.wpcom

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun WPComLoginMagicLinkHandlerScreen(viewModel: WPComLoginMagicLinkHandlerViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        WPComLoginMagicLinkHandlerScreen(
            viewState = it,
            onRetryClick = viewModel::continueLogin,
            onCloseClick = viewModel::onCloseClick
        )
    }
}

@Composable
fun WPComLoginMagicLinkHandlerScreen(
    viewState: WPComLoginMagicLinkHandlerViewModel.ViewState,
    onRetryClick: () -> Unit = {},
    onCloseClick: () -> Unit = {}
) {
    BackHandler(onBack = onCloseClick)

    when (viewState) {
        WPComLoginMagicLinkHandlerViewModel.ViewState.Loading -> Box(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        WPComLoginMagicLinkHandlerViewModel.ViewState.Error ->
            Scaffold(
                topBar = {
                    Toolbar(
                        onNavigationButtonClick = onCloseClick,
                        navigationIcon = ImageVector.vectorResource(R.drawable.ic_close_24dp)
                    )
                },
                backgroundColor = MaterialTheme.colors.surface
            ) { paddingValues ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(id = R.dimen.major_100),
                        Alignment.CenterVertically
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    Text(
                        text = stringResource(id = R.string.login_jetpack_installation_magic_link_failure),
                        textAlign = TextAlign.Center
                    )
                    WCColoredButton(onClick = onRetryClick) {
                        Text(text = stringResource(id = R.string.retry))
                    }
                }
            }
    }
}

@Preview
@Composable
private fun ErrorPreview() {
    WooThemeWithBackground {
        WPComLoginMagicLinkHandlerScreen(
            viewState = WPComLoginMagicLinkHandlerViewModel.ViewState.Error
        )
    }
}

@Preview
@Composable
private fun LoadingPreview() {
    WooThemeWithBackground {
        WPComLoginMagicLinkHandlerScreen(
            viewState = WPComLoginMagicLinkHandlerViewModel.ViewState.Loading
        )
    }
}
