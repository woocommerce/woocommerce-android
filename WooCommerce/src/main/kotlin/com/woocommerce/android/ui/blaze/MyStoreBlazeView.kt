package com.woocommerce.android.ui.blaze

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.MyStoreBlazeViewModel.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.MyStoreBlazeViewModel.BlazeProduct
import com.woocommerce.android.ui.compose.component.ListItemImage
import com.woocommerce.android.ui.compose.component.WCTextButton

@Composable
fun MyStoreBlazeView(
    state: BlazeCampaignUi,
    onCreateCampaignClicked: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            Column(modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))) {
                Text(
                    text = stringResource(id = R.string.blaze_campaign_title),
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    modifier = Modifier.padding(
                        top = dimensionResource(id = R.dimen.major_100),
                        end = dimensionResource(id = R.dimen.major_300)
                    ),
                    text = stringResource(id = R.string.blaze_campaign_subtitle),
                    style = MaterialTheme.typography.subtitle1,
                    color = colorResource(id = R.color.color_on_surface_medium_selector)
                )
                if (!state.hasActiveCampaigns) {
                    BlazeProductItem(
                        product = state.product,
                        onProductSelected = {},
                        modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
                    )
                }
            }

            Divider()
            WCTextButton(
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_75)),
                onClick = onCreateCampaignClicked
            ) {
                Text(stringResource(id = R.string.blaze_campaign_create_campaign_button))
            }
        }
    }
}

@Composable
fun BlazeProductItem(
    product: BlazeProduct,
    onProductSelected: () -> Unit,
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
            .clickable { onProductSelected() }
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ListItemImage(
                imageUrl = product.imgUrl,
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.major_275))
                    .clip(shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))),
                placeHolderDrawableId = R.drawable.ic_product,
            )
            Text(
                modifier = Modifier
                    .padding(start = dimensionResource(id = R.dimen.major_100))
                    .weight(1f),
                text = product.name,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null
            )
        }
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun MyStoreBlazeViewPreview() {
    MyStoreBlazeView(
        state = BlazeCampaignUi(
            isVisible = true,
            hasActiveCampaigns = false,
            product = BlazeProduct(
                name = "Product name",
                imgUrl = "",
            )
        ),
        onCreateCampaignClicked = {}
    )
}
