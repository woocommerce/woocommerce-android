package com.woocommerce.android.ui.bookings.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingPaymentSection(
    model: BookingPaymentDetailsModel,
    onViewOrder: () -> Unit,
    modifier: Modifier = Modifier,
    onIssueRefund: (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        BookingSectionHeader(R.string.booking_payment_header)
        HorizontalDivider(thickness = 0.5.dp)
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surfaceContainer)
                .padding(vertical = 16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                PaymentRow(label = R.string.booking_payment_label_service, value = model.service)
                PaymentRow(label = R.string.tax, value = model.tax)
                PaymentRow(label = R.string.discount, value = model.discount)
                PaymentRow(label = R.string.total, value = model.total, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (onIssueRefund != null) {
                WCOutlinedButton(
                    onClick = onIssueRefund,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    text = stringResource(id = R.string.booking_overflow_refund),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            WCOutlinedButton(
                onClick = onViewOrder,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                text = stringResource(id = R.string.booking_payment_view_order),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun PaymentRow(
    @StringRes label: Int,
    value: String,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        PaymentText(stringResource(label), fontWeight = fontWeight)
        PaymentText(value, modifier = Modifier.padding(start = 8.dp), fontWeight = fontWeight)
    }
}

@Composable
private fun PaymentText(
    text: String,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = fontWeight),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

data class BookingPaymentDetailsModel(
    val service: String,
    val tax: String,
    val discount: String,
    val total: String
)

@LightDarkThemePreviews
@Composable
private fun BookingPaymentSectionPreview() {
    WooThemeWithBackground {
        BookingPaymentSection(
            model = BookingPaymentDetailsModel(
                service = "$55.00",
                tax = "$4.50",
                discount = "-",
                total = "$59.50"
            ),
            onViewOrder = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}
