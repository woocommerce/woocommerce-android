package com.woocommerce.android.ui.blaze.creation.success

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.FeedbackRequest
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews

@Composable
fun BlazeCampaignSuccessBottomSheet(
    onDoneTapped: () -> Unit,
    onFeedbackTapped: (Boolean) -> Unit,
    shouldShowFeedbackRequest: ShouldShowFeedbackRequest,
) {
    var showFeedbackRequest by remember { mutableStateOf(false) }
    LaunchedEffect(shouldShowFeedbackRequest) {
        showFeedbackRequest = shouldShowFeedbackRequest()
    }

    BlazeCampaignSuccessBottomSheet(
        onDoneTapped = onDoneTapped,
        onFeedbackTapped = onFeedbackTapped,
        showFeedbackRequest = showFeedbackRequest,
    )
}

@Composable
fun BlazeCampaignSuccessBottomSheet(
    onDoneTapped: () -> Unit,
    onFeedbackTapped: (Boolean) -> Unit,
    showFeedbackRequest: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = dimensionResource(id = dimen.minor_100),
            topEnd = dimensionResource(id = dimen.minor_100)
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(30.dp))
            Image(
                painter = painterResource(id = drawable.blaze_campaign_created_success),
                contentDescription = ""
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = string.blaze_campaign_created_success_title),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = stringResource(id = string.blaze_campaign_created_success_description),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(32.dp))
            WCColoredButton(
                onClick = onDoneTapped,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = string.blaze_campaign_created_success_done_button))
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (showFeedbackRequest) {
                FeedbackRequest(
                    onFeedbackReceived = onFeedbackTapped,
                    feedbackRequestText = R.string.blaze_campaign_created_success_feedback_request,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@LightDarkThemePreviews
@Composable
private fun BlazeCampaignSuccessBottomSheetPreview() {
    BlazeCampaignSuccessBottomSheet(
        onDoneTapped = {},
        onFeedbackTapped = {},
        showFeedbackRequest = true
    )
}
