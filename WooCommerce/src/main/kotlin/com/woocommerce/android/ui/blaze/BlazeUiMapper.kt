package com.woocommerce.android.ui.blaze

import com.woocommerce.android.R
import com.woocommerce.android.util.CurrencyFormatter
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel

fun BlazeCampaignModel.toUiState(currencyFormatter: CurrencyFormatter) =
    BlazeCampaignUi(
        product = BlazeProductUi(
            name = title,
            imgUrl = imageUrl.orEmpty(),
        ),
        status = CampaignStatusUi.fromString(uiStatus),
        stats = listOf(
            BlazeCampaignStat(
                name = R.string.blaze_campaign_status_ctr,
                value = "$impressions → $clicks"
            ),
            BlazeCampaignStat(
                name = getBudgetTitle(this),
                value = getBudgetValue(this, currencyFormatter)
            )
        ),
        isEndlessCampaign = isEndlessCampaign
    )

private fun getBudgetTitle(campaign: BlazeCampaignModel) =
    when {
        campaign.isEndlessCampaign -> R.string.blaze_campaign_status_budget_weekly
        CampaignStatusUi.isActive(campaign.uiStatus) -> R.string.blaze_campaign_status_budget_remaining
        else -> R.string.blaze_campaign_status_budget_total
    }

private fun getBudgetValue(campaign: BlazeCampaignModel, currencyFormatter: CurrencyFormatter): String =
    currencyFormatter.formatCurrencyRounded(
        when {
            campaign.isEndlessCampaign -> getWeeklyBudget(campaign)
            CampaignStatusUi.isActive(campaign.uiStatus) -> (campaign.totalBudget - campaign.spentBudget)
            else -> campaign.totalBudget
        }
    )

private fun getWeeklyBudget(campaign: BlazeCampaignModel): Double =
    (campaign.totalBudget / campaign.durationInDays) * BlazeRepository.WEEKLY_DURATION
