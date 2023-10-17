package com.woocommerce.android.ui.blaze

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class BlazeProductUi(
    val name: String,
    val imgUrl: String
) : Parcelable

@Parcelize
data class BlazeCampaignUi(
    val product: BlazeProductUi,
    val status: CampaignStatusUi,
    val stats: List<BlazeCampaignStat>,
) : Parcelable

@Parcelize
data class BlazeCampaignStat(
    @StringRes val name: Int,
    val value: Int
) : Parcelable

enum class CampaignStatusUi(
    @StringRes val statusDisplayText: Int,
    @ColorRes val textColor: Int,
    @ColorRes val backgroundColor: Int,
) {
    InModeration(
        statusDisplayText = R.string.blaze_campaign_status_in_moderation,
        textColor = R.color.blaze_campaign_status_in_moderation_text,
        backgroundColor = R.color.blaze_campaign_status_in_moderation_background
    ),
    Active(
        statusDisplayText = R.string.blaze_campaign_status_active,
        textColor = R.color.blaze_campaign_status_active_text,
        backgroundColor = R.color.blaze_campaign_status_active_background
    ),
    Completed(
        statusDisplayText = R.string.blaze_campaign_status_completed,
        textColor = R.color.blaze_campaign_status_rejected_text,
        backgroundColor = R.color.blaze_campaign_status_rejected_background
    ),
    Rejected(
        statusDisplayText = R.string.blaze_campaign_status_rejected,
        textColor = R.color.blaze_campaign_status_completed_text,
        backgroundColor = R.color.blaze_campaign_status_completed_background
    ),
}
