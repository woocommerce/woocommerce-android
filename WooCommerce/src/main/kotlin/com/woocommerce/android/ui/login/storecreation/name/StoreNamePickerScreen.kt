package com.woocommerce.android.ui.login.storecreation.name

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.AlertDialog
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun StoreNamePickerScreen(viewModel: StoreNamePickerViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        StoreNamePickerScreen(
            storeName = it.storeName,
            isPermissionDialogVisible = it.isPermissionRationaleVisible,
            onCancelPressed = viewModel::onCancelPressed,
            onHelpPressed = viewModel::onHelpPressed,
            onStoreNameChanged = viewModel::onStoreNameChanged,
            onContinueClicked = viewModel::onContinueClicked,
            onPermissionRationaleDismissed = viewModel::onPermissionRationaleDismissed,
            onPermissionRationaleConfirmed = viewModel::onPermissionRationaleAccepted
        )
    }
}

@Composable
private fun StoreNamePickerScreen(
    storeName: String,
    isPermissionDialogVisible: Boolean,
    onCancelPressed: () -> Unit,
    onHelpPressed: () -> Unit,
    onStoreNameChanged: (String) -> Unit,
    onContinueClicked: () -> Unit,
    onPermissionRationaleDismissed: () -> Unit,
    onPermissionRationaleConfirmed: () -> Unit
) {
    Scaffold(topBar = {
        ToolbarWithHelpButton(
            onNavigationButtonClick = onCancelPressed,
            onHelpButtonClick = onHelpPressed,
        )
    }) { padding ->
        NamePickerForm(
            storeName = storeName,
            onStoreNameChanged = onStoreNameChanged,
            onContinueClicked = onContinueClicked,
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
                .padding(padding)
                .padding(dimensionResource(id = R.dimen.major_125))
        )
    }

    if (isPermissionDialogVisible) {
        AlertDialog(
            onDismissRequest = onPermissionRationaleDismissed,
            title = {
                Text(text = stringResource(id = R.string.notifications_permission_title))
            },
            text = {
                Text(text = stringResource(id = R.string.notifications_permission_description))
            },
            confirmButton = {
                TextButton(onClick = onPermissionRationaleConfirmed) {
                    Text(stringResource(id = R.string.allow))
                }
            },
            dismissButton = {
                TextButton(onClick = onPermissionRationaleDismissed) {
                    Text(stringResource(id = R.string.dismiss))
                }
            },
            neutralButton = {
            }
        )
    }
}

@Composable
private fun NamePickerForm(
    storeName: String,
    onStoreNameChanged: (String) -> Unit,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        ) {
            Text(
                text = stringResource(id = R.string.store_creation_store_name_caption).uppercase(),
                style = MaterialTheme.typography.caption,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
            Text(
                text = stringResource(id = R.string.store_creation_store_name_title),
                style = MaterialTheme.typography.h5,
            )
            Text(
                text = stringResource(id = R.string.store_creation_store_name_subtitle),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
            WCOutlinedTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(top = dimensionResource(id = R.dimen.major_100)),
                value = storeName,
                onValueChange = onStoreNameChanged,
                label = stringResource(id = R.string.store_creation_store_name_hint),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }
        WCColoredButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinueClicked,
            enabled = storeName.isNotBlank()
        ) {
            Text(text = stringResource(id = R.string.continue_button))
        }
    }
    // Request focus on store name field when entering screen
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun NamePickerPreview() {
    WooThemeWithBackground {
        NamePickerForm(
            storeName = "White Christmas Tress",
            onContinueClicked = {},
            onStoreNameChanged = {}
        )
    }
}
