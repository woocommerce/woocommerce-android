package com.woocommerce.android.ui.promobanner

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun PromoBanner(
    bannerType: PromoBannerType,
    onCtaClick: (String) -> Unit,
    onDismissClick: () -> Unit,
    source: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onDismissClick
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(id = R.string.card_reader_upsell_card_reader_banner_dismiss),
                    tint = colorResource(id = R.color.color_on_surface)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    top = dimensionResource(id = R.dimen.major_200)
                ),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(id = bannerType.titleRes),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        bottom = dimensionResource(id = R.dimen.minor_100)
                    )
                )
                Text(
                    text = stringResource(id = bannerType.messageRes),
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
                        text = stringResource(id = R.string.try_it_now),
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
private fun PromoBannerPreview() {
    WooThemeWithBackground {
        PromoBanner(PromoBannerType.LINKED_PRODUCTS, {}, {}, "")
    }
}
