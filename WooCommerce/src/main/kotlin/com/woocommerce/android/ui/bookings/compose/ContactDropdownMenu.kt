package com.woocommerce.android.ui.bookings.compose

import android.content.Context
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.woocommerce.android.R
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.PhoneContactOption
import com.woocommerce.android.util.getAvailablePhoneContactOptions
import com.woocommerce.android.util.stringRes
import org.wordpress.android.util.ToastUtils

@Composable
fun ContactDropdownMenu(
    expanded: Boolean,
    phone: String,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    var contactOptions by remember { mutableStateOf(emptyList<PhoneContactOption>()) }

    // Update the available contact options when the composable enters the RESUMED state
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        contactOptions = context.getAvailablePhoneContactOptions()
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        contactOptions.forEach { contactOption ->
            DropdownMenuItem(
                text = { Text(stringResource(contactOption.stringRes)) },
                onClick = {
                    contactOption.action(context, phone)
                    onDismissRequest()
                }
            )
        }
    }
}

private val PhoneContactOption.action: (Context, String) -> Unit
    get() = when (this) {
        PhoneContactOption.CALL -> {
            // This is the lambda being returned. It must define its parameters.
            { context, phone ->
                ActivityUtils.dialPhoneNumber(context, phone) {
                    ToastUtils.showToast(context, R.string.error_no_phone_app)
                }
            }
        }

        PhoneContactOption.SMS -> {
            { context, phone ->
                ActivityUtils.sendSms(context, phone) {
                    ToastUtils.showToast(context, R.string.error_no_sms_app)
                }
            }
        }

        PhoneContactOption.WHATSAPP -> {
            { context, phone ->
                ActivityUtils.openWhatsApp(context, phone)
            }
        }

        PhoneContactOption.TELEGRAM -> {
            { context, phone ->
                ActivityUtils.openTelegram(context, phone)
            }
        }
    }
