package com.woocommerce.android.ui.blaze.creation.destination

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationParametersViewModel.ViewState
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationParametersViewModel.ViewState.ParameterBottomSheetState.Editing
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.ModalStatusBarBottomSheetLayout
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AdDestinationParametersBottomSheet(
    viewState: ViewState,
    modalSheetState: ModalBottomSheetState,
    onParameterChanged: (String, String) -> Unit,
    onParameterSaved: (String, String) -> Unit,
    onParameterBottomSheetDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    screenContent: @Composable () -> Unit,
) {
    val roundedCornerRadius = dimensionResource(id = R.dimen.major_100)

    BackHandler(modalSheetState.isVisible) {
        onParameterBottomSheetDismissed()
    }

    LaunchedEffect(modalSheetState.currentValue) {
        if (modalSheetState.currentValue == Hidden) {
            onParameterBottomSheetDismissed()
        }
    }

    ModalStatusBarBottomSheetLayout(
        sheetState = modalSheetState,
        sheetShape = RoundedCornerShape(topStart = roundedCornerRadius, topEnd = roundedCornerRadius),
        sheetContent = {
            if (viewState.bottomSheetState is Editing) {
                ParameterBottomSheet(
                    paramsState = viewState.bottomSheetState,
                    onParameterChanged = onParameterChanged,
                    onParameterSaved = onParameterSaved,
                    modifier = modifier.fillMaxWidth()
                )
            }
        }
    ) {
        screenContent()
    }
}

@Composable
private fun ParameterBottomSheet(
    paramsState: Editing,
    onParameterChanged: (String, String) -> Unit,
    onParameterSaved: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
                    R.string.blaze_campaign_edit_ad_destination_destination_with_parameters, paramsState.url
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
}

@LightDarkThemePreviews
@Composable
fun PreviewParameterBottomSheet() {
    WooThemeWithBackground {
        ParameterBottomSheet(
            paramsState = Editing(
                targetUrl = "https://example.com",
                parameters = emptyMap(),
                key = "key",
                value = "value"
            ),
            onParameterChanged = { _, _ -> },
            onParameterSaved = { _, _ -> },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
