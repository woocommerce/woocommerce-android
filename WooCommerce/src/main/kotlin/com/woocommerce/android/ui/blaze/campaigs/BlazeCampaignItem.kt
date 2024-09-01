package com.woocommerce.android.ui.blaze.campaigs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.component.WCTag

@Composable
fun BlazeCampaignItem(
    campaign: BlazeCampaignUi,
    onCampaignClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .border(
                width = dimensionResource(id = R.dimen.minor_10),
                color = colorResource(R.color.divider_color),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
            )
            .clip(shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)))
            .clickable { onCampaignClicked() }
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Row(verticalAlignment = Alignment.Top) {
            ProductThumbnail(imageUrl = campaign.product.imgUrl)
            Column(
                modifier = Modifier
                    .padding(start = dimensionResource(id = R.dimen.major_100))
                    .weight(1f),
            ) {
                campaign.status?.let {
                    WCTag(
                        text = stringResource(id = campaign.status.statusDisplayText).uppercase(),
                        textColor = colorResource(id = campaign.status.textColor),
                        backgroundColor = colorResource(id = campaign.status.backgroundColor),
                        textStyle = MaterialTheme.typography.caption.copy(
                            letterSpacing = 1.5.sp
                        )
                    )
                }

                Text(
                    modifier = Modifier
                        .padding(top = dimensionResource(id = R.dimen.minor_50)),
                    text = campaign.product.name,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
                ) {
                    CampaignStat(
                        statName = stringResource(R.string.blaze_campaign_status_ctr_label),
                        statValue = stringResource(
                            id = R.string.blaze_campaign_status_ctr_valur,
                            campaign.clicks,
                            campaign.impressions
                        )
                    )
                    Spacer(modifier = Modifier.width(42.dp))
                    CampaignStat(
                        statName = stringResource(campaign.budgetLabel),
                        statValue = campaign.formattedBudget
                    )
                }
            }
        }
    }
}

@Composable
private fun CampaignStat(
    statName: String,
    statValue: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(end = dimensionResource(id = R.dimen.major_100))
    ) {
        Text(
            text = statName,
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.color_on_surface_medium_selector),
        )
        Text(
            modifier = Modifier
                .padding(top = dimensionResource(id = R.dimen.minor_50)),
            text = statValue,
            style = MaterialTheme.typography.h6,
        )
    }
}
