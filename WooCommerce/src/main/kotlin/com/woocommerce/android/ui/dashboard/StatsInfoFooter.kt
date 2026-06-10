package com.woocommerce.android.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun StatsInfoFooter(
    text: String,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        // The whole row is the touch target (rather than just the 16dp icon) so it meets the
        // minimum touch-target size without making the footer any taller.
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onInfoClick)
            .padding(dimensionResource(id = R.dimen.major_100)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(id = R.color.color_on_surface_medium),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_50)))
        Icon(
            painter = painterResource(id = R.drawable.ic_tintable_info_outline_24dp),
            contentDescription = stringResource(id = R.string.dashboard_stats_info_content_description),
            tint = colorResource(id = R.color.color_primary),
            modifier = Modifier.size(dimensionResource(id = R.dimen.major_100))
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun StatsInfoFooterDelayedPreview() {
    WooThemeWithBackground {
        StatsInfoFooter(
            text = stringResource(id = R.string.dashboard_stats_delayed_footer),
            onInfoClick = {}
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun StatsInfoFooterLastUpdatePreview() {
    WooThemeWithBackground {
        StatsInfoFooter(
            text = "Last update: 8:52 AM",
            onInfoClick = {}
        )
    }
}
