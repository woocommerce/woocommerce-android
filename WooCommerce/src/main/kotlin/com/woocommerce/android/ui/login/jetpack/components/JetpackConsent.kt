package com.woocommerce.android.ui.login.jetpack.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.util.ChromeCustomTabUtils

@Composable
fun JetpackConsent(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val consent = annotatedStringRes(
        stringResId = R.string.login_jetpack_connection_consent,
        onUrlClick = { url ->
            when (url) {
                "terms" -> ChromeCustomTabUtils.launchUrl(context, AppUrls.WORPRESS_COM_TERMS)
                "sync" -> ChromeCustomTabUtils.launchUrl(context, AppUrls.JETPACK_SYNC_POLICY)
            }
        }
    )

    Text(
        text = consent,
        style = MaterialTheme.typography.caption,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.onSurface,
        modifier = modifier
    )
}
