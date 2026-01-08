package com.woocommerce.android.ui.woopos.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosInputField
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosRefundReasonScreen(
    refundReason: String,
    onReasonChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        WooPosToolbar(
            titleText = stringResource(R.string.woopos_orders_refund_reason),
            onBackClicked = onCancel,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WooPosInputField(
                    value = refundReason,
                    onValueChange = onReasonChanged,
                    label = stringResource(R.string.woopos_orders_refund_reason_placeholder),
                    contentAlignment = Alignment.Center,
                    textStyle = WooPosTypography.Heading,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .padding(horizontal = WooPosSpacing.Medium.value)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            WooPosButton(
                text = stringResource(
                    if (refundReason.isBlank()) {
                        R.string.woopos_orders_refund_reason_add
                    } else {
                        R.string.woopos_orders_refund_reason_save
                    }
                ),
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WooPosSpacing.Medium.value)
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
        }
    }
}
