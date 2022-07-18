package com.woocommerce.android.ui.compose.component.banner

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.TakePaymentViewState.Success
import com.woocommerce.android.ui.prefs.MainSettingsContract

@Composable
fun PaymentsScreenBanner(
    viewModel: SelectPaymentMethodViewModel,
    title: String,
    subtitle: String,
    ctaLabel: String,
) {
    val selectPaymentState by viewModel.viewStateData.observeAsState(
        SelectPaymentMethodViewModel.TakePaymentViewState.Loading
    )
    if (
        selectPaymentState is Success &&
        (selectPaymentState as Success).isPaymentCollectableWithCardReader &&
        viewModel.canShowCardReaderUpsellBanner(System.currentTimeMillis())
    ) {
        Banner(
            onCtaClick = viewModel::onCtaClicked,
            onDismissClick = viewModel::onDismissClicked,
            title = title,
            subtitle = subtitle,
            ctaLabel = ctaLabel,
        )
    }
}

@Composable
fun OrderListScreenBanner(
    viewModel: OrderListViewModel,
    title: String,
    subtitle: String,
    ctaLabel: String,
) {
    if (viewModel.canShowCardReaderUpsellBanner(System.currentTimeMillis())) {
        Banner(
            onCtaClick = viewModel::onCtaClicked,
            onDismissClick = viewModel::onDismissClicked,
            title = title,
            subtitle = subtitle,
            ctaLabel = ctaLabel,
        )
    }
}

@Composable
fun SettingsScreenBanner(
    presenter: MainSettingsContract.Presenter,
    title: String,
    subtitle: String,
    ctaLabel: String,
) {
    if (presenter.canShowCardReaderUpsellBanner(System.currentTimeMillis())) {
        Banner(
            onCtaClick = presenter::onCtaClicked,
            onDismissClick = presenter::onDismissClicked,
            title = title,
            subtitle = subtitle,
            ctaLabel = ctaLabel,
        )
    }
}

@Composable
fun Banner(
    onCtaClick: () -> Unit,
    onDismissClick: () -> Unit,
    title: String,
    subtitle: String,
    ctaLabel: String,
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
                    text = title,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        bottom = dimensionResource(id = R.dimen.minor_100)
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(
                        bottom = dimensionResource(id = R.dimen.minor_100)
                    )
                )
                Text(
                    text = ctaLabel,
                    color = colorResource(id = R.color.color_secondary),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(
                            top = dimensionResource(id = R.dimen.minor_100),
                            bottom = dimensionResource(id = R.dimen.major_110)
                        )
                        .clickable(
                            onClick = onCtaClick
                        )
                )
            }
            Column {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.End),
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
            title = stringResource(id = R.string.card_reader_upsell_card_reader_banner_title),
            subtitle = stringResource(id = R.string.card_reader_upsell_card_reader_banner_description),
            ctaLabel = stringResource(id = R.string.card_reader_upsell_card_reader_banner_cta)
        )
    }
}
