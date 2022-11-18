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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.woocommerce.android.R
import com.woocommerce.android.compose.utils.toAnnotatedString
import com.woocommerce.android.ui.compose.URL_ANNOTATION_TAG
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.signup.SignUpViewModel.ErrorType
import com.woocommerce.android.ui.login.signup.SignUpViewModel.SignUpState

@Composable
fun SignUpScreen(viewModel: SignUpViewModel) {
    val state by viewModel.viewState.observeAsState(SignUpState())

    BackHandler(onBack = viewModel::onBackPressed)
    Scaffold(topBar = {
        Toolbar(onArrowBackPressed = viewModel::onBackPressed)
    }) {
        when {
            state.isLoading ->
                ProgressDialog(
                    title = "",
                    subtitle = stringResource(id = R.string.signup_creating_account_loading_message)
                )
            else -> SignUpForm(
                termsOfServiceClicked = viewModel::onTermsOfServiceClicked,
                onPrimaryButtonClicked = viewModel::onGetStartedCLicked,
                onLoginClicked = viewModel::onLoginClicked,
                signUpState = state
            )
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
    termsOfServiceClicked: () -> Unit,
    onPrimaryButtonClicked: (String, String) -> Unit,
    onLoginClicked: () -> Unit,
    signUpState: SignUpState,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var email by remember { mutableStateOf(signUpState.email ?: "") }
    var password by remember { mutableStateOf(signUpState.password ?: "") }

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
        LoginToExistingAccountText(onLoginClicked)
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        val isEmailError = signUpState.error?.type == ErrorType.EMAIL
        WCOutlinedTextField(
            modifier = Modifier.focusRequester(focusRequester),
            value = email,
            onValueChange = { email = it },
            label = stringResource(id = R.string.signup_email_address_hint),
            isError = isEmailError,
            helperText = if (isEmailError) signUpState.error?.stringId?.let { stringResource(id = it) } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        val isPasswordError = signUpState.error?.type == ErrorType.PASSWORD
        WCPasswordField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(id = R.string.signup_password_hint),
            isError = isPasswordError,
            helperText = if (isPasswordError) signUpState.error?.stringId?.let { stringResource(id = it) } else null,
            keyboardActions = KeyboardActions(onDone = { onPrimaryButtonClicked(email, password) })
        )
        TermsOfServiceText(
            modifier = Modifier.clickable { termsOfServiceClicked() }
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        WCColoredButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onPrimaryButtonClicked(email, password) },
            enabled = email.isNotBlank() && password.isNotBlank()
        ) {
            Text(text = stringResource(id = R.string.signup_get_started_button))
        }
    }
    // Request focus on email field when entering screen
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun TermsOfServiceText(modifier: Modifier = Modifier) {
    Text(
        text = HtmlCompat
            .fromHtml(
                stringResource(id = R.string.signup_terms_of_service),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            ).toAnnotatedString(),
        style = MaterialTheme.typography.body2,
        modifier = modifier,
    )
}

@Composable
private fun LoginToExistingAccountText(onLoginClicked: () -> Unit) {
    val text = annotatedStringRes(stringResId = R.string.signup_already_registered)
    ClickableText(
        text = text,
        style = MaterialTheme.typography.body1.copy(color = colorResource(id = R.color.color_on_surface_medium)),
    ) {
        text.getStringAnnotations(tag = URL_ANNOTATION_TAG, start = it, end = it)
            .firstOrNull()
            ?.let { onLoginClicked() }
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun SignUpFormPreview() {
    WooThemeWithBackground {
        SignUpForm(
            termsOfServiceClicked = {},
            onPrimaryButtonClicked = { _, _ -> },
            onLoginClicked = {},
            signUpState = SignUpState()
        )
    }
}
