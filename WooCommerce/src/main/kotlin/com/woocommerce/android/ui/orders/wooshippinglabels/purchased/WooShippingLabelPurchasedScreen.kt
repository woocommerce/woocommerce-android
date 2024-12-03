package com.woocommerce.android.ui.orders.wooshippinglabels.purchased

import androidx.annotation.StringRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.Colors
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.HazmatCard
import com.woocommerce.android.ui.orders.wooshippinglabels.RoundedCornerBoxWithBorder
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippableItemsUI
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippingProductsCard
import com.woocommerce.android.ui.orders.wooshippinglabels.generateItems

@Suppress("MagicNumber")
private val darkGreen = Color(0xFF005C12)

@Suppress("MagicNumber")
private val lightGreen = Color(0xFFEDFAEF)

val Colors.successColor: Color get() = if (isLight) darkGreen else lightGreen

val Colors.successSurface: Color get() = if (isLight) lightGreen else darkGreen

@Composable
internal fun WooShippingLabelPurchasedScreen(
    selectedLabelPaperSizeOption: WooShippingLabelPaperSize,
    onLabelPaperSizeOptionSelected: (WooShippingLabelPaperSize) -> Unit,
    onPrintShippingLabelClicked: () -> Unit,
    onTrackShipmentClicked: () -> Unit,
    onSchedulePickUpClicked: () -> Unit,
    onRefundClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        Text(
            text = stringResource(id = R.string.shipping_label_purchased_title),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(id = R.string.shipping_label_purchased_message),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.subtitle1,
        )
        Spacer(modifier = Modifier.padding(top = 16.dp))
        PrintShippingLabelCard(
            selectedLabelPaperSizeOption = selectedLabelPaperSizeOption,
            onLabelPaperSizeOptionSelected = onLabelPaperSizeOptionSelected,
            onPrintShippingLabelClicked = onPrintShippingLabelClicked,
            onTrackShipmentClicked = onTrackShipmentClicked,
            onSchedulePickUpClicked = onSchedulePickUpClicked,
            onRefundClicked = onRefundClicked,
            onLearnMoreClicked = onLearnMoreClicked,
        )
        Text(
            text = stringResource(id = R.string.shipping_label_purchased_note),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_on_surface_medium),
            modifier = Modifier.padding(top = 8.dp),
        )

        val isExpanded = remember { mutableStateOf(false) }
        ShippingProductsCard(
            shippableItems = ShippableItemsUI(
                shippableItems = generateItems(6),
                formattedTotalWeight = "8.5kg",
                formattedTotalPrice = "$92.78"
            ),
            isExpanded = isExpanded.value,
            onExpand = { isExpanded.value = it },
            iconColor = MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(top = 24.dp)
        )
        Spacer(modifier = Modifier.padding(top = 16.dp))
        HazmatCard(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colors.background,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
                )
                .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))),
        )
    }
}

@Preview(showSystemUi = true, device = Devices.PIXEL_4)
@Composable
internal fun WooShippingLabelPurchasedScreenPreview() {
    WooThemeWithBackground {
        Surface {
            val selectedLabelPaperSizeOption = remember { mutableStateOf(WooShippingLabelPaperSize.LEGAL) }
            WooShippingLabelPurchasedScreen(
                selectedLabelPaperSizeOption = selectedLabelPaperSizeOption.value,
                onLabelPaperSizeOptionSelected = { selectedLabelPaperSizeOption.value = it },
                onPrintShippingLabelClicked = {},
                modifier = Modifier.padding(16.dp),
                onTrackShipmentClicked = {},
                onSchedulePickUpClicked = {},
                onRefundClicked = {},
                onLearnMoreClicked = {}
            )
        }
    }
}

@Composable
private fun PrintShippingLabelCard(
    selectedLabelPaperSizeOption: WooShippingLabelPaperSize,
    onLabelPaperSizeOptionSelected: (WooShippingLabelPaperSize) -> Unit,
    onPrintShippingLabelClicked: () -> Unit,
    onTrackShipmentClicked: () -> Unit,
    onSchedulePickUpClicked: () -> Unit,
    onRefundClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colors.successSurface,
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
            )
            .padding(16.dp)
    ) {
        RoundedCornerBoxWithBorder(backgroundColor = MaterialTheme.colors.successSurface) {
            LabelPaperSizeDropdownMenu(
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
                backgroundColor = MaterialTheme.colors.successColor,
                contentColor = MaterialTheme.colors.surface
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
                tint = MaterialTheme.colors.successColor
            )
            Text(
                text = stringResource(id = R.string.shipping_label_purchased_learn_how_to_print),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.successColor
            )
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        ShippingLabelLink(
            text = stringResource(id = R.string.shipping_label_purchased_track_shipment),
            onClick = {
                onTrackShipmentClicked()
            },
            showIcon = true,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        ShippingLabelLink(
            text = stringResource(id = R.string.shipping_label_purchased_schedule_pick_up),
            onClick = {
                onSchedulePickUpClicked()
            },
            showIcon = true,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        ShippingLabelLink(
            text = stringResource(id = R.string.shipping_label_purchased_request_refund),
            onClick = {
                onRefundClicked()
            },
            showIcon = false,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun LabelPaperSizeDropdownMenu(
    selectedLabelPaperSizeOption: WooShippingLabelPaperSize,
    onLabelPaperSizeOptionSelected: (WooShippingLabelPaperSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = WooShippingLabelPaperSize.entries

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
                tint = MaterialTheme.colors.successColor

            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.align(alignment = Alignment.CenterEnd)
        ) {
            options.forEach { option ->
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
            color = MaterialTheme.colors.successColor,
            fontWeight = FontWeight.Bold
        )
        if (showIcon) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colors.successColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun ShippingLabelLinkPreview() {
    WooThemeWithBackground {
        ShippingLabelLink(
            text = "Shipping Label",
            onClick = {},
            showIcon = true
        )
    }
}

enum class WooShippingLabelPaperSize(@StringRes val stringResource: Int) {
    LEGAL(R.string.shipping_label_paper_size_legal),
    LETTER(R.string.shipping_label_paper_size_letter),
    LABEL(R.string.shipping_label_paper_size_label)
}
