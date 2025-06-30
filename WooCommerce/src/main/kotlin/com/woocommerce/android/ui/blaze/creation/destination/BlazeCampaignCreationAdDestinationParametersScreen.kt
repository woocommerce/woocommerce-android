package com.woocommerce.android.ui.blaze.creation.destination

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationParametersViewModel.ViewState
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationParametersViewModel.ViewState.ParameterBottomSheetState
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCModalBottomSheet
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BlazeCampaignCreationAdDestinationParametersScreen(
    viewModel: BlazeCampaignCreationAdDestinationParametersViewModel
) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        AdDestinationParametersScreen(
            viewState,
            viewModel::onBackPressed,
            viewModel::onAddParameterTapped,
            viewModel::onParameterTapped,
            viewModel::onDeleteParameterTapped,
            viewModel::onParameterChanged,
            viewModel::onParameterSaved,
            viewModel::onParameterBottomSheetDismissed
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdDestinationParametersScreen(
    viewState: ViewState,
    onBackPressed: () -> Unit,
    onAddParameterTapped: () -> Unit,
    onParameterTapped: (String) -> Unit,
    onDeleteParameterTapped: (String) -> Unit,
    onParameterChanged: (String, String) -> Unit,
    onParameterSaved: (String, String) -> Unit,
    onParameterBottomSheetDismissed: () -> Unit
) {
    val modalSheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.blaze_campaign_edit_ad_destination_parameters_property_title),
                onNavigationButtonClick = onBackPressed,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack
            )
        },
        modifier = Modifier.background(MaterialTheme.colors.surface)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            item(key = "header") {
                WCTextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(id = R.dimen.minor_50)),
                    onClick = onAddParameterTapped,
                    text = stringResource(id = R.string.blaze_campaign_edit_ad_destination_add_parameter_button),
                    icon = Icons.Default.Add
                )
            }

            items(
                items = viewState.parameters.entries.toList(),
                key = { item -> "key${item.key}" }
            ) { (key, value) ->
                ParameterItem(
                    onParameterTapped = onParameterTapped,
                    key = key,
                    value = value,
                    onDeleteParameterTapped = onDeleteParameterTapped,
                    modifier = Modifier.animateItem()
                )
            }

            item(key = "footer") {
                Column(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .padding(
                                start = dimensionResource(id = R.dimen.major_100),
                                end = dimensionResource(id = R.dimen.major_100),
                                top = dimensionResource(id = R.dimen.major_100),
                                bottom = dimensionResource(id = R.dimen.minor_100)
                            ),
                        text = stringResource(
                            R.string.blaze_campaign_edit_ad_characters_remaining,
                            viewState.charactersRemaining
                        ),
                        style = MaterialTheme.typography.caption,
                        color = colorResource(id = R.color.color_on_surface_medium)
                    )
                    Text(
                        modifier = Modifier
                            .padding(
                                horizontal = dimensionResource(id = R.dimen.major_100),
                            ),
                        text = stringResource(
                            R.string.blaze_campaign_edit_ad_destination_destination_with_parameters,
                            viewState.url
                        ),
                        style = MaterialTheme.typography.caption,
                        color = colorResource(id = R.color.color_on_surface_medium)
                    )
                }
            }
        }
        if (viewState.bottomSheetState is ParameterBottomSheetState.Editing) {
            WCModalBottomSheet(
                onDismissRequest = onParameterBottomSheetDismissed,
                sheetState = modalSheetState,
                modifier = Modifier.imeNestedScroll(),
            ) {
                ParameterBottomSheetContent(
                    paramsState = viewState.bottomSheetState,
                    onParameterChanged = onParameterChanged,
                    onParameterSaved = onParameterSaved,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ParameterItem(
    key: String,
    value: String,
    onParameterTapped: (String) -> Unit,
    onDeleteParameterTapped: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable { onParameterTapped(key) }
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    top = dimensionResource(id = R.dimen.minor_100),
                    bottom = dimensionResource(id = R.dimen.minor_100)
                ),
            verticalAlignment = CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colorResource(id = R.color.color_on_surface_medium)
                )
            }
            IconButton(
                onClick = { onDeleteParameterTapped(key) }
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(id = R.string.delete),
                    tint = colorResource(id = R.color.color_on_surface_medium)
                )
            }
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@LightDarkThemePreviews
@Composable
fun PreviewAdDestinationParametersScreen() {
    WooThemeWithBackground {
        AdDestinationParametersScreen(
            viewState = ViewState(
                targetUrl = "https://woocommerce.com",
                parameters = mapOf(
                    "utm_source" to "woocommerce",
                    "utm_medium" to "android",
                    "utm_campaign" to "blaze"
                ),
                bottomSheetState = ViewState.ParameterBottomSheetState.Hidden
            ),
            onBackPressed = {},
            onAddParameterTapped = {},
            onParameterTapped = {},
            onDeleteParameterTapped = {},
            onParameterChanged = { _, _ -> },
            onParameterSaved = { _, _ -> },
            onParameterBottomSheetDismissed = {}
        )
    }
}

@Composable
private fun ParameterBottomSheetContent(
    paramsState: ParameterBottomSheetState.Editing,
    onParameterChanged: (String, String) -> Unit,
    onParameterSaved: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = dimensionResource(id = R.dimen.major_100)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))
    ) {
        Text(
            text = stringResource(id = R.string.blaze_campaign_edit_ad_destination_add_parameter_button),
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )

        Divider(
            modifier = Modifier
                .padding(top = dimensionResource(id = R.dimen.minor_100)),
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10),
        )

        WCOutlinedTextField(
            value = paramsState.key,
            label = stringResource(id = R.string.blaze_campaign_edit_ad_destination_parameter_key),
            onValueChange = {
                onParameterChanged(it, paramsState.value)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            isError = paramsState.error != 0,
            helperText = if (paramsState.error != 0) stringResource(paramsState.error) else null,
        )

        WCOutlinedTextField(
            value = paramsState.value,
            label = stringResource(id = R.string.blaze_campaign_edit_ad_destination_parameter_value),
            onValueChange = {
                onParameterChanged(paramsState.key, it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
        )

        Text(
            modifier = Modifier
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            text = stringResource(
                R.string.blaze_campaign_edit_ad_destination_destination_with_parameters,
                paramsState.url
            ),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_on_surface_medium)
        )

        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    end = dimensionResource(id = R.dimen.major_100),
                    top = dimensionResource(id = R.dimen.minor_100)
                ),
            onClick = { onParameterSaved(paramsState.key, paramsState.value) },
            text = stringResource(id = R.string.save),
            enabled = paramsState.isSaveButtonEnabled
        )
    }
}

@LightDarkThemePreviews
@Composable
fun PreviewEmptyAdDestinationParametersScreen() {
    WooThemeWithBackground {
        AdDestinationParametersScreen(
            viewState = ViewState(
                targetUrl = "https://woocommerce.com?utm_source=woocommerce&utm_medium=android&utm_campaign=blaze",
                parameters = emptyMap(),
                bottomSheetState = ParameterBottomSheetState.Hidden
            ),
            onBackPressed = {},
            onAddParameterTapped = {},
            onParameterTapped = {},
            onDeleteParameterTapped = {},
            onParameterChanged = { _, _ -> },
            onParameterSaved = { _, _ -> },
            onParameterBottomSheetDismissed = {}
        )
    }
}
