package com.woocommerce.android.ui.woopos.settings.details.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosSettingsPaymentsScreen(
    modifier: Modifier = Modifier,
    viewModel: WooPosSettingsPaymentsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    WooPosSettingsPaymentsScreen(
        state = state,
        onUIEvent = viewModel::onUIEvent,
        modifier = modifier
    )
}

@Composable
private fun WooPosSettingsPaymentsScreen(
    state: WooPosSettingsPaymentsState,
    onUIEvent: (WooPosSettingsPaymentsUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WooPosSpacing.Medium.value),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
    ) {
        when (state) {
            WooPosSettingsPaymentsState.Loading -> Unit
            is WooPosSettingsPaymentsState.Content -> PaymentSuccessSoundSection(
                isEnabled = state.isPaymentSuccessSoundEnabled,
                onToggle = { onUIEvent(WooPosSettingsPaymentsUIEvent.PaymentSuccessSoundToggled(it)) }
            )
        }
    }
}

@Composable
private fun PaymentSuccessSoundSection(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    WooPosCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isEnabled,
                    role = Role.Switch,
                    onValueChange = onToggle,
                )
                .padding(WooPosSpacing.Medium.value),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)
            ) {
                WooPosText(
                    text = stringResource(R.string.woopos_settings_payments_success_sound),
                    style = WooPosTypography.BodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )

                WooPosText(
                    text = stringResource(R.string.woopos_settings_payments_success_sound_subtitle),
                    style = WooPosTypography.BodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFFFFFF),
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                ),
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsPaymentsScreenEnabledPreview() {
    WooPosTheme {
        WooPosSettingsPaymentsScreen(
            state = WooPosSettingsPaymentsState.Content(isPaymentSuccessSoundEnabled = true),
            onUIEvent = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsPaymentsScreenDisabledPreview() {
    WooPosTheme {
        WooPosSettingsPaymentsScreen(
            state = WooPosSettingsPaymentsState.Content(isPaymentSuccessSoundEnabled = false),
            onUIEvent = {}
        )
    }
}
