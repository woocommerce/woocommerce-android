package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.components.JetpackToWooHeader

@Composable
fun JetpackActivationWPCom2FAScreen(viewModel: JetpackActivationWPCom2FAViewModel) {
    JetpackActivationWPCom2FAScreen(
        onCloseClick = viewModel::onCloseClick,
        onSMSLinkClick = viewModel::onSMSLinkClick,
        onContinueClick = viewModel::onContinueClick
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun JetpackActivationWPCom2FAScreen(
    onCloseClick: () -> Unit = {},
    onSMSLinkClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            Toolbar(
                onNavigationButtonClick = onCloseClick,
                navigationIcon = Icons.Filled.Clear
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(id = R.dimen.major_100)),
            ) {
                JetpackToWooHeader()
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
                val title = R.string.login_jetpack_connect
                Text(
                    text = stringResource(id = title),
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                Text(
                    text = stringResource(
                        id = R.string.enter_verification_code
                    )
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                WCPasswordField(
                    value = "",
                    onValueChange = { },
                    label = stringResource(id = R.string.verification_code)
                )
                WCTextButton(onClick = onSMSLinkClick) {
                    Text(text = stringResource(id = R.string.login_text_otp))
                }
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            }

            Spacer(modifier = Modifier.weight(1f))

            WCColoredButton(
                onClick = {
                    keyboardController?.hide()
                    onContinueClick()
                },
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            ) {
                Text(
                    text = stringResource(id = R.string.login_jetpack_connect)
                )
            }
        }
    }
}

@Preview
@Composable
private fun JetpackActivationWPCom2FAScreenPreview() {
    WooThemeWithBackground {
        JetpackActivationWPCom2FAScreen()
    }
}
