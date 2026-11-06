package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
internal fun WooPosCustomAmountLayout(
    title: String,
    onBackClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier,
    buttonState: WooPosButtonState = WooPosButtonState.ENABLED,
    isEditor: Boolean = false,
    content: @Composable (Modifier) -> Unit,
) {
    BackHandler { onBackClicked() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.XLarge.value),
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WooPosBackButton(onClick = onBackClicked)
            WooPosText(
                modifier = Modifier.weight(1f),
                text = title,
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
            )
        }
        content(Modifier.weight(1f).fillMaxWidth())
        WooPosButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isEditor) WooPosSpacing.Medium.value else WooPosSpacing.XLarge.value),
            text = stringResource(R.string.save),
            state = buttonState,
            onClick = onSaveClicked,
        )
    }
}
