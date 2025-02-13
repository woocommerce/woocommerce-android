package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCOutlinedSpinner
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ContentType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.RestrictionType

@Composable
fun WooShippingCustomsFormScreen(viewModel: WooShippingCustomsFormViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    WooShippingCustomsFormScreen(
        contentType = viewState?.contentType ?: ContentType.MERCHANDISE,
        restrictionType = viewState?.restrictionType ?: RestrictionType.NONE,
        itnValue = viewState?.itnValue.orEmpty(),
        otherContentDetailsInput = viewState?.otherContentInput.orEmpty(),
        otherRestrictionDetailsInput = viewState?.otherRestrictionInput.orEmpty(),
        returnToSenderChecked = viewState?.returnToSenderChecked ?: false,
        isAddCustomsButtonEnabled = viewState?.isAddCustomsButtonEnabled ?: false,
        shouldDisplayContentTypeInput = viewState?.shouldDisplayContentTypeInput ?: false,
        shouldDisplayRestrictionTypeInput = viewState?.shouldDisplayRestrictionTypeInput ?: false,
        onContentTypeClick = viewModel::onContentTypeClick,
        onRestrictionTypeClick = viewModel::onRestrictionTypeClick,
        onItnChanged = viewModel::onITNChanged,
        onOtherContentDetailsInputChanged = viewModel::onOtherContentInputChanged,
        onOtherRestrictionDetailsInputChanged = viewModel::onRestrictionDetailsInputChanged,
        onReturnToSenderChanged = viewModel::onReturnToSenderChanged,
    ) {
    }
}

@Composable
fun WooShippingCustomsFormScreen(
    modifier: Modifier = Modifier,
    contentType: ContentType,
    restrictionType: RestrictionType,
    itnValue: String,
    otherContentDetailsInput: String,
    otherRestrictionDetailsInput: String,
    returnToSenderChecked: Boolean,
    isAddCustomsButtonEnabled: Boolean,
    shouldDisplayContentTypeInput: Boolean,
    shouldDisplayRestrictionTypeInput: Boolean,
    onContentTypeClick: () -> Unit,
    onRestrictionTypeClick: () -> Unit,
    onItnChanged: (String) -> Unit,
    onReturnToSenderChanged: (Boolean) -> Unit,
    onOtherContentDetailsInputChanged: (String) -> Unit,
    onOtherRestrictionDetailsInputChanged: (String) -> Unit,
    onAddCustomsDataClick: () -> Unit

) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WCOutlinedSpinner(
                onClick = onContentTypeClick,
                value = stringResource(id = contentType.resourceId),
                label = stringResource(id = R.string.woo_shipping_labels_customs_content_type_label),
                modifier = modifier.fillMaxWidth()
            )

            AnimatedVisibility(shouldDisplayContentTypeInput) {
                WCOutlinedTextField(
                    value = otherContentDetailsInput,
                    onValueChange = onOtherContentDetailsInputChanged,
                    label = stringResource(id = R.string.woo_shipping_labels_customs_content_details_label),
                    singleLine = true,
                    helperText = stringResource(id = R.string.woo_shipping_labels_customs_content_details_description),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = modifier.fillMaxWidth()
                )
            }

            WCOutlinedSpinner(
                onClick = onRestrictionTypeClick,
                value = stringResource(id = restrictionType.resourceId),
                label = stringResource(id = R.string.woo_shipping_labels_customs_restriction_type_label),
                modifier = modifier.fillMaxWidth()
            )

            AnimatedVisibility(shouldDisplayRestrictionTypeInput) {
                WCOutlinedTextField(
                    value = otherRestrictionDetailsInput,
                    onValueChange = onOtherRestrictionDetailsInputChanged,
                    label = stringResource(id = R.string.woo_shipping_labels_customs_restriction_details_label),
                    singleLine = true,
                    helperText = stringResource(
                        id = R.string.woo_shipping_labels_customs_restriction_details_description
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = modifier.fillMaxWidth()
                )
            }

            WCOutlinedTextField(
                value = itnValue,
                onValueChange = onItnChanged,
                label = stringResource(id = R.string.woo_shipping_labels_customs_itn_label),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                modifier = modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.woo_shipping_labels_customs_return_to_sender_label),
                    modifier = modifier
                        .align(Alignment.CenterVertically)
                        .weight(1f)
                )
                Checkbox(
                    checked = returnToSenderChecked,
                    onCheckedChange = onReturnToSenderChanged,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colors.primary,
                        uncheckedColor = MaterialTheme.colors.onSurface
                    )
                )
            }
        }
        Button(
            modifier = modifier.fillMaxWidth(),
            enabled = isAddCustomsButtonEnabled,
            onClick = onAddCustomsDataClick
        ) {
            Text(stringResource(id = R.string.woo_shipping_labels_customs_add_missing_information))
        }
    }
}

@Preview
@Composable
fun PreviewWooShippingCustomsFormScreen() {
    WooThemeWithBackground {
        WooShippingCustomsFormScreen(
            contentType = ContentType.MERCHANDISE,
            restrictionType = RestrictionType.NONE,
            itnValue = "123456",
            otherContentDetailsInput = "Important Stuff",
            otherRestrictionDetailsInput = "Restricted Stuff",
            returnToSenderChecked = false,
            isAddCustomsButtonEnabled = true,
            shouldDisplayContentTypeInput = true,
            shouldDisplayRestrictionTypeInput = false,
            onContentTypeClick = {},
            onRestrictionTypeClick = {},
            onItnChanged = {},
            onReturnToSenderChanged = {},
            onOtherContentDetailsInputChanged = {},
            onOtherRestrictionDetailsInputChanged = {},
            onAddCustomsDataClick = {}
        )
    }
}
