package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosSettingsSecurityScreen(onGetStartedClicked: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(WooPosSpacing.Medium.value),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value),
    ) {
        WooPosText(stringResource(R.string.woopos_security_setup), style = WooPosTypography.Heading)
        WooPosText(stringResource(R.string.woopos_security_description), style = WooPosTypography.BodyLarge)
        WooPosButton(text = stringResource(R.string.woopos_security_get_started), onClick = onGetStartedClicked)
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityScreenPreview() {
    WooPosTheme {
        WooPosSettingsSecurityScreen(onGetStartedClicked = {})
    }
}
