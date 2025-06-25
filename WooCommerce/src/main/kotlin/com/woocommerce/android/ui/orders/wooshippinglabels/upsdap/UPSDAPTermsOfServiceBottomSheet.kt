package com.woocommerce.android.ui.orders.wooshippinglabels.upsdap

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
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
    originAddress: OriginShippingAddress,
    onAcceptClicked: () -> Unit,
    onUrlClicked: (String) -> Unit
) {
    val scrollState = rememberScrollState()

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
                .verticalScroll(scrollState),
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

            Spacer(modifier = Modifier.height(16.dp))

            OriginAddressSection(address = originAddress)

            Spacer(modifier = Modifier.height(16.dp))

            // Divider
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
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

            Conditions(onUrlClicked)

            Spacer(modifier = Modifier.height(32.dp))

            // Accept Button
            WCColoredButton(
                onClick = onAcceptClicked,
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
private fun Conditions(onUrlClicked: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = annotatedStringRes(
                stringResId = R.string.wpp_shipping_ups_tos_condition_1,
                onUrlClick = onUrlClicked
            ),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = annotatedStringRes(
                stringResId = R.string.wpp_shipping_ups_tos_condition_2,
                onUrlClick = onUrlClicked
            ),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = annotatedStringRes(
                stringResId = R.string.wpp_shipping_ups_tos_condition_3,
                onUrlClick = onUrlClicked
            ),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
        )
    }
}

@LightDarkThemePreviews
@Composable
fun UPSDAPTermsOfServiceBottomSheetPreview() {
    WooThemeWithBackground {
        UPSDAPTermsOfServiceBottomSheet(
            originAddress = ShippingLabelSampleData.getShipFrom(),
            onAcceptClicked = {},
            onUrlClicked = {}
        )
    }
}
