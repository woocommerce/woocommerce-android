package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosSettingsSecurityScreen(onGetStartedClicked: () -> Unit) {
    val viewModel: WooPosSettingsSecurityViewModel = hiltViewModel()
    val isProtectionEnabled by viewModel.isProtectionEnabled.collectAsState()
    WooPosSettingsSecurityScreen(isProtectionEnabled = isProtectionEnabled, onGetStartedClicked = onGetStartedClicked)
}

@Composable
fun WooPosSettingsSecurityScreen(isProtectionEnabled: Boolean, onGetStartedClicked: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = WooPosSpacing.XXLarge.value, vertical = WooPosSpacing.Medium.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isProtectionEnabled) {
                // TODO WOOMOB-4602: Replace with PIN management controls.
                WooPosSettingsSecurityProtectedMessage()
            } else {
                AccessControlSetup(onGetStartedClicked)
            }
        }
    }
}

@Composable
private fun AccessControlSetup(onGetStartedClicked: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.XLarge.value),
    ) {
        Image(painter = painterResource(R.drawable.woopos_access_control), contentDescription = null)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_security_setup),
                style = WooPosTypography.BodyXLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            WooPosText(
                text = stringResource(R.string.woopos_security_description),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        WooPosButton(
            text = stringResource(R.string.woopos_security_get_started),
            onClick = onGetStartedClicked,
            modifier = Modifier
                .widthIn(max = BUTTON_MAX_WIDTH)
                .fillMaxWidth(),
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityScreenPreview() {
    WooPosTheme {
        WooPosSettingsSecurityScreen(isProtectionEnabled = false, onGetStartedClicked = {})
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityScreenProtectedPreview() {
    WooPosTheme {
        WooPosSettingsSecurityScreen(isProtectionEnabled = true, onGetStartedClicked = {})
    }
}

private val BUTTON_MAX_WIDTH = 612.dp
