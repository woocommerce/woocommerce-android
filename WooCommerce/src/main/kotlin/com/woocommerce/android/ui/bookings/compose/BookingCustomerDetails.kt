package com.woocommerce.android.ui.bookings.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.ActivityUtils

@Composable
fun BookingCustomerDetails(
    model: BookingCustomerDetailsModel,
    modifier: Modifier = Modifier,
) {
    var phoneMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = modifier) {
        BookingSectionHeader(R.string.booking_customer_details_header)
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            HorizontalDivider(thickness = 0.5.dp)
            CustomerDetailsRow(value = model.name)
            CustomerDetailsRow(
                value = model.email,
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_email),
                        contentDescription = stringResource(id = R.string.booking_customer_label_email),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable {
                    ActivityUtils.sendEmail(context, model.email)
                }
            )
            CustomerDetailsRow(
                value = model.phone,
                trailingIcon = {
                    Box {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu_more_vert),
                            contentDescription = stringResource(id = R.string.booking_customer_label_phone),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        ContactDropdownMenu(
                            expanded = phoneMenuExpanded,
                            phone = model.phone,
                            onDismissRequest = { phoneMenuExpanded = false }
                        )
                    }
                },
                modifier = Modifier.clickable { phoneMenuExpanded = true }
            )
            Column(
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                BookingDetailsLabel(label = R.string.booking_billing_address_label)
                model.billingAddressLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp)
        }
    }
}

@Composable
private fun CustomerDetailsRow(
    value: String,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (trailingIcon != null) {
                val iconHeight = with(LocalDensity.current) {
                    MaterialTheme.typography.labelLarge.lineHeight.toPx().toDp()
                }
                Row(
                    modifier = Modifier
                        .height(iconHeight)
                        .padding(start = 8.dp)
                ) {
                    trailingIcon()
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            thickness = 0.5.dp
        )
    }
}

data class BookingCustomerDetailsModel(
    val name: String,
    val email: String,
    val phone: String,
    val billingAddressLines: List<String>,
)

@LightDarkThemePreviews
@Composable
private fun BookingCustomerDetailsPreview() {
    WooThemeWithBackground {
        BookingCustomerDetails(
            model = BookingCustomerDetailsModel(
                name = "Margarita Nikolaevna",
                email = "margarita@example.com",
                phone = "+1 555-123-4567",
                billingAddressLines = listOf(
                    "238 Willow Creek Drive",
                    "Montgomery",
                    "AL 36109"
                )
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
