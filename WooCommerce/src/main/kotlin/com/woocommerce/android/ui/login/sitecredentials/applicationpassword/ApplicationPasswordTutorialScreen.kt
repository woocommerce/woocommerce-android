package com.woocommerce.android.ui.login.sitecredentials.applicationpassword

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun ApplicationPasswordTutorialScreen(viewModel: ApplicationPasswordTutorialViewModel) {
    val viewState = viewModel.viewState.observeAsState()
    ApplicationPasswordTutorialScreen(
        authorizationStarted = viewState.value?.authorizationStarted ?: false,
        errorMessageRes = viewState.value?.errorMessage,
        webViewUrl = viewState.value?.authorizationUrl.orEmpty(),
        webViewUserAgent = viewModel.userAgent,
        onContinueClicked = viewModel::onContinueClicked,
        onContactSupportClicked = viewModel::onContactSupportClicked,
        onPageLoaded = viewModel::onWebPageLoaded,
        onNavigationButtonClicked = viewModel::onNavigationButtonClicked
    )
}

@Composable
fun ApplicationPasswordTutorialScreen(
    modifier: Modifier = Modifier,
    authorizationStarted: Boolean,
    webViewUrl: String,
    webViewUserAgent: UserAgent?,
    @StringRes errorMessageRes: Int?,
    onPageLoaded: (String) -> Unit,
    onContinueClicked: () -> Unit,
    onContactSupportClicked: () -> Unit,
    onNavigationButtonClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                onNavigationButtonClick = onNavigationButtonClicked,
                navigationIcon = when {
                    authorizationStarted -> Icons.Filled.Close
                    else -> Icons.Filled.ArrowBack
                }
            )
        }
    ) { paddingValues ->
        if (authorizationStarted && webViewUserAgent != null) {
            WCWebView(
                url = webViewUrl,
                userAgent = webViewUserAgent,
                onPageFinished = onPageLoaded,
                modifier = modifier
            )
        } else {
            TutorialContentScreen(
                modifier = modifier,
                paddingValues = paddingValues,
                errorMessageRes = errorMessageRes,
                onContinueClicked = onContinueClicked,
                onContactSupportClicked = onContactSupportClicked
            )
        }
    }
}

@Composable
private fun TutorialContentScreen(
    modifier: Modifier,
    paddingValues: PaddingValues,
    @StringRes errorMessageRes: Int?,
    onContinueClicked: () -> Unit,
    onContactSupportClicked: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colors.surface)
    ) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = modifier.padding(dimensionResource(id = R.dimen.major_100)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))
            ) {
                Text(
                    text = stringResource(id = R.string.login_app_password_title),
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold
                )
                Text(stringResource(id = errorMessageRes ?: R.string.login_app_password_subtitle))
            }

            Divider(modifier = modifier.padding(start = dimensionResource(id = R.dimen.major_100)))

            Column(
                modifier = modifier.padding(dimensionResource(id = R.dimen.major_100)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))
            ) {
                Text(stringResource(id = R.string.login_app_password_instructions_title))
                Text(stringResource(id = R.string.login_app_password_instructions_step_1))
                Text(stringResource(id = R.string.login_app_password_instructions_step_2))

                Image(
                    painter = painterResource(id = R.drawable.app_password_tutorial_hint),
                    contentDescription = null,
                    modifier = modifier.align(alignment = Alignment.CenterHorizontally)
                )

                Text(stringResource(id = R.string.login_app_password_instructions_step_3))
            }

            Divider(modifier = modifier.padding(start = dimensionResource(id = R.dimen.major_100)))

            Text(
                text = stringResource(id = R.string.login_app_password_instructions_footer),
                modifier = modifier.padding(dimensionResource(id = R.dimen.major_100))
            )
        }

        Spacer(modifier.weight(1f))
        Divider()

        Column(
            modifier = modifier
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                .padding(vertical = dimensionResource(id = R.dimen.minor_100))
        ) {
            Button(
                onClick = onContinueClicked,
                modifier = modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.login_app_password_continue_button))
            }

            OutlinedButton(
                onClick = onContactSupportClicked,
                modifier = modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.login_app_password_support_button))
            }
        }
    }
}

@Preview
@Composable
fun ApplicationPasswordTutorialScreenPreview() {
    WooThemeWithBackground {
        ApplicationPasswordTutorialScreen(
            authorizationStarted = false,
            errorMessageRes = R.string.login_app_password_subtitle,
            webViewUrl = "",
            webViewUserAgent = null,
            onContinueClicked = { },
            onContactSupportClicked = { },
            onPageLoaded = { },
            onNavigationButtonClicked = { }
        )
    }
}
