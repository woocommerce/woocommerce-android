package com.woocommerce.android.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun WCAnalyticsNotAvailableErrorView(
    title: String,
    onContactSupportClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_woo_generic_error),
            contentDescription = null
        )

        Text(
            text = title,
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(id = R.string.dashboard_wcanalytics_inactive_description),
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center
        )

        WCOutlinedButton(
            text = stringResource(id = R.string.dashboard_wcanalytics_inactive_contact_us),
            onClick = onContactSupportClick
        )
    }
}

@Composable
@Preview
private fun WCAdminNotAvailableErrorViewPreview() {
    WooThemeWithBackground {
        WCAnalyticsNotAvailableErrorView(
            title = stringResource(id = R.string.my_store_stats_plugin_inactive_title),
            onContactSupportClick = {}
        )
    }
}
