package com.woocommerce.android.ui.orders.details.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.model.OrderAttributionOrigin
import com.woocommerce.android.model.origin
import com.woocommerce.android.ui.compose.component.WCTextButton
import org.wordpress.android.fluxc.model.OrderAttributionInfo

@Composable
fun OrderDetailAttributionInfoView(attributionInfo: OrderAttributionInfo) {
    Column {
        Text(
            text = stringResource(id = R.string.order_detail_attribution_header).uppercase(),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
        )
        Card(
            shape = RectangleShape
        ) {
            OrderAttributionContent(attributionInfo)
        }
    }
}

@Composable
private fun OrderAttributionContent(
    attributionInfo: OrderAttributionInfo,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
        modifier = modifier
    ) {
        var detailsExpanded by rememberSaveable { mutableStateOf(false) }

        OrderAttributionInfoRow(
            title = stringResource(id = R.string.order_detail_attribution_origin),
            value = attributionInfo.origin.label
        )

        AnimatedVisibility(
            enter = expandVertically(),
            exit = shrinkVertically(),
            visible = detailsExpanded,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
            ) {
                attributionInfo.sourceType?.let { sourceType ->
                    OrderAttributionInfoRow(
                        title = stringResource(id = R.string.order_detail_attribution_source_type),
                        value = sourceType
                    )
                }

                attributionInfo.campaign?.let { campaign ->
                    OrderAttributionInfoRow(
                        title = stringResource(id = R.string.order_detail_attribution_campaign),
                        value = campaign
                    )
                }

                attributionInfo.source?.let { source ->
                    OrderAttributionInfoRow(
                        title = stringResource(id = R.string.order_detail_attribution_source),
                        value = source
                    )
                }

                attributionInfo.medium?.let { medium ->
                    OrderAttributionInfoRow(
                        title = stringResource(id = R.string.order_detail_attribution_medium),
                        value = medium
                    )
                }

                attributionInfo.deviceType?.let { deviceType ->
                    OrderAttributionInfoRow(
                        title = stringResource(id = R.string.order_detail_attribution_device_type),
                        value = deviceType
                    )
                }

                attributionInfo.sessionPageViews?.let { sessionPageViews ->
                    OrderAttributionInfoRow(
                        title = stringResource(id = R.string.order_detail_attribution_session_page_views),
                        value = sessionPageViews
                    )
                }
            }
        }

        if (attributionInfo.hasAdditionalDetails) {
            WCTextButton(
                onClick = { detailsExpanded = !detailsExpanded },
                contentPadding = PaddingValues(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = ButtonDefaults.TextButtonContentPadding.calculateTopPadding()
                ),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            id = if (detailsExpanded) R.string.hide_details else R.string.show_details
                        ).uppercase()
                    )
                    Icon(
                        painter = painterResource(
                            id = if (detailsExpanded) {
                                R.drawable.ic_arrow_up
                            } else {
                                R.drawable.ic_arrow_down
                            }
                        ),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderAttributionInfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_100)
            )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(end = dimensionResource(id = R.dimen.major_100))
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
        )
    }
}

private val OrderAttributionOrigin.label
    @Composable
    get() = when (this) {
        is OrderAttributionOrigin.Utm -> stringResource(
            id = R.string.order_detail_attribution_utm_origin,
            source ?: stringResource(id = R.string.order_detail_attribution_unknown_origin)
        )

        is OrderAttributionOrigin.Referral -> stringResource(
            id = R.string.order_detail_attribution_referral_origin,
            source ?: stringResource(id = R.string.order_detail_attribution_unknown_origin)
        )

        is OrderAttributionOrigin.Organic -> stringResource(
            id = R.string.order_detail_attribution_organic_origin,
            source ?: stringResource(id = R.string.order_detail_attribution_unknown_origin)
        )

        OrderAttributionOrigin.Direct -> stringResource(id = R.string.order_detail_attribution_direct_origin)
        OrderAttributionOrigin.Admin -> stringResource(id = R.string.order_detail_attribution_admin_origin)
        OrderAttributionOrigin.Mobile -> stringResource(id = R.string.order_detail_attribution_mobile_origin)
        OrderAttributionOrigin.Unknown -> stringResource(id = R.string.order_detail_attribution_unknown_origin)
    }

private val OrderAttributionInfo.hasAdditionalDetails
    get() = campaign != null ||
        source != null ||
        sourceType != null ||
        medium != null ||
        deviceType != null ||
        sessionPageViews != null
