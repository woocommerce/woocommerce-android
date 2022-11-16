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
import androidx.compose.material.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun StoreNamePickerScreen(viewModel: StoreNamePickerViewModel) {
    viewModel.storeName.observeAsState().value?.let { storeName ->
        Scaffold(topBar = {
            Toolbar(
                onCancelPressed = viewModel::onCancelPressed
            )
        }) {
            NamePickerForm(
                storeName = storeName,
                onStoreNameChanged = viewModel::onStoreNameChanged,
                onContinueClicked = viewModel::onContinueClicked,
                modifier = Modifier
                    .background(MaterialTheme.colors.surface)
                    .fillMaxSize()
                    .padding(dimensionResource(id = R.dimen.major_125))
            )
        }
    }
}

@Composable
private fun Toolbar(
    onCancelPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = {},
        navigationIcon = {
            TextButton(onClick = onCancelPressed) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
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
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(id = R.string.store_creation_store_name_subtitle),
                style = MaterialTheme.typography.body1,
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
