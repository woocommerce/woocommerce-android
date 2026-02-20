package com.woocommerce.android.ui.login.wpcom.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.clickableAnnotatedStringRes
import com.woocommerce.android.util.ChromeCustomTabUtils

@Composable
fun WPComConsent(
    forJetpackSetup: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val consent = clickableAnnotatedStringRes(
        stringResId = R.string.login_wpcom_connection_consent,
        onUrlClick = { url ->
            when (url) {
                "terms" -> ChromeCustomTabUtils.launchUrl(context, AppUrls.WORPRESS_COM_TERMS)
                "sync" -> ChromeCustomTabUtils.launchUrl(context, AppUrls.JETPACK_SYNC_POLICY)
            }
        },
        stringResource(
            if (forJetpackSetup) {
                R.string.login_wpcom_connection_consent_jetpack
            } else {
                R.string.login_wpcom_connection_consent_generic
            }
        )
    )

    Text(
        text = consent,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}
