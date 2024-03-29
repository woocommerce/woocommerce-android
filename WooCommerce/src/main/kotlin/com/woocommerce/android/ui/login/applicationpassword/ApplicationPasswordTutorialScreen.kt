package com.woocommerce.android.ui.login.applicationpassword

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun ApplicationPasswordTutorialScreen(viewModel: ApplicationPasswordTutorialViewModel) {
    ApplicationPasswordTutorialScreen(
        onContinueClicked = viewModel::onContinueClicked,
        onContactSupportClicked = viewModel::onContactSupportClicked
    )
}

@Composable
fun ApplicationPasswordTutorialScreen(
    modifier: Modifier = Modifier,
    onContinueClicked: () -> Unit,
    onContactSupportClicked: () -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
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
            Text(stringResource(id = R.string.login_app_password_subtitle))
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
                painter = painterResource(id = R.drawable.stats_today_widget_preview),
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
            onContinueClicked = { },
            onContactSupportClicked = { }
        )
    }
}
