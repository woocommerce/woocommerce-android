package com.woocommerce.android.ui.login.error.base

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.error.base.LoginBaseErrorDialogFragment.LoginErrorButton

@Composable
fun LoginErrorScreen(
    text: CharSequence,
    @DrawableRes illustration: Int,
    onHelpButtonClick: () -> Unit,
    inlineButtons: List<LoginErrorButton>,
    primaryButton: LoginErrorButton?,
    secondaryButton: LoginErrorButton?
) {
    Scaffold(
        topBar = {
            ToolbarWithHelpButton(
                navigationIcon = null,
                onHelpButtonClick = onHelpButtonClick
            )
        }
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(it)
                .padding(dimensionResource(id = R.dimen.major_100))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Image(painter = painterResource(id = illustration), contentDescription = null)
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                when (text) {
                    is String -> Text(
                        text = text,
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center
                    )
                    is AnnotatedString -> Text(
                        text = text,
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center
                    )
                    else -> error("Unsupported text type")
                }
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                if (inlineButtons.size > 1) {
                    Divider()
                }
                inlineButtons.forEach {
                    WCTextButton(onClick = it.onClick) {
                        Text(text = stringResource(id = it.title))
                    }
                }
                if (inlineButtons.size > 1) {
                    Divider()
                }
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            }

            ButtonBar(
                primaryButton,
                secondaryButton
            )
        }
    }
}

@Composable
private fun ButtonBar(
    primaryButton: LoginErrorButton?,
    secondaryButton: LoginErrorButton?,
    modifier: Modifier = Modifier
) {
    @Composable
    fun Buttons(modifier: Modifier) {
        primaryButton?.let {
            WCColoredButton(
                onClick = primaryButton.onClick,
                modifier = modifier
            ) {
                Text(text = stringResource(id = primaryButton.title))
            }
        }
        secondaryButton?.let {
            WCOutlinedButton(
                onClick = secondaryButton.onClick,
                modifier = modifier
            ) {
                Text(text = stringResource(id = secondaryButton.title))
            }
        }
    }

    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.major_100))
        ) {
            Buttons(modifier = Modifier.weight(1f))
        }
        else -> Column(modifier = modifier) {
            Buttons(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
@Preview("Portrait")
@Preview("Landscape", widthDp = 700, heightDp = 400)
private fun LoginErrorScreenPreview() {
    WooThemeWithBackground {
        LoginErrorScreen(
            text = stringResource(id = string.login_error_generic),
            illustration = drawable.img_woo_generic_error,
            onHelpButtonClick = { },
            inlineButtons = listOf(LoginErrorButton(string.login_try_another_account, {})),
            primaryButton = LoginErrorButton(string.login_try_another_account, {}),
            secondaryButton = LoginErrorButton(string.login_try_another_account, {})
        )
    }
}
