package com.woocommerce.android.ui.sitepicker.sitediscovery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.DialogButtonsRowLayout
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.sitepicker.sitediscovery.SitePickerSiteDiscoveryViewModel.ViewState.AddressInputState
import com.woocommerce.android.ui.sitepicker.sitediscovery.SitePickerSiteDiscoveryViewModel.ViewState.ErrorState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SitePickerSiteDiscoveryScreen(viewModel: SitePickerSiteDiscoveryViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        Scaffold(topBar = {
            Toolbar(
                onHelpButtonClick = viewModel::onHelpButtonClick,
                onBackButtonClick = viewModel::onBackButtonClick
            )
        }) { paddingValues ->
            val transition = updateTransition(viewState, label = "ViewStateTransition")
            transition.AnimatedContent(
                contentKey = { viewState::class.java },
            ) { targetState ->
                when (targetState) {
                    is AddressInputState -> AddressInputView(
                        targetState,
                        Modifier.padding(paddingValues)
                    )
                    is ErrorState -> ErrorView(
                        targetState,
                        Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressInputView(
    state: AddressInputState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(color = MaterialTheme.colors.surface)
            .padding(dimensionResource(id = R.dimen.major_100)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
    ) {
        Text(text = stringResource(id = R.string.enter_site_address))
        WCOutlinedTextField(
            value = state.siteAddress,
            onValueChange = state.onAddressChanged,
            label = stringResource(id = R.string.login_site_address),
            isError = state.inlineErrorMessage != 0,
            helperText = state.inlineErrorMessage.takeIf { it != 0 }?.let { stringResource(id = it) },
            maxLines = 1
        )
        WCTextButton(onClick = state.onShowSiteAddressTapped) {
            Text(text = stringResource(id = R.string.login_find_your_site_adress))
        }
        Spacer(modifier = Modifier.weight(1f))
        WCColoredButton(
            onClick = state.onContinueTapped,
            enabled = state.isAddressValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.continue_button))
        }

        if (state.isAddressSiteHelpShown) {
            SiteAddressHelpDialog(state.onSiteAddressHelpDismissed, state.onMoreHelpTapped)
        }

        if (state.isLoading) {
            ProgressDialog(title = "", subtitle = stringResource(id = R.string.login_checking_site_address))
        }
    }
}

@Composable
fun SiteAddressHelpDialog(
    onDismissed: () -> Unit,
    onMoreHelpTapped: () -> Unit
) {
    Dialog(onDismissRequest = onDismissed) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface, MaterialTheme.shapes.medium)
                .padding(dimensionResource(id = R.dimen.major_100)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
        ) {
            Text(text = stringResource(id = R.string.login_site_address_help_content))
            Image(
                painter = painterResource(id = R.drawable.login_site_address_help),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )

            DialogButtonsRowLayout(
                confirmButton = {
                    WCTextButton(onClick = onDismissed) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                },
                dismissButton = {},
                neutralButton = {
                    WCTextButton(onClick = onMoreHelpTapped) {
                        Text(text = stringResource(id = R.string.login_site_address_more_help))
                    }
                }
            )
        }
    }
}

@Composable
fun ErrorView(viewState: ErrorState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(color = MaterialTheme.colors.surface)
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(
                space = dimensionResource(id = R.dimen.major_100),
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painter = painterResource(id = viewState.imageResourceId), contentDescription = null)
            Text(text = viewState.message, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        WCColoredButton(onClick = viewState.primaryButtonAction, modifier = Modifier.fillMaxWidth()) {
            Text(text = viewState.primaryButtonText)
        }
        WCOutlinedButton(onClick = viewState.secondaryButtonAction, modifier = Modifier.fillMaxWidth()) {
            Text(text = viewState.secondaryButtonText)
        }
    }
}

@Composable
private fun Toolbar(
    onHelpButtonClick: () -> Unit,
    onBackButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = { Text(stringResource(id = R.string.login_site_picker_enter_site_address)) },
        navigationIcon = {
            IconButton(onClick = onBackButtonClick) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        actions = {
            IconButton(onClick = onHelpButtonClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_help_24dp),
                    contentDescription = stringResource(id = R.string.help)
                )
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}

@Composable
@Preview
private fun AddressInputViewPreview() {
    WooThemeWithBackground {
        AddressInputView(
            state = AddressInputState(
                siteAddress = "",
                isAddressValid = true,
                isAddressSiteHelpShown = false,
                isLoading = false,
                inlineErrorMessage = 0,
                onAddressChanged = {},
                onShowSiteAddressTapped = {},
                onContinueTapped = {},
                onSiteAddressHelpDismissed = {},
                onMoreHelpTapped = {}
            )
        )
    }
}
