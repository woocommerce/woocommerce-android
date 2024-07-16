package com.woocommerce.android.ui.dashboard.google

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry

@Composable
fun DashboardGoogleAdsCard(modifier: Modifier = Modifier) {
    // Displaying multiple states at once for testing purposes
    Column {
        WidgetCard(
            titleResource = DashboardWidget.Type.GOOGLE_ADS.titleResource,
            menu = DashboardWidgetMenu(
                listOf(
                    DashboardWidget.Type.GOOGLE_ADS.defaultHideMenuEntry { /* TODO */ }
                )
            ),
            isError = false,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.major_100),
                    )
            ) {
                GoogleAdsLoading()
            }
        }

        Spacer(modifier = modifier.height(32.dp))

        WidgetCard(
            titleResource = DashboardWidget.Type.GOOGLE_ADS.titleResource,
            menu = DashboardWidgetMenu(
                listOf(
                    DashboardWidget.Type.GOOGLE_ADS.defaultHideMenuEntry { /* TODO */ }
                )
            ),
            isError = false,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.major_100),
                    )
            ) {
                GoogleAdsNoCampaigns()
            }
        }

        Spacer(modifier = modifier.height(32.dp))

        WidgetCard(
            titleResource = DashboardWidget.Type.GOOGLE_ADS.titleResource,
            menu = DashboardWidgetMenu(
                listOf(
                    DashboardWidget.Type.GOOGLE_ADS.defaultHideMenuEntry { /* TODO */ }
                )
            ),
            button = DashboardViewModel.DashboardWidgetAction(
                titleResource = R.string.dashboard_google_ads_card_view_all_campaigns_button,
                action = { /* TODO */ }
            ),
            isError = false,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.major_100),
                    )
            ) {
                GoogleAdsWithCampaigns()
            }
        }
    }
}

@Composable
private fun GoogleAdsLoading(
    modifier: Modifier = Modifier
) {
    val roundedShape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .border(
                    width = dimensionResource(id = R.dimen.minor_10),
                    color = colorResource(id = R.color.divider_color),
                    shape = roundedShape
                )
                .clip(roundedShape)
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.major_100)
                )
                .background(MaterialTheme.colors.surface)
                .fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.google_logo),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R.dimen.major_250))
            )
            Column(
                modifier = Modifier
                    .padding(start = dimensionResource(id = R.dimen.major_100))
                    .weight(1f)
            ) {
                SkeletonView(width = 200.dp, height = 24.dp)
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.minor_100)))
                SkeletonView(width = 250.dp, height = 16.dp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .border(
                width = dimensionResource(id = R.dimen.minor_10),
                color = colorResource(id = R.color.divider_color),
                shape = roundedShape
            )
            .clip(roundedShape)
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.major_100)
            )
            .background(MaterialTheme.colors.surface)
            .fillMaxWidth()
    ) {
        SkeletonView(width = 200.dp, height = 24.dp)
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun GoogleAdsNoCampaigns(
    modifier: Modifier = Modifier
) {
    val roundedShape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
    Column {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier
                .border(
                    width = dimensionResource(id = R.dimen.minor_10),
                    color = colorResource(id = R.color.divider_color),
                    shape = roundedShape
                )
                .clip(roundedShape)
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.major_100)
                )
                .background(MaterialTheme.colors.surface)
        ) {
            Image(
                painter = painterResource(id = R.drawable.google_logo),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R.dimen.major_250))
            )
            Column(
                modifier = Modifier
                    .padding(start = dimensionResource(id = R.dimen.major_100))
                    .weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.dashboard_google_ads_card_no_campaign_heading),
                    style = MaterialTheme.typography.h6
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.minor_100)))
                Text(
                    text = stringResource(R.string.dashboard_google_ads_card_no_campaign_description),
                    style = MaterialTheme.typography.body1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        WCOutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(id = R.dimen.minor_100),
                    bottom = dimensionResource(id = R.dimen.major_100)
                ),
            onClick = { /* TODO */ },
        ) {
            Text(stringResource(R.string.dashboard_google_ads_card_create_campaign_button))
        }
    }
}

@Composable
private fun GoogleAdsWithCampaigns(
    modifier: Modifier = Modifier
) {
    val roundedShape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
    Column {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier
                .border(
                    width = dimensionResource(id = R.dimen.minor_10),
                    color = colorResource(id = R.color.divider_color),
                    shape = roundedShape
                )
                .clip(roundedShape)
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.major_100)
                )
                .background(MaterialTheme.colors.surface)
        ) {
            Image(
                painter = painterResource(id = R.drawable.google_logo),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R.dimen.major_250))
            )
            Column(
                modifier = Modifier
                    .padding(start = dimensionResource(id = R.dimen.major_100))
                    .weight(1f),
            ) {
                Row {
                    Text(
                        text = stringResource(R.string.dashboard_google_ads_card_has_campaign_heading),
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right),
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.major_100)))

                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dashboard_google_ads_card_has_campaign_impressions),
                            style = MaterialTheme.typography.body2
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                        Text(
                            text = "1234567890",
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dashboard_google_ads_card_has_campaign_clicks),
                            style = MaterialTheme.typography.body2
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                        Text(
                            text = "25300",
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        WCOutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(id = R.dimen.minor_100),
                    bottom = dimensionResource(id = R.dimen.major_100)
                ),
            onClick = { /* TODO */ },
        ) {
            Text(stringResource(R.string.dashboard_google_ads_card_create_campaign_button))
        }
    }
}
