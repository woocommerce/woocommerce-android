package com.woocommerce.android.ui.blaze.creation.success

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
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews

@Composable
fun BlazeCampaignSuccessBottomSheet(onDoneTapped: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.blaze_campaign_created_success),
            contentDescription = ""
        )
        Text(
            text = stringResource(id = R.string.blaze_campaign_created_success_title),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onSurface
        )
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(id = R.string.blaze_campaign_created_success_description),
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface
        )
        WCColoredButton(
            onClick = onDoneTapped,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.blaze_campaign_created_success_done_button))
        }
    }
}

@LightDarkThemePreviews
@Composable
private fun BlazeCampaignSuccessBottomSheetPreview() {
    BlazeCampaignSuccessBottomSheet(onDoneTapped = {})
}
