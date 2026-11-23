package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItemInfo

@Composable
fun WooPosSettingsSecurityScreen(
    pinSetupRole: WooPosLocalRole?,
    onSetUpPIN: (WooPosLocalRole) -> Unit,
) {
    val viewModel: WooPosSettingsSecurityViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    LaunchedEffect(pinSetupRole) { if (pinSetupRole == null) viewModel.refresh() }
    val visibleState = if (pinSetupRole == WooPosLocalRole.ADMIN && state is WooPosSettingsSecurityState.Protected) {
        WooPosSettingsSecurityState.Off
    } else {
        state
    }
    WooPosSettingsSecurityScreen(
        state = visibleState,
        onSetUpPIN = onSetUpPIN,
        onProtectionTurnedOff = viewModel::onProtectionTurnedOff,
    )
}

/** The setup empty state is centered without the pane heading; PIN management shows it. */
@Composable
fun wooPosSettingsSecurityShowsHeading(): Boolean {
    val viewModel: WooPosSettingsSecurityViewModel = hiltViewModel()
    return viewModel.state.collectAsState().value is WooPosSettingsSecurityState.Protected
}

@Composable
fun WooPosSettingsSecurityScreen(
    state: WooPosSettingsSecurityState,
    onSetUpPIN: (WooPosLocalRole) -> Unit,
    onProtectionTurnedOff: () -> Unit,
) {
    when (state) {
        WooPosSettingsSecurityState.Loading -> Unit
        WooPosSettingsSecurityState.Off -> CenteredContent {
            AccessControlSetup(onGetStartedClicked = { onSetUpPIN(WooPosLocalRole.ADMIN) })
        }
        WooPosSettingsSecurityState.Restricted -> CenteredContent {
            WooPosSettingsSecurityProtectedMessage(WooPosLocalRole.ADMIN)
        }
        WooPosSettingsSecurityState.Unavailable -> CenteredContent {
            WooPosText(
                text = stringResource(R.string.woopos_lock_unavailable),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        is WooPosSettingsSecurityState.Protected -> PINProtection(state, onSetUpPIN, onProtectionTurnedOff)
    }
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
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
            content()
        }
    }
}

@Composable
private fun PINProtection(
    state: WooPosSettingsSecurityState.Protected,
    onSetUpPIN: (WooPosLocalRole) -> Unit,
    onProtectionTurnedOff: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WooPosSpacing.Medium.value),
    ) {
        WooPosCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(WooPosSpacing.Medium.value)) {
                WooPosText(
                    text = stringResource(R.string.woopos_security_pin_protection),
                    style = WooPosTypography.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                PINProtectionToggle(onProtectionTurnedOff)
                if (state.removalFailed) {
                    WooPosText(
                        text = stringResource(R.string.woopos_security_save_failed),
                        style = WooPosTypography.BodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))
                PINRow(
                    title = stringResource(R.string.woopos_security_admin_pin),
                    subtitle = stringResource(R.string.woopos_security_admin_pin_detail),
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))
                PINRow(
                    title = stringResource(R.string.woopos_security_cashier_pin),
                    subtitle = stringResource(R.string.woopos_security_cashier_pin_detail),
                ) {
                    if (!state.hasCashierPIN) {
                        WooPosButtonSmall(
                            text = stringResource(R.string.woopos_security_set_up),
                            onClick = { onSetUpPIN(WooPosLocalRole.CASHIER) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PINProtectionToggle(onProtectionTurnedOff: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = true, role = Role.Switch, onValueChange = { onProtectionTurnedOff() })
            .padding(vertical = WooPosSpacing.Small.value),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_security_enable_pin_protection),
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = true,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        )
    }
}

@Composable
private fun PINRow(title: String, subtitle: String, action: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        WooPosSettingsDetailsMenuItemInfo(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
        action()
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
    SecurityScreenPreview(WooPosSettingsSecurityState.Off)
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityScreenProtectedPreview() {
    SecurityScreenPreview(WooPosSettingsSecurityState.Protected(hasCashierPIN = false, removalFailed = false))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityScreenCashierSetPreview() {
    SecurityScreenPreview(WooPosSettingsSecurityState.Protected(hasCashierPIN = true, removalFailed = true))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityScreenRestrictedPreview() {
    SecurityScreenPreview(WooPosSettingsSecurityState.Restricted)
}

@Composable
private fun SecurityScreenPreview(state: WooPosSettingsSecurityState) {
    WooPosTheme {
        WooPosSettingsSecurityScreen(state = state, onSetUpPIN = {}, onProtectionTurnedOff = {})
    }
}

private val BUTTON_MAX_WIDTH = 612.dp
