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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosRemoteReaderExplainerDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
) {
    WooPosDialogWrapper(
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_remote_ttp_explainer_background_content_description
        ),
        widthFraction = 0.55f,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_remote_ttp_explainer_title),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosText(
                text = stringResource(R.string.woopos_remote_ttp_explainer_intro),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

            LabeledSection(
                heading = stringResource(R.string.woopos_remote_ttp_explainer_setup_heading),
                body = stringResource(R.string.woopos_remote_ttp_explainer_setup_steps),
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            LabeledSection(
                heading = stringResource(R.string.woopos_remote_ttp_explainer_requirements_heading),
                body = stringResource(R.string.woopos_remote_ttp_explainer_requirements_body),
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.XXXLarge.value))

            WooPosButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.woopos_remote_ttp_explainer_got_it),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun LabeledSection(heading: String, body: String) {
    WooPosText(
        text = heading,
        style = WooPosTypography.BodyLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
    WooPosText(
        text = body,
        style = WooPosTypography.BodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

@WooPosPreview
@Composable
fun WooPosRemoteReaderExplainerDialogPreview() {
    WooPosTheme {
        WooPosRemoteReaderExplainerDialog(isVisible = true, onDismiss = {})
    }
}
