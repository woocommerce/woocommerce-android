package com.woocommerce.android.ui.woopos.cardreader.remote

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosRemoteReaderExplainerContent(onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        WooPosText(
            text = stringResource(R.string.woopos_remote_ttp_explainer_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosText(
            text = stringResource(R.string.woopos_remote_ttp_explainer_intro),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))

        ExplainerSection(
            heading = stringResource(R.string.woopos_remote_ttp_explainer_setup_heading),
            body = stringResource(R.string.woopos_remote_ttp_explainer_setup_body),
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        ExplainerSection(
            heading = stringResource(R.string.woopos_remote_ttp_explainer_requirements_heading),
            body = stringResource(R.string.woopos_remote_ttp_explainer_requirements_body),
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))

        WooPosButton(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterHorizontally),
            text = stringResource(R.string.woopos_remote_ttp_explainer_got_it),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun ExplainerSection(heading: String, body: String) {
    WooPosText(
        text = heading,
        style = WooPosTypography.BodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
    WooPosText(
        text = body,
        style = WooPosTypography.BodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@WooPosPreview
@Composable
fun WooPosRemoteReaderExplainerContentPreview() {
    WooPosTheme {
        WooPosRemoteReaderExplainerContent(onDismiss = {})
    }
}
