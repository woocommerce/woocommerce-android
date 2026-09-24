package com.woocommerce.android.ui.blaze.campaigs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.campaigs.BlazeCampaignListViewModel.OutstandingBalanceUi
import com.woocommerce.android.ui.blaze.campaigs.BlazeCampaignListViewModel.UnpaidOrderUi
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.LegacyWooThemeWithBackground

private const val ERROR_BACKGROUND_ALPHA = 0.1f

@Composable
fun BlazeOutstandingBalanceNotice(
    outstandingBalance: OutstandingBalanceUi,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_large))
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colors.error.copy(alpha = ERROR_BACKGROUND_ALPHA), shape = shape)
                .padding(dimensionResource(id = R.dimen.major_100)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_75))
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tintable_info_outline_24dp),
                contentDescription = null,
                tint = MaterialTheme.colors.error
            )
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50))) {
                Text(
                    text = stringResource(id = R.string.blaze_campaign_list_outstanding_balance_title),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        id = R.string.blaze_campaign_list_outstanding_balance_message,
                        outstandingBalance.formattedDebt
                    ),
                    style = MaterialTheme.typography.body2
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = dimensionResource(id = R.dimen.minor_10),
                    color = colorResource(id = R.color.divider_color),
                    shape = shape
                )
                .padding(dimensionResource(id = R.dimen.major_100)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
        ) {
            outstandingBalance.unpaidOrders.forEach { unpaidOrder ->
                UnpaidOrderRow(unpaidOrder = unpaidOrder)
            }
        }
    }
}

@Composable
private fun UnpaidOrderRow(unpaidOrder: UnpaidOrderUi) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
    ) {
        Column(modifier = Modifier.weight(1f)) {
            unpaidOrder.formattedDate?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                )
            }
            Text(
                text = unpaidOrder.formattedAmount,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
        }
        WCOutlinedButton(
            onClick = unpaidOrder.onPayClicked,
            text = stringResource(id = R.string.blaze_campaign_list_outstanding_balance_pay)
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun BlazeOutstandingBalanceNoticePreview() {
    LegacyWooThemeWithBackground {
        BlazeOutstandingBalanceNotice(
            outstandingBalance = OutstandingBalanceUi(
                formattedDebt = "$60.05",
                unpaidOrders = listOf(
                    UnpaidOrderUi(formattedDate = "Jun 19, 2025", formattedAmount = "$25.05", onPayClicked = {}),
                    UnpaidOrderUi(formattedDate = null, formattedAmount = "$35.00", onPayClicked = {})
                )
            ),
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
        )
    }
}
