package com.woocommerce.android.ui.woopos.cardreader.remote

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            text = stringResource(R.string.woopos_remote_ttp_explainer_description),
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        ExplainerSectionHeading(stringResource(R.string.woopos_remote_ttp_explainer_what_you_need_heading))
        ExplainerBody(stringResource(R.string.woopos_remote_ttp_explainer_what_you_need_body))

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        ExplainerSectionHeading(stringResource(R.string.woopos_remote_ttp_explainer_connect_heading))
        listOf(
            R.string.woopos_remote_ttp_explainer_connect_step_same_store,
            R.string.woopos_remote_ttp_explainer_connect_step_phone,
            R.string.woopos_remote_ttp_explainer_connect_step_tablet,
        ).forEachIndexed { index, step ->
            ExplainerStep(number = index + 1, text = stringResource(step))
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))

        WooPosButton(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            text = stringResource(R.string.woopos_remote_ttp_explainer_got_it),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun ExplainerSectionHeading(text: String) {
    WooPosText(
        text = text,
        style = WooPosTypography.BodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = WooPosSpacing.XSmall.value),
    )
}

@Composable
private fun ExplainerBody(text: String, modifier: Modifier = Modifier) {
    WooPosText(
        text = text,
        style = WooPosTypography.BodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun ExplainerStep(number: Int, text: String) {
    Row(modifier = Modifier.padding(top = WooPosSpacing.XSmall.value)) {
        ExplainerBody(
            text = "$number.",
            modifier = Modifier
                .alignByBaseline()
                .padding(end = WooPosSpacing.Small.value),
        )
        ExplainerBody(
            text = text,
            modifier = Modifier
                .alignByBaseline()
                .weight(1f),
        )
    }
}

@WooPosPreview
@Composable
fun WooPosRemoteReaderExplainerContentPreview() {
    WooPosTheme {
        WooPosRemoteReaderExplainerContent(onDismiss = {})
    }
}
