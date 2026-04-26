package com.woocommerce.android.ui.login.qrlogin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

/**
 * Fullscreen "Signing you in…" view shown while the QR exchange + site fetch + eligibility check
 * runs. Replaces a centered ProgressDialog so the screen has a consistent surface instead of a
 * small dialog floating on a scrim.
 */
@Composable
fun QrLoginAuthenticatingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = dimensionResource(id = R.dimen.major_150)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_150)))
        Text(
            text = stringResource(id = R.string.login_qr_scanner_authenticating),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun QrLoginAuthenticatingScreenPreview() {
    WooThemeWithBackground {
        QrLoginAuthenticatingScreen()
    }
}
