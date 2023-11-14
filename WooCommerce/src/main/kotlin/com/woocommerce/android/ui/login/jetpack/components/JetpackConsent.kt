package com.woocommerce.android.ui.login.jetpack.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.URL_ANNOTATION_TAG
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.util.ChromeCustomTabUtils

@Composable
fun JetpackConsent(modifier: Modifier = Modifier) {
    val consent = annotatedStringRes(stringResId = R.string.login_jetpack_connection_consent)
    val context = LocalContext.current
    ClickableText(
        text = consent,
        style = MaterialTheme.typography.caption.copy(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface
        ),
        modifier = modifier
    ) {
        consent.getStringAnnotations(tag = URL_ANNOTATION_TAG, start = it, end = it)
            .firstOrNull()
            ?.let { annotation ->
                when (annotation.item) {
                    "terms" -> ChromeCustomTabUtils.launchUrl(context, AppUrls.WORPRESS_COM_TERMS)
                    "sync" -> ChromeCustomTabUtils.launchUrl(context, AppUrls.JETPACK_SYNC_POLICY)
                }
            }
    }
}
