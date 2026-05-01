package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint

@Composable
internal fun WooPosCheckoutPaymentButtons(
    methods: List<WooPosPaymentMethod>,
    onMethodClicked: (WooPosPaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPhone = currentWooPosBreakpoint() == WooPosBreakpoint.Phone
    val outerPaddingModifier = if (isPhone) {
        Modifier.padding(WooPosSpacing.Large.value)
    } else {
        Modifier.padding(horizontal = WooPosSpacing.XLarge.value)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(outerPaddingModifier)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        methods.forEach { method ->
            WooPosOutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag(method.testTag()),
                text = stringResource(method.labelRes()),
                onClick = { onMethodClicked(method) },
            )
        }
    }
}
