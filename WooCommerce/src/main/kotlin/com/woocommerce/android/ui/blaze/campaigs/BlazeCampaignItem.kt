package com.woocommerce.android.ui.blaze.campaigs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.designsystem.WooTheme

/**
 * Blaze campaign item for migrated screens rooted in WooDesignSystemTheme.
 * Legacy-rooted screens should use [LegacyBlazeCampaignItem] until they migrate.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlazeCampaignItem(
    campaign: BlazeCampaignUi,
    onCampaignClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .border(
                width = WooTheme.stroke.regular,
                color = WooTheme.colors.outlineVariant,
                shape = RoundedCornerShape(WooTheme.radius.medium),
            )
            .clip(RoundedCornerShape(WooTheme.radius.medium))
            .clickable(onClick = onCampaignClicked)
            .padding(WooTheme.padding.padding5),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            ProductThumbnail(
                imageUrl = campaign.product.imgUrl,
                contentDescription = stringResource(id = R.string.product_image_content_description),
            )
            Column(
                modifier = Modifier
                    .padding(start = WooTheme.padding.padding5)
                    .weight(1f),
            ) {
                campaign.status?.let { status ->
                    BlazeStatusTag(
                        text = stringResource(id = status.statusDisplayText).uppercase(),
                        textColor = colorResource(id = status.textColor),
                        backgroundColor = colorResource(id = status.backgroundColor),
                    )
                }
                Text(
                    modifier = Modifier.padding(top = WooTheme.padding.padding2),
                    text = campaign.product.name,
                    style = WooTheme.text.titleMedium.strong,
                    color = WooTheme.colors.surface.onDefault,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    modifier = Modifier
                        .padding(top = WooTheme.padding.padding5)
                        .wrapContentWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CampaignStat(
                        statName = stringResource(R.string.blaze_campaign_status_ctr_label),
                        statValue = stringResource(
                            id = R.string.blaze_campaign_status_ctr_value_shortened,
                            campaign.impressions,
                            campaign.clicks,
                        ),
                        modifier = Modifier.padding(bottom = WooTheme.padding.padding5),
                    )
                    CampaignStat(
                        statName = stringResource(campaign.budgetLabel),
                        statValue = campaign.formattedBudget,
                    )
                }
            }
        }
    }
}

@Composable
private fun BlazeStatusTag(
    text: String,
    textColor: Color,
    backgroundColor: Color,
) {
    Text(
        text = text,
        style = WooTheme.text.labelSmall.regular.copy(letterSpacing = 1.5.sp),
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(WooTheme.radius.small))
            .background(backgroundColor)
            .padding(
                horizontal = WooTheme.padding.padding3 + WooTheme.padding.padding1,
                vertical = WooTheme.padding.padding2,
            ),
    )
}

@Composable
private fun CampaignStat(
    statName: String,
    statValue: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(end = WooTheme.padding.padding5)) {
        Text(
            text = statName,
            style = WooTheme.text.bodyMedium.regular,
            color = WooTheme.colors.surface.onDefault,
        )
        Text(
            modifier = Modifier.padding(top = WooTheme.padding.padding2),
            text = statValue,
            style = WooTheme.text.titleLarge.strong,
            color = WooTheme.colors.surface.onDefault,
        )
    }
}
