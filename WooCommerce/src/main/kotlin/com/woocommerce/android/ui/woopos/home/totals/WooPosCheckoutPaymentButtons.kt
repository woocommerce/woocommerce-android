package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(outerPaddingModifier)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        methods.forEachIndexed { index, method ->
            val buttonModifier = Modifier
                .fillMaxWidth()
                .testTag(method.testTag())
            val text = stringResource(method.labelRes())
            val onClick = { onMethodClicked(method) }
            if (index == 0) {
                WooPosButton(modifier = buttonModifier, text = text, onClick = onClick)
            } else {
                WooPosOutlinedButton(modifier = buttonModifier, text = text, onClick = onClick)
            }
        }
    }
}
