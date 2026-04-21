package com.woocommerce.android.ui.woopos.cardreader.remote

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosRemoteReaderExplainerDialog(onDismiss: () -> Unit) {
    WooPosDialogWrapper(
        isVisible = true,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_remote_ttp_explainer_background_content_description
        ),
        widthFraction = 0.55f,
        onCloseClick = onDismiss,
        onDismissRequest = onDismiss,
    ) {
        ExplainerContent(onDismiss = onDismiss)
    }
}

@Composable
private fun ExplainerContent(onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        WooPosText(
            text = stringResource(R.string.woopos_remote_ttp_explainer_title),
            style = WooPosTypography.Heading,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosText(
            text = stringResource(R.string.woopos_remote_ttp_explainer_subtitle),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        WooPosText(
            text = stringResource(R.string.woopos_remote_ttp_explainer_phone_setup_heading),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        NumberedStep(number = "1.", text = stringResource(R.string.woopos_remote_ttp_explainer_step_install))
        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
        NumberedStep(number = "2.", text = stringResource(R.string.woopos_remote_ttp_explainer_step_open_settings))
        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
        NumberedStep(number = "3.", text = stringResource(R.string.woopos_remote_ttp_explainer_step_card_reader_mode))

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        WooPosText(
            text = stringResource(R.string.woopos_remote_ttp_explainer_requirements_heading),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        BulletItem(text = stringResource(R.string.woopos_remote_ttp_explainer_req_wifi))
        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
        BulletItem(text = stringResource(R.string.woopos_remote_ttp_explainer_req_android))
        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
        BulletItem(text = stringResource(R.string.woopos_remote_ttp_explainer_req_ttp))

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))

        WooPosButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.woopos_remote_ttp_explainer_got_it),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun NumberedStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        WooPosText(
            text = number,
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
        WooPosText(
            text = text,
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BulletItem(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        WooPosText(
            text = "•",
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
        WooPosText(
            text = text,
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@WooPosPreview
@Composable
fun WooPosRemoteReaderExplainerDialogPreview() {
    WooPosTheme {
        ExplainerContent(onDismiss = {})
    }
}
