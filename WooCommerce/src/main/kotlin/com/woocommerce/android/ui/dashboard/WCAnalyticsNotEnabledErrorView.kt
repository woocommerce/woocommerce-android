package com.woocommerce.android.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground

@Composable
fun WCAnalyticsNotAvailableErrorView(
    title: String,
    onContactSupportClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(WooTheme.padding.padding5)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_woo_generic_error),
            contentDescription = null
        )

        Text(
            text = title,
            style = WooTheme.text.titleLarge.strong,
            color = WooTheme.colors.surface.onDefault,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(id = R.string.dashboard_wcanalytics_inactive_description),
            style = WooTheme.text.bodyLarge.regular,
            color = WooTheme.colors.surface.onDefault,
            textAlign = TextAlign.Center
        )

        WooOutlinedButton(
            text = stringResource(id = R.string.dashboard_wcanalytics_inactive_contact_us),
            onClick = onContactSupportClick
        )
    }
}

@Composable
@PreviewLightDark
private fun WCAdminNotAvailableErrorViewPreview() {
    WooDesignSystemThemeWithBackground {
        WCAnalyticsNotAvailableErrorView(
            title = stringResource(id = R.string.my_store_stats_plugin_inactive_title),
            onContactSupportClick = {}
        )
    }
}
