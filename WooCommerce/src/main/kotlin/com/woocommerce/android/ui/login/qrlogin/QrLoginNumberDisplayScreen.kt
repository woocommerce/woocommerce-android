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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import kotlinx.coroutines.delay

/**
 * Number-matching approval step. Shows the 3-digit number the merchant must tap on the
 * matching wc-admin screen for the sign-in to complete. The number is rendered prominently
 * (large, monospaced) with the host as context and a 90-second countdown.
 *
 * Cancel returns to the scanner. The server keeps the session in `scanned` until its
 * 90-second window elapses, so wc-admin's polling auto-transitions to the "denied" terminal
 * screen — no explicit cancel call is needed.
 */
@Composable
fun QrLoginNumberDisplayScreen(
    subtitle: String,
    realNumber: String,
    expiresAtEpochMs: Long,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Hero(subtitle = subtitle, realNumber = realNumber, expiresAtEpochMs = expiresAtEpochMs)
        }
        CancelButton(onCancel = onCancel)
    }
}

@Composable
private fun Hero(subtitle: String, realNumber: String, expiresAtEpochMs: Long) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.login_qr_match_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_75)))
        Text(
            text = stringResource(id = R.string.login_qr_match_host_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_75)))
        // Prominent host display so the merchant can spot a phishing-style mismatch
        // (e.g. a homograph attack: `my-stōre.example` vs the punycode form
        // `xn--my-stre-1za.example` that OkHttp's HttpUrl normalisation surfaces).
        // The ViewModel's `toDisplayHost` already converts IDN names to ASCII, so any
        // visual sleight-of-hand in the QR's URL surfaces here for the user to read.
        SubtitleBadge(subtitle = subtitle)
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_150)))
        Text(
            text = stringResource(id = R.string.login_qr_match_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_150)))
        NumberTile(realNumber = realNumber)
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_125)))
        Countdown(expiresAtEpochMs = expiresAtEpochMs)
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Text(
            text = stringResource(id = R.string.login_qr_match_security_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SubtitleBadge(subtitle: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.major_100)))
            .background(colorResource(id = R.color.color_primary).copy(alpha = TILE_BG_ALPHA))
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_150),
                vertical = dimensionResource(id = R.dimen.major_100)
            )
    ) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = colorResource(id = R.color.color_primary),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NumberTile(realNumber: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.major_100)))
            .background(colorResource(id = R.color.color_primary).copy(alpha = TILE_BG_ALPHA))
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_300),
                vertical = dimensionResource(id = R.dimen.major_200)
            )
    ) {
        Text(
            text = realNumber,
            style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Monospace),
            fontSize = NUMBER_FONT_SIZE_SP.sp,
            color = colorResource(id = R.color.color_primary),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Countdown(expiresAtEpochMs: Long) {
    var secondsRemaining by remember(expiresAtEpochMs) {
        mutableLongStateOf(((expiresAtEpochMs - System.currentTimeMillis()) / MILLIS_PER_SECOND).coerceAtLeast(0L))
    }
    LaunchedEffect(expiresAtEpochMs) {
        while (secondsRemaining > 0L) {
            delay(MILLIS_PER_SECOND)
            secondsRemaining =
                ((expiresAtEpochMs - System.currentTimeMillis()) / MILLIS_PER_SECOND).coerceAtLeast(0L)
        }
    }
    Text(
        text = stringResource(id = R.string.login_qr_match_countdown, secondsRemaining),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CancelButton(onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(id = R.dimen.major_100)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WCTextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.login_qr_match_cancel))
        }
    }
}

private const val TILE_BG_ALPHA = 0.12f
private const val NUMBER_FONT_SIZE_SP = 64
private const val MILLIS_PER_SECOND = 1_000L

@LightDarkThemePreviews
@Composable
private fun QrLoginNumberDisplayScreenPreview() {
    WooThemeWithBackground {
        QrLoginNumberDisplayScreen(
            subtitle = "store.example",
            realNumber = "042",
            expiresAtEpochMs = System.currentTimeMillis() + 90_000L,
            onCancel = {}
        )
    }
}
