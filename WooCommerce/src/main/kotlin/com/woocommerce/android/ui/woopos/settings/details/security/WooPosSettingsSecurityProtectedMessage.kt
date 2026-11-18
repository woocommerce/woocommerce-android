package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole

@Composable
fun WooPosSettingsSecurityProtectedMessage(role: WooPosLocalRole, modifier: Modifier = Modifier) {
    val (title, message) = when (role) {
        WooPosLocalRole.ADMIN -> R.string.woopos_security_success to R.string.woopos_security_success_message
        WooPosLocalRole.CASHIER ->
            R.string.woopos_security_cashier_success to R.string.woopos_security_cashier_success_message
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        Image(
            painter = painterResource(R.drawable.woopos_check_circle),
            contentDescription = null,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value),
        )
        WooPosText(
            text = stringResource(title),
            style = WooPosTypography.BodyXLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        WooPosText(
            text = stringResource(message),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = MESSAGE_MAX_WIDTH),
        )
    }
}

private val MESSAGE_MAX_WIDTH = 470.dp
