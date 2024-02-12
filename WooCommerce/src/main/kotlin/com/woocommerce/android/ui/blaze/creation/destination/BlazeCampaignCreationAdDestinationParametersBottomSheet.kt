package com.woocommerce.android.ui.blaze.creation.destination

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue.Hidden
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationParametersViewModel.ViewState
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationParametersViewModel.ViewState.ParameterBottomSheetState.Editing
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.ModalStatusBarBottomSheetLayout
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AdDestinationParametersBottomSheet(
    viewState: ViewState,
    modalSheetState: ModalBottomSheetState,
    onParameterChanged: (String, String) -> Unit,
    onParameterSaved: (String, String) -> Unit,
    onParameterBottomSheetDismissed: () -> Unit,
    screenContent: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isSheetFullScreen by remember { mutableStateOf(false) }
    val roundedCornerRadius = if (isSheetFullScreen) 0.dp else dimensionResource(id = R.dimen.minor_100)
    val modifier = if (isSheetFullScreen)
        Modifier.fillMaxSize()
    else
        Modifier.fillMaxWidth()

    BackHandler(modalSheetState.isVisible) {
        coroutineScope.launch { modalSheetState.hide() }
    }

    if (modalSheetState.currentValue != Hidden) {
        DisposableEffect(Unit) {
            onDispose {
                onParameterBottomSheetDismissed()
            }
        }
    }

    ModalStatusBarBottomSheetLayout(
        sheetState = modalSheetState,
        sheetShape = RoundedCornerShape(topStart = roundedCornerRadius, topEnd = roundedCornerRadius),
        sheetContent = {
            if (viewState.bottomSheetState is Editing) {
                ParameterBottomSheet(
                    key = viewState.bottomSheetState.key,
                    value = viewState.bottomSheetState.value,
                    onParameterChanged = onParameterChanged,
                    onParameterSaved = onParameterSaved,
                    url = viewState.bottomSheetState.url,
                    modifier = modifier
                )
            }
        }
    ) {
        screenContent()
    }
}

@Composable
private fun ParameterBottomSheet(
    key: String,
    value: String,
    onParameterChanged: (String, String) -> Unit,
    onParameterSaved: (String, String) -> Unit,
    url: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))
        Column(
            modifier = Modifier
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
                value = key,
                label = stringResource(id = R.string.blaze_campaign_edit_ad_destination_parameter_key),
                onValueChange = {
                    onParameterChanged(it, value)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )

            WCOutlinedTextField(
                value = value,
                label = stringResource(id = R.string.blaze_campaign_edit_ad_destination_parameter_value),
                onValueChange = {
                    onParameterChanged(key, it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )

            Text(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
                text = stringResource(R.string.blaze_campaign_edit_ad_destination_destination_with_parameters, url),
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
                onClick = { onParameterSaved(key, value) },
                text = stringResource(id = R.string.save)
            )
        }
    }
}

@LightDarkThemePreviews
@Composable
fun PreviewParameterBottomSheet() {
    ParameterBottomSheet(
        key = "key",
        value = "value",
        onParameterChanged = { _, _ -> },
        onParameterSaved = { _, _ -> },
        url = "https://woocommerce.com",
        modifier = Modifier.fillMaxWidth()
    )
}
