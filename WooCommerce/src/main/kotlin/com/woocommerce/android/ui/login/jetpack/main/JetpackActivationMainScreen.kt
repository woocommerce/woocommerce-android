package com.woocommerce.android.ui.login.jetpack.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader

@Composable
fun JetpackActivationMainScreen(viewModel: JetpackActivationMainViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        JetpackActivationMainScreen(it)
    }
}

@Composable
fun JetpackActivationMainScreen(
    viewState: JetpackActivationMainViewModel.ViewState,
    onCloseClick: () -> Unit = {}
) {
    Scaffold(
        topBar = { Toolbar(onCloseClick) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
                .padding(paddingValues)
                .padding(dimensionResource(id = R.dimen.major_100))
                .verticalScroll(rememberScrollState())
        ) {
            JetpackToWooHeader()
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
            Text(
                text = stringResource(
                    id = if (viewState.isJetpackInstalled) R.string.login_jetpack_connection_steps_screen_title
                    else R.string.login_jetpack_installation_steps_screen_title
                ),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
            Text(
                text = annotatedStringRes(
                    stringResId = R.string.login_jetpack_steps_screen_subtitle,
                    viewState.siteUrl
                ),
                style = MaterialTheme.typography.body1
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            viewState.steps.forEach {
                JetpackActivationStep(it)
            }
        }
    }
}

@Composable
fun JetpackActivationStep(step: JetpackActivationMainViewModel.Step, modifier: Modifier = Modifier) {
    /*TODO*/
}

@Composable
private fun Toolbar(
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = {},
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    Filled.Clear,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}

@Composable
@Preview
private fun JetpackActivationMainScreenPreview() {
    WooThemeWithBackground {
        JetpackActivationMainScreen(
            viewState = JetpackActivationMainViewModel.ViewState(
                siteUrl = "reallyniceshirts.com",
                isJetpackInstalled = false,
                steps = listOf(
                    JetpackActivationMainViewModel.Step(
                        title = R.string.login_jetpack_steps_installing,
                        state = JetpackActivationMainViewModel.StepState.Success
                    ),
                    JetpackActivationMainViewModel.Step(
                        title = R.string.login_jetpack_steps_activating,
                        state = JetpackActivationMainViewModel.StepState.Ongoing
                    ),
                    JetpackActivationMainViewModel.Step(
                        title = R.string.login_jetpack_steps_authorizing,
                        state = JetpackActivationMainViewModel.StepState.Idle,
                        additionalInfo = R.string.login_jetpack_steps_authorizing_hint
                    ),
                    JetpackActivationMainViewModel.Step(
                        title = R.string.login_jetpack_steps_done,
                        state = JetpackActivationMainViewModel.StepState.Idle
                    )
                )
            )
        )
    }
}
