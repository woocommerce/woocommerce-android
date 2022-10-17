package com.woocommerce.android.ui.login.signup

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.login.signup.SignUpViewModel.Error
import com.woocommerce.android.ui.login.signup.SignUpViewModel.Loading
import com.woocommerce.android.ui.login.signup.SignUpViewModel.SignUpForm

@Composable
fun SignUpScreen(viewModel: SignUpViewModel) {
    val signUpState by viewModel.viewState.observeAsState(SignUpForm)

    BackHandler(onBack = viewModel::onBackPressed)
    Scaffold(topBar = {
        Toolbar(onArrowBackPressed = viewModel::onBackPressed)
    }) {
        when (signUpState) {
            SignUpForm -> SignUpForm(
                termsOfServiceClicked = viewModel::onTermsOfServiceClicked,
                onPrimaryButtonClicked = viewModel::onGetStartedCLicked,
            )
            Loading ->
                ProgressDialog(
                    title = "",
                    subtitle = stringResource(id = R.string.signup_creating_account_loading_message)
                )
            Error -> TODO()
        }
    }
}

@Composable
private fun Toolbar(
    onArrowBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = {},
        navigationIcon = {
            IconButton(onClick = onArrowBackPressed) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}

@Composable
private fun SignUpForm(
    modifier: Modifier = Modifier,
    termsOfServiceClicked: () -> Unit,
    onPrimaryButtonClicked: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(id = R.dimen.major_125)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
    ) {
        Text(
            text = stringResource(id = R.string.signup_get_started_label),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(id = R.string.signup_already_registered),
            style = MaterialTheme.typography.body1,
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        WCOutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(id = R.string.signup_email_address_hint),
        )
        WCPasswordField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(id = R.string.signup_password_hint),
        )
        TermsOfServiceText(
            modifier = Modifier.clickable { termsOfServiceClicked() }
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        WCColoredButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onPrimaryButtonClicked(email, password) }
        ) {
            Text(text = stringResource(id = R.string.signup_get_started_button))
        }
    }
}

@Composable
private fun TermsOfServiceText(modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            append(stringResource(id = R.string.signup_terms_of_service_description))
            append(" ")
            pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            append(stringResource(id = R.string.signup_terms_of_service_linked_text))
            toAnnotatedString()
        },
        style = MaterialTheme.typography.body2,
        modifier = modifier,
    )
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun SignUpFormPreview() {
    SignUpForm(
        termsOfServiceClicked = {},
        onPrimaryButtonClicked = { _, _ -> },
    )
}
