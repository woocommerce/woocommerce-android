package com.woocommerce.android.ui.orders.wooshippinglabels.upsdap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippingLabelSampleData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress

@Composable
fun UPSDAPTermsOfServiceBottomSheet(
    viewModel: UPSDAPTermsOfServiceViewModel
) {
    viewModel.viewState.observeAsState().value?.let {
        UPSDAPTermsOfServiceBottomSheet(it)
    }
}

@Composable
fun UPSDAPTermsOfServiceBottomSheet(
    viewState: UPSDAPTermsOfServiceViewModel.ViewState
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = dimensionResource(id = R.dimen.minor_100),
            topEnd = dimensionResource(id = R.dimen.minor_100)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.wpp_shipping_ups_tos_title),
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            OriginAddressSection(address = viewState.originShippingAddress)

            Spacer(modifier = Modifier.height(16.dp))

            Divider()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.wpp_shipping_ups_tos_description),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                color = MaterialTheme.colors.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Conditions(
                conditionsState = viewState.conditionsState,
                onUrlClicked = viewState.onUrlClicked
            )

            Spacer(modifier = Modifier.height(32.dp))

            WCColoredButton(
                onClick = viewState.onContinueClicked,
                enabled = viewState.areAllConditionsAccepted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.wpp_shipping_ups_tos_accept))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OriginAddressSection(address: OriginShippingAddress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.wpp_shipping_ups_tos_shipping_from),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = address.format(singleLine = false),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
        )
    }
}

@Composable
private fun Conditions(
    conditionsState: UPSDAPTermsOfServiceViewModel.ConditionsState,
    onUrlClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
    ) {
        CheckboxWithTitle(
            checked = conditionsState.isTermsOfServiceChecked,
            title = annotatedStringRes(
                stringResId = R.string.wpp_shipping_ups_tos_condition_terms,
                onUrlClick = onUrlClicked
            ),
            onCheckedChange = conditionsState.onTermsOfServiceCheckedChanged
        )

        CheckboxWithTitle(
            checked = conditionsState.isProhibitedItemsChecked,
            title = annotatedStringRes(
                stringResId = R.string.wpp_shipping_ups_tos_condition_prohibited_items,
                onUrlClick = onUrlClicked
            ),
            onCheckedChange = conditionsState.onProhibitedItemsCheckedChanged
        )

        CheckboxWithTitle(
            checked = conditionsState.isTechnologyAgreementChecked,
            title = annotatedStringRes(
                stringResId = R.string.wpp_shipping_ups_tos_condition_technology_agreement,
                onUrlClick = onUrlClicked
            ),
            onCheckedChange = conditionsState.onTechnologyAgreementCheckedChanged
        )
    }
}

@Composable
private fun CheckboxWithTitle(
    checked: Boolean,
    title: AnnotatedString,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = title,
            style = MaterialTheme.typography.body2
        )
    }
}

@LightDarkThemePreviews
@Composable
fun UPSDAPTermsOfServiceBottomSheetPreview() {
    WooThemeWithBackground {
        UPSDAPTermsOfServiceBottomSheet(
            UPSDAPTermsOfServiceViewModel.ViewState(
                originShippingAddress = ShippingLabelSampleData.getShipFrom(),
                conditionsState = UPSDAPTermsOfServiceViewModel.ConditionsState(
                    isTermsOfServiceChecked = true,
                    isProhibitedItemsChecked = true,
                    isTechnologyAgreementChecked = true,
                    onTermsOfServiceCheckedChanged = {},
                    onProhibitedItemsCheckedChanged = {},
                    onTechnologyAgreementCheckedChanged = {}
                ),
                onUrlClicked = {},
                onContinueClicked = {}
            )
        )
    }
}
