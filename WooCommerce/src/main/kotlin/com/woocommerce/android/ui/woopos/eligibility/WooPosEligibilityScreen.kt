package com.woocommerce.android.ui.woopos.eligibility

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability

@Composable
fun WooPosEligibilityScreen(
    reason: WooPosLaunchability.NonLaunchabilityReason,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    BackHandler {
        onNavigationEvent(WooPosNavigationEvent.ExitPosClicked)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Large.value.toAdaptivePadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_woo_pos_error_x),
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))

        WooPosText(
            text = "Unable to load",
            style = WooPosTypography.Heading,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))

        WooPosText(
            text = "The POS system is not available for your store's currency. " +
                "It currently supports only US dollars and British pounds. " +
                "Please check your store currency settings or contact support for assistance.",
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(547.dp)
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        WooPosButton(
            text = stringResource(id = R.string.woopos_eligibility_retry_check_label),
            modifier = Modifier.size(width = 366.dp, height = 80.dp)
        ) {}

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

        WooPosOutlinedButton(
            text = stringResource(id = R.string.woopos_eligibility_exit_pos_label),
            modifier = Modifier.size(width = 366.dp, height = 80.dp)
        ) {
            onNavigationEvent(WooPosNavigationEvent.ExitPosClicked)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WooPosEligibilityScreenPreview() {
    WooPosEligibilityScreen(
        reason = WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency,
        onNavigationEvent = {}
    )
}
