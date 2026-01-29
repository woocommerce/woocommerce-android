package com.woocommerce.android.ui.woopos.home.totals.payment.success

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSuccessCheckmark
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSuccessCheckmarkAnimationStage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

@Composable
fun WooPosPaymentSuccessScreen(
    state: WooPosTotalsViewState.PaymentSuccess,
    onReceiptClicked: () -> Unit,
    onNewTransactionClicked: () -> Unit,
) {
    val animationStage = remember { mutableStateOf(WooPosSuccessCheckmarkAnimationStage.INITIAL) }
    val hugeSpacing = WooPosSpacing.Huge.value
    val mediumSpacing = WooPosSpacing.Medium.value
    val marginBetweenButtonAndText by animateDpAsState(
        targetValue = if (animationStage.value >= WooPosSuccessCheckmarkAnimationStage.BUTTONS) {
            hugeSpacing
        } else {
            mediumSpacing
        },
        label = "Check mark size"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceBright),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WooPosSuccessCheckmark(
            contentDescription = stringResource(R.string.woopos_payment_successful_label),
            onAnimationStageChanged = { stage -> animationStage.value = stage },
            modifier = Modifier.testTag(WooPosTestTags.SUCCESS_CHECKMARK_ICON)
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XXXLarge.value))

        WooPosText(
            text = stringResource(R.string.woopos_payment_successful_label),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosText(
            text = state.orderTotalText,
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(marginBetweenButtonAndText))

        WooPosButton(
            modifier = Modifier
                .height(80.dp)
                .width(604.dp)
                .testTag(WooPosTestTags.NEW_ORDER_BUTTON),
            onClick = onNewTransactionClicked,
            text = stringResource(R.string.woopos_new_order_button)
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosOutlinedButton(
            modifier = Modifier
                .height(80.dp)
                .width(604.dp),
            onClick = onReceiptClicked,
            text = stringResource(R.string.woopos_receipt_button)
        )
    }
}

@WooPosPreview
@Composable
fun WooPosPaymentSuccessScreenPreview() {
    WooPosTheme {
        WooPosPaymentSuccessScreen(
            state = WooPosTotalsViewState.PaymentSuccess(
                orderTotalText = "A payment of 13.18 was successfully made",
            ),
            onReceiptClicked = {},
            onNewTransactionClicked = {}
        )
    }
}
