package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing

@Composable
internal fun WooPosCustomAmountLayout(
    title: String,
    onBackClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier,
    buttonState: WooPosButtonState = WooPosButtonState.ENABLED,
    buttonText: String = stringResource(R.string.save),
    content: @Composable (Modifier) -> Unit,
) {
    BackHandler { onBackClicked() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        WooPosToolbar(
            titleText = title,
            onBackClicked = onBackClicked,
        )
        content(Modifier.weight(1f).fillMaxWidth())
        WooPosButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value),
            text = buttonText,
            state = buttonState,
            onClick = onSaveClicked,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
    }
}
