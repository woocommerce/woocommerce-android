package com.woocommerce.android.ui.payments.banner

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.TakePaymentViewState.Success
import com.woocommerce.android.ui.prefs.MainSettingsContract

@Composable
fun PaymentsScreenBanner(
    viewModel: SelectPaymentMethodViewModel,
) {
    val selectPaymentState by viewModel.viewStateData.observeAsState(
        SelectPaymentMethodViewModel.TakePaymentViewState.Loading
    )
    (selectPaymentState as? Success)?.upsellCardReaderBannerState?.let { banner ->
        if (banner.shouldShowCardReaderUpsellBanner) {
            Banner(
                onCtaClick = viewModel::onCtaClicked,
                onDismissClick = viewModel::onDismissClicked,
                title = banner.title,
                subtitle = banner.description,
                ctaLabel = banner.ctaLabel,
                source = AnalyticsTracker.KEY_BANNER_PAYMENTS
            )
        }
    }
}

@Composable
fun OrderListScreenBanner(
    viewModel: OrderListViewModel,
    title: Int,
    subtitle: Int,
    ctaLabel: Int,
) {
    val isEligibleForInPersonPayments by viewModel.isEligibleForInPersonPayments.observeAsState(false)

    if (
        isEligibleForInPersonPayments &&
        viewModel.canShowCardReaderUpsellBanner(System.currentTimeMillis(), AnalyticsTracker.KEY_BANNER_ORDER_LIST)
    ) {
        Banner(
            onCtaClick = viewModel::onCtaClicked,
            onDismissClick = viewModel::onDismissClicked,
            title = title,
            subtitle = subtitle,
            ctaLabel = ctaLabel,
            source = AnalyticsTracker.KEY_BANNER_ORDER_LIST
        )
    }
}

@Composable
fun SettingsScreenBanner(
    presenter: MainSettingsContract.Presenter,
    title: Int,
    subtitle: Int,
    ctaLabel: Int,
) {
    val isEligibleForInPersonPayments by presenter.isEligibleForInPersonPayments.observeAsState(false)
    if (
        isEligibleForInPersonPayments &&
        presenter.canShowCardReaderUpsellBanner(System.currentTimeMillis(), AnalyticsTracker.KEY_BANNER_SETTINGS)
    ) {
        Banner(
            onCtaClick = presenter::onCtaClicked,
            onDismissClick = presenter::onDismissClicked,
            title = title,
            subtitle = subtitle,
            ctaLabel = ctaLabel,
            source = AnalyticsTracker.KEY_BANNER_SETTINGS
        )
    }
}

@Composable
fun Banner(
    onCtaClick: (String) -> Unit,
    onDismissClick: () -> Unit,
    title: Int,
    subtitle: Int,
    ctaLabel: Int,
    source: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    top = dimensionResource(id = R.dimen.minor_100)
                ),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onDismissClick
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(
                        id = R.string.card_reader_upsell_card_reader_banner_dismiss
                    ),
                    tint = colorResource(id = R.color.color_on_surface)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    top = dimensionResource(id = R.dimen.minor_100)
                ),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .padding(
                            top = dimensionResource(id = R.dimen.major_100),
                            bottom = dimensionResource(id = R.dimen.minor_100)
                        )
                        .width(dimensionResource(id = R.dimen.major_275))
                        .height(dimensionResource(id = R.dimen.major_150))
                        .clip(
                            RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
                        )
                        .background(colorResource(id = R.color.woo_purple_10)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.card_reader_upsell_card_reader_banner_new),
                        color = colorResource(id = R.color.woo_purple_60),
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = stringResource(id = title),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        bottom = dimensionResource(id = R.dimen.minor_100)
                    )
                )
                Text(
                    text = stringResource(id = subtitle),
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(
                        bottom = dimensionResource(id = R.dimen.minor_100)
                    )
                )
                TextButton(
                    modifier = Modifier
                        .padding(
                            top = dimensionResource(id = R.dimen.minor_100),
                            bottom = dimensionResource(id = R.dimen.minor_100),
                        ),
                    contentPadding = PaddingValues(start = dimensionResource(id = R.dimen.minor_00)),
                    onClick = { onCtaClick(source) }
                ) {
                    Text(
                        text = stringResource(id = ctaLabel),
                        color = colorResource(id = R.color.color_secondary),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column {
                Image(
                    painter = painterResource(id = R.drawable.ic_banner_upsell_card_reader_illustration),
                    contentDescription = null,
                    contentScale = ContentScale.Inside
                )
            }
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PaymentScreenBannerPreview() {
    WooThemeWithBackground {
        Banner(
            onCtaClick = {},
            onDismissClick = {},
            title = R.string.card_reader_upsell_card_reader_banner_title,
            subtitle = R.string.card_reader_upsell_card_reader_banner_description,
            ctaLabel = R.string.card_reader_upsell_card_reader_banner_cta,
            source = AnalyticsTracker.KEY_BANNER_PAYMENTS
        )
    }
}
