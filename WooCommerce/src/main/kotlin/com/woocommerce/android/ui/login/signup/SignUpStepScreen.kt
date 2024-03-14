package com.woocommerce.android.ui.login.signup

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.URL_ANNOTATION_TAG
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.signup.SignUpViewModel.ErrorType
import com.woocommerce.android.ui.login.signup.SignUpViewModel.SignUpState
import com.woocommerce.android.ui.login.signup.SignUpViewModel.SignUpStepType

@Composable
fun SignUpStepScreen(viewModel: SignUpViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        BackHandler(onBack = viewModel::onBackPressed)
        Scaffold(topBar = {
            Toolbar(
                title = "",
                onNavigationButtonClick = viewModel::onBackPressed,
                actions = {
                    TextButton(onClick = viewModel::onLoginClicked) {
                        Text(text = stringResource(id = R.string.log_in))
                    }
                }
            )
        }) { padding ->
            AnimatedVisibility(
                visible = state.stepType == SignUpStepType.EMAIL,
                enter = slideInHorizontally(animationSpec = tween()) { fullWidth -> -fullWidth },
                exit = slideOutHorizontally(animationSpec = tween()) { fullWidth -> -fullWidth },
            ) {
                SignUpEmailForm(
                    state = state,
                    termsOfServiceClicked = viewModel::onTermsOfServiceClicked,
                    onPrimaryButtonClicked = viewModel::onEmailContinueClicked,
                    onEmailInputChanged = viewModel::onEmailInputChanged,
                    modifier = Modifier.padding(padding)
                )
            }
            AnimatedVisibility(
                visible = state.stepType == SignUpStepType.PASSWORD,
                enter = slideInHorizontally(animationSpec = tween()) { fullWidth -> fullWidth },
                exit = slideOutHorizontally(animationSpec = tween()) { fullWidth -> fullWidth },
            ) {
                SignUpPasswordForm(
                    state = state,
                    termsOfServiceClicked = viewModel::onTermsOfServiceClicked,
                    onPrimaryButtonClicked = viewModel::onPasswordContinueClicked,
                    onPasswordInputChanged = viewModel::onPasswordInputChanged,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun SignUpEmailForm(
    state: SignUpState,
    termsOfServiceClicked: () -> Unit,
    onPrimaryButtonClicked: () -> Unit,
    onEmailInputChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100)),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))
        ) {
            Text(
                text = stringResource(id = R.string.signup_get_started_label),
                style = MaterialTheme.typography.h5,
            )
            Text(
                text = stringResource(id = R.string.signup_get_started_create_account_label),
                style = MaterialTheme.typography.body1
                    .copy(color = colorResource(id = R.color.color_on_surface_medium)),
            )

            val isEmailError = state.error?.type == ErrorType.EMAIL
            WCOutlinedTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(top = dimensionResource(id = R.dimen.minor_100)),
                value = state.email,
                onValueChange = { onEmailInputChanged(it) },
                label = stringResource(id = R.string.signup_email_address_hint),
                isError = isEmailError,
                helperText = if (isEmailError) state.error?.stringId?.let { stringResource(id = it) } else null,
                singleLine = true,
                keyboardActions = KeyboardActions(onDone = { onPrimaryButtonClicked() })
            )
        }
        SignUpFooter(termsOfServiceClicked, onPrimaryButtonClicked, state.email, state.isLoading)
    }
    // Request focus on email field when entering screen
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun SignUpPasswordForm(
    termsOfServiceClicked: () -> Unit,
    onPrimaryButtonClicked: () -> Unit,
    onPasswordInputChanged: (String) -> Unit,
    state: SignUpState,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100)),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))
        ) {
            Text(
                text = stringResource(id = R.string.signup_create_password_label),
                style = MaterialTheme.typography.h5,
            )
            Text(
                text = stringResource(id = R.string.signup_get_started_create_account_label),
                style = MaterialTheme.typography.body1
                    .copy(color = colorResource(id = R.color.color_on_surface_medium)),
            )

            val isPasswordError = state.error?.type == ErrorType.PASSWORD
            WCPasswordField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(top = dimensionResource(id = R.dimen.minor_100)),
                value = state.password,
                onValueChange = { onPasswordInputChanged(it) },
                label = stringResource(id = R.string.signup_password_hint),
                isError = isPasswordError,
                helperText = if (isPasswordError) {
                    state.error?.stringId
                        ?.let { stringResource(id = it) }
                } else {
                    null
                },
                keyboardActions = KeyboardActions(onDone = { onPrimaryButtonClicked() })
            )
        }
        SignUpFooter(termsOfServiceClicked, onPrimaryButtonClicked, state.password)
    }
    // Request focus on email field when entering screen
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    if (state.isLoading) {
        ProgressDialog(
            title = "",
            subtitle = stringResource(id = R.string.signup_creating_account_loading_message)
        )
    }
}

@Composable
private fun ColumnScope.SignUpFooter(
    termsOfServiceClicked: () -> Unit,
    onPrimaryButtonClicked: () -> Unit,
    textInput: String,
    isLoading: Boolean = false
) {
    TermsOfServiceText(
        onTermsOfServiceClicked = termsOfServiceClicked,
        modifier = Companion
            .align(Alignment.CenterHorizontally)
            .padding(bottom = dimensionResource(id = R.dimen.minor_100))
    )
    WCColoredButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onPrimaryButtonClicked() },
        enabled = textInput.isNotBlank()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size = dimensionResource(id = R.dimen.major_150)),
                color = colorResource(id = R.color.color_on_primary_surface),
            )
        } else {
            Text(text = stringResource(id = R.string.continue_button))
        }
    }
}

@Composable
private fun TermsOfServiceText(
    onTermsOfServiceClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val text = annotatedStringRes(stringResId = R.string.signup_terms_of_service)
    ClickableText(
        text = text,
        style = MaterialTheme.typography.caption.copy(color = colorResource(id = R.color.color_on_surface_medium)),
        modifier = modifier
    ) {
        text.getStringAnnotations(tag = URL_ANNOTATION_TAG, start = it, end = it)
            .firstOrNull()
            ?.let { onTermsOfServiceClicked() }
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
        SignUpEmailForm(
            termsOfServiceClicked = {},
            onPrimaryButtonClicked = {},
            onEmailInputChanged = {},
            state = SignUpState(SignUpStepType.EMAIL),
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_350)))
        SignUpPasswordForm(
            termsOfServiceClicked = {},
            onPrimaryButtonClicked = {},
            onPasswordInputChanged = {},
            state = SignUpState(SignUpStepType.PASSWORD),
        )
    }
}
