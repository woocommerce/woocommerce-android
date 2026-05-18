package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosFullScreenInputLayout
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosInputField
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
@Suppress("UnusedParameter")
fun WooPosRefundReasonScreen(
    orderId: Long,
    initialReason: String,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    var refundReason by remember { mutableStateOf(initialReason) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    WooPosFullScreenInputLayout(
        modifier = Modifier.navigationBarsPadding(),
        titleText = stringResource(R.string.woopos_orders_refund_reason),
        onBackClicked = {
            onNavigationEvent(WooPosNavigationEvent.GoBack)
        },
        centerContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WooPosInputField(
                    value = refundReason,
                    onValueChange = { refundReason = it },
                    label = stringResource(R.string.woopos_orders_refund_reason_placeholder),
                    contentAlignment = Alignment.Center,
                    textStyle = WooPosTypography.BodyLarge,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    labelMaxLines = 2,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .padding(horizontal = WooPosSpacing.Medium.value)
                )
            }
        },
        buttonText = stringResource(
            if (refundReason.isBlank()) {
                R.string.woopos_orders_refund_reason_add
            } else {
                R.string.woopos_orders_refund_reason_save
            }
        ),
        onButtonClicked = {
            onNavigationEvent(
                WooPosNavigationEvent.GoBackWithResult(
                    key = REFUND_REASON_RESULT_KEY,
                    value = refundReason
                )
            )
        }
    )
}
