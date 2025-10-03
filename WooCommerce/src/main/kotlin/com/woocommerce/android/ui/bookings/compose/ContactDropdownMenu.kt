package com.woocommerce.android.ui.bookings.compose

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.util.ActivityUtils
import org.wordpress.android.util.ToastUtils

private const val WHATSAPP_PACKAGE_NAME = "com.whatsapp"
private const val TELEGRAM_PACKAGE_NAME = "org.telegram.messenger"

@Composable
fun ContactDropdownMenu(
    expanded: Boolean,
    phone: String,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.orderdetail_call_customer)) },
            onClick = {
                ActivityUtils.dialPhoneNumber(context, phone) { error ->
                    ToastUtils.showToast(context, R.string.error_no_phone_app)
                }
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.orderdetail_message_customer)) },
            onClick = {
                ActivityUtils.sendSms(context, phone) {
                    ToastUtils.showToast(context, R.string.error_no_sms_app)
                }
                onDismissRequest()
            }
        )
        if (ActivityUtils.isAppInstalled(context, WHATSAPP_PACKAGE_NAME)) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.orderdetail_message_customer_using_whatsapp))
                },
                onClick = {
                    ActivityUtils.openWhatsApp(context, phone)
                    onDismissRequest()
                }
            )
        }
        if (ActivityUtils.isAppInstalled(context, TELEGRAM_PACKAGE_NAME)) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.orderdetail_message_customer_using_telegram))
                },
                onClick = {
                    ActivityUtils.openTelegram(context, phone)
                    onDismissRequest()
                }
            )
        }
    }
}
