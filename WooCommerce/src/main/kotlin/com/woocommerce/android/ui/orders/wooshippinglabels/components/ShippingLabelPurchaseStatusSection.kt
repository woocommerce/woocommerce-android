package com.woocommerce.android.ui.orders.wooshippinglabels.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.LabelPurchaseStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.RoundedCornerBoxWithBorder
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingLabelPaperSize

@Composable
fun ShippingLabelPurchaseStatusSection(
    labelPurchaseStatus: LabelPurchaseStatus,
    selectedLabelPaperSizeOption: WooShippingLabelPaperSize,
    onLabelPaperSizeOptionSelected: (WooShippingLabelPaperSize) -> Unit,
    onPrintShippingLabelClicked: () -> Unit,
    onTrackShipmentClicked: () -> Unit,
    onSchedulePickUpClicked: () -> Unit,
    onRefundClicked: () -> Unit,
    onPrintCustomsClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (labelPurchaseStatus) {
        LabelPurchaseStatus.Idle -> {
            // Empty
        }

        LabelPurchaseStatus.PurchaseInProgress -> {
            PurchaseInProgressNotice(modifier.padding(vertical = 16.dp))
        }

        is LabelPurchaseStatus.Purchased -> {
            PurchasedLabelSection(
                status = labelPurchaseStatus,
                availablePaperSizes = labelPurchaseStatus.availablePrintSizes,
                selectedLabelPaperSizeOption = selectedLabelPaperSizeOption,
                onLabelPaperSizeOptionSelected = onLabelPaperSizeOptionSelected,
                onPrintShippingLabelClicked = onPrintShippingLabelClicked,
                onTrackShipmentClicked = onTrackShipmentClicked,
                onSchedulePickUpClicked = onSchedulePickUpClicked,
                onRefundClicked = onRefundClicked,
                onPrintCustomsClicked = onPrintCustomsClicked,
                onLearnMoreClicked = onLearnMoreClicked,
                modifier = modifier.padding(vertical = 16.dp)
            )
        }

        is LabelPurchaseStatus.Failed -> {
            PurchaseFailureNotice(
                errorMessage = labelPurchaseStatus.errorMessage,
                modifier = modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun PurchasedLabelSection(
    status: LabelPurchaseStatus.Purchased,
    availablePaperSizes: List<WooShippingLabelPaperSize>,
    selectedLabelPaperSizeOption: WooShippingLabelPaperSize,
    onLabelPaperSizeOptionSelected: (WooShippingLabelPaperSize) -> Unit,
    onPrintShippingLabelClicked: () -> Unit,
    onTrackShipmentClicked: () -> Unit,
    onSchedulePickUpClicked: () -> Unit,
    onRefundClicked: () -> Unit,
    onPrintCustomsClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.shipping_label_purchased_success_title),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(id = R.string.shipping_label_purchased_success_message),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.subtitle1,
        )

        Spacer(modifier = Modifier.padding(top = 16.dp))

        Column(
            modifier = Modifier
                .background(
                    color = colorResource(id = R.color.woo_shipping_label_success_surface),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
                )
                .padding(16.dp)
        ) {
            RoundedCornerBoxWithBorder(
                backgroundColor = colorResource(id = R.color.woo_shipping_label_success_surface)
            ) {
                LabelPaperSizeDropdownMenu(
                    availablePaperSizes = availablePaperSizes,
                    selectedLabelPaperSizeOption = selectedLabelPaperSizeOption,
                    onLabelPaperSizeOptionSelected = onLabelPaperSizeOptionSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            WCColoredButton(
                text = stringResource(id = R.string.shipping_label_print_button),
                onClick = { onPrintShippingLabelClicked() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                colors = buttonColors(
                    containerColor = colorResource(id = R.color.woo_shipping_label_success),
                    contentColor = colorResource(id = R.color.woo_white)
                )
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onLearnMoreClicked() }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(16.dp),
                    tint = colorResource(id = R.color.woo_shipping_label_success)
                )
                Text(
                    text = stringResource(id = R.string.shipping_label_purchased_learn_how_to_print),
                    style = MaterialTheme.typography.caption,
                    color = colorResource(id = R.color.woo_shipping_label_success)
                )
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ShippingLabelLink(
                text = stringResource(id = R.string.shipping_label_purchased_track_shipment),
                onClick = { onTrackShipmentClicked() },
                showIcon = true,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            ShippingLabelLink(
                text = stringResource(id = R.string.shipping_label_purchased_schedule_pick_up),
                onClick = { onSchedulePickUpClicked() },
                showIcon = true,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (status.isCustomsFormAvailable) {
                ShippingLabelLink(
                    text = stringResource(id = R.string.shipping_label_print_customs_form),
                    onClick = { onPrintCustomsClicked() },
                    showIcon = true,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            if (status.isRefundAvailable) {
                ShippingLabelLink(
                    text = stringResource(id = R.string.shipping_label_purchased_request_refund),
                    onClick = { onRefundClicked() },
                    showIcon = false,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        ReprintWarning(Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun PurchaseInProgressNotice(modifier: Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.shipping_label_purchased_in_progress_title),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(id = R.string.shipping_label_purchased_in_progress_message),
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorResource(R.color.woo_yellow_10).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
                )
                .padding(16.dp)
        )
        ReprintWarning()
    }
}

@Composable
private fun PurchaseFailureNotice(errorMessage: String?, modifier: Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.shipping_label_purchased_failure_title),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colors.error.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
                )
                .padding(16.dp)
        ) {
            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.subtitle1
                )
            }

            Text(
                text = stringResource(id = R.string.shipping_label_purchased_failure_message),
                style = MaterialTheme.typography.subtitle1
            )
        }
    }
}

@Composable
private fun LabelPaperSizeDropdownMenu(
    availablePaperSizes: List<WooShippingLabelPaperSize>,
    selectedLabelPaperSizeOption: WooShippingLabelPaperSize,
    onLabelPaperSizeOptionSelected: (WooShippingLabelPaperSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .then(modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(selectedLabelPaperSizeOption.stringResource),
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = stringResource(
                    R.string.sorted_by,
                    stringResource(selectedLabelPaperSizeOption.stringResource)
                ),
                tint = colorResource(id = R.color.woo_shipping_label_success)

            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.align(alignment = Alignment.CenterEnd)
        ) {
            availablePaperSizes.forEach { option ->
                DropdownMenuItem(onClick = {
                    onLabelPaperSizeOptionSelected(option)
                    expanded = false
                }) {
                    Text(
                        text = stringResource(option.stringResource),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
            }
        }
    }
}

@Composable
private fun ShippingLabelLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIcon: Boolean = false
) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.woo_shipping_label_success),
            fontWeight = FontWeight.Bold
        )
        if (showIcon) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                tint = colorResource(id = R.color.woo_shipping_label_success),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun ReprintWarning(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(id = R.string.shipping_label_purchased_note),
        style = MaterialTheme.typography.caption,
        color = colorResource(id = R.color.color_on_surface_medium),
        modifier = modifier,
    )
}

@LightDarkThemePreviews
@Composable
private fun PurchaseInProgressPreview() {
    WooThemeWithBackground {
        val selectedLabelPaperSizeOption = remember { mutableStateOf(WooShippingLabelPaperSize.A4) }
        ShippingLabelPurchaseStatusSection(
            labelPurchaseStatus = LabelPurchaseStatus.PurchaseInProgress,
            selectedLabelPaperSizeOption = selectedLabelPaperSizeOption.value,
            onLabelPaperSizeOptionSelected = { selectedLabelPaperSizeOption.value = it },
            onPrintShippingLabelClicked = {},
            onTrackShipmentClicked = {},
            onSchedulePickUpClicked = {},
            onRefundClicked = {},
            onPrintCustomsClicked = {},
            onLearnMoreClicked = {},
            modifier = Modifier.padding(24.dp)
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun PurchasedPreview() {
    WooThemeWithBackground {
        val selectedLabelPaperSizeOption = remember { mutableStateOf(WooShippingLabelPaperSize.A4) }
        ShippingLabelPurchaseStatusSection(
            labelPurchaseStatus = LabelPurchaseStatus.Purchased(
                availablePrintSizes = listOf(WooShippingLabelPaperSize.A4, WooShippingLabelPaperSize.LABEL),
                isCustomsFormAvailable = true,
                isRefundAvailable = true
            ),
            selectedLabelPaperSizeOption = selectedLabelPaperSizeOption.value,
            onLabelPaperSizeOptionSelected = { selectedLabelPaperSizeOption.value = it },
            onPrintShippingLabelClicked = {},
            onTrackShipmentClicked = {},
            onSchedulePickUpClicked = {},
            onRefundClicked = {},
            onPrintCustomsClicked = {},
            onLearnMoreClicked = {},
            modifier = Modifier.padding(24.dp)
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun FailurePreview() {
    WooThemeWithBackground {
        ShippingLabelPurchaseStatusSection(
            labelPurchaseStatus = LabelPurchaseStatus.Failed("An error occurred while purchasing the label."),
            selectedLabelPaperSizeOption = WooShippingLabelPaperSize.A4,
            onLabelPaperSizeOptionSelected = {},
            onPrintShippingLabelClicked = {},
            onTrackShipmentClicked = {},
            onSchedulePickUpClicked = {},
            onRefundClicked = {},
            onPrintCustomsClicked = {},
            onLearnMoreClicked = {},
            modifier = Modifier.padding(24.dp)
        )
    }
}
