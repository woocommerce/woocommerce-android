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

@Composable
fun Banner(bannerState: BannerState) {
    if (bannerState.shouldDisplayBanner) {
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    onClick = bannerState.onDismissClicked
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
                            text = stringResource(id = bannerState.chipLabel),
                            color = colorResource(id = R.color.woo_purple_60),
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = stringResource(id = bannerState.title),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            bottom = dimensionResource(id = R.dimen.minor_100)
                        )
                    )
                    Text(
                        text = stringResource(id = bannerState.description),
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
                        onClick = bannerState.onPrimaryActionClicked
                    ) {
                        Text(
                            text = stringResource(id = bannerState.primaryActionLabel),
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
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PaymentScreenBannerPreview() {
    WooThemeWithBackground {
        Banner(
            BannerState(
                shouldDisplayBanner = true,
                onPrimaryActionClicked = {},
                onDismissClicked = {},
                title = R.string.card_reader_upsell_card_reader_banner_title,
                description = R.string.card_reader_upsell_card_reader_banner_description,
                primaryActionLabel = R.string.card_reader_upsell_card_reader_banner_cta,
                chipLabel = R.string.card_reader_upsell_card_reader_banner_new
            )
        )
    }
}
