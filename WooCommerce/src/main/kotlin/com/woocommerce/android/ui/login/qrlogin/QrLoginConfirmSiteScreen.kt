package com.woocommerce.android.ui.login.qrlogin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun QrLoginConfirmSiteScreen(
    host: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = dimensionResource(id = R.dimen.major_150)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero scrolls so the Connect / Cancel buttons stay visible in landscape on phones
        // where the static layout would otherwise push them off the bottom edge.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Hero(host = host)
        }
        Buttons(onConfirm = onConfirm, onCancel = onCancel)
    }
}

@Composable
private fun Hero(host: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconBadge()
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_200)))
        Text(
            text = stringResource(id = R.string.login_qr_confirm_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_75)))
        Text(
            text = stringResource(id = R.string.login_qr_confirm_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_200)))
        HostBadge(host = host)
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_125)))
        Text(
            text = stringResource(id = R.string.login_qr_confirm_security_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun IconBadge() {
    Box(
        modifier = Modifier
            .size(dimensionResource(id = R.dimen.image_major_72))
            .clip(CircleShape)
            .background(colorResource(id = R.color.color_primary).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_baseline_qr_code_scanner),
            contentDescription = null,
            tint = colorResource(id = R.color.color_primary),
            modifier = Modifier.size(dimensionResource(id = R.dimen.image_minor_100))
        )
    }
}

@Composable
private fun HostBadge(host: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.major_100)))
            .background(colorResource(id = R.color.color_primary).copy(alpha = 0.12f))
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_150),
                vertical = dimensionResource(id = R.dimen.major_100)
            )
    ) {
        Text(
            text = host,
            style = MaterialTheme.typography.titleMedium,
            color = colorResource(id = R.color.color_primary),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Buttons(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(id = R.dimen.major_100)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WCColoredButton(
            onClick = onConfirm,
            text = stringResource(id = R.string.login_qr_confirm_connect),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_75)))
        WCTextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.login_qr_confirm_cancel))
        }
    }
}

@LightDarkThemePreviews
@Composable
private fun QrLoginConfirmSiteScreenPreview() {
    WooThemeWithBackground {
        QrLoginConfirmSiteScreen(
            host = "store.example",
            onConfirm = {},
            onCancel = {}
        )
    }
}
