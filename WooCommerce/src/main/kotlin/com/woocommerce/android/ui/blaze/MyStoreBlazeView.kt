package com.woocommerce.android.ui.blaze

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.blaze.MyStoreBlazeViewModel.BlazeCampaignUi

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
            Column(
                modifier = Modifier.padding(dimensionResource(id = dimen.major_100)),
            ) {
                Text(
                    modifier = Modifier.padding(top = dimensionResource(id = dimen.major_100)),
                    text = stringResource(id = R.string.blaze_campaign_title),
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    modifier = Modifier.padding(top = dimensionResource(id = dimen.major_100)),
                    text = stringResource(id = R.string.blaze_campaign_subtitle),
                    style = MaterialTheme.typography.subtitle1,
                    color = colorResource(id = R.color.color_on_surface_medium_selector)
                )
                if (!state.hasActiveCampaigns && state.product != null) {
                    BlazeProductItem(product = state.product)
                }
            }

            Divider()
            TextButton(onClick = onCreateCampaignClicked) {
                Text(stringResource(id = R.string.allow))
            }
        }
    }
}

@Composable
fun BlazeProductItem(product: Product) {
    Text(
        text = product.name,
        style = MaterialTheme.typography.subtitle1,
        fontWeight = FontWeight.Bold
    )
}

@ExperimentalFoundationApi
@PreviewLightDark
@PreviewScreenSizes
@Composable
fun MyStoreBlazeView() {
    MyStoreBlazeView(
        state = BlazeCampaignUi(
            isVisible = true,
            hasActiveCampaigns = false,
            product = null
        ),
        onCreateCampaignClicked = {}
    )
}
