package com.woocommerce.android.ui.blaze

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.blaze.MyStoreBlazeViewModel.MyStoreBlazeCampaignState
import com.woocommerce.android.ui.blaze.campaigs.BlazeCampaignItem
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.component.WCTextButton

@Composable
fun MyStoreBlazeView(
    state: MyStoreBlazeCampaignState,
    onDismissBlazeView: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Column(
                    modifier = Modifier.padding(
                        top = dimensionResource(id = R.dimen.major_100),
                        start = dimensionResource(id = R.dimen.major_100),
                        end = dimensionResource(id = R.dimen.major_100),
                    )
                ) {
                    BlazeCampaignHeader()
                    when (state) {
                        is MyStoreBlazeCampaignState.Campaign -> BlazeCampaignItem(
                            campaign = state.campaign,
                            onCampaignClicked = state.onCampaignClicked,
                            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
                        )

                        is MyStoreBlazeCampaignState.NoCampaign -> {
                            Text(
                                modifier = Modifier.padding(
                                    top = dimensionResource(id = R.dimen.major_100),
                                    end = dimensionResource(id = R.dimen.major_300)
                                ),
                                text = stringResource(id = R.string.blaze_campaign_subtitle),
                                style = MaterialTheme.typography.body1,
                            )
                            BlazeProductItem(
                                product = state.product,
                                onProductSelected = state.onProductClicked,
                                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
                            )
                        }

                        else -> error("Invalid state")
                    }
                }
                when (state) {
                    is MyStoreBlazeCampaignState.Campaign -> ShowAllOrCreateCampaignFooter(
                        onShowAllClicked = state.onViewAllCampaignsClicked,
                        onCreateCampaignClicked = state.onCreateCampaignClicked
                    )

                    is MyStoreBlazeCampaignState.NoCampaign -> CreateCampaignFooter(
                        onCreateCampaignClicked = state.onCreateCampaignClicked,
                        modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
                    )

                    else -> error("Invalid state")
                }
            }
            WcOverflowMenu(
                items = arrayOf(stringResource(id = R.string.blaze_overflow_menu_hide_blaze)),
                onSelected = onDismissBlazeView,
            )
        }
    }
}

@Composable
private fun CreateCampaignFooter(
    onCreateCampaignClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Divider(Modifier.padding(start = dimensionResource(id = R.dimen.major_100)))
        WCTextButton(
            onClick = onCreateCampaignClicked,
            contentPadding = PaddingValues(
                start = dimensionResource(id = R.dimen.major_100),
                end = dimensionResource(id = R.dimen.major_100),
                top = ButtonDefaults.TextButtonContentPadding.calculateTopPadding(),
                bottom = ButtonDefaults.TextButtonContentPadding.calculateBottomPadding(),
            )
        ) {
            Text(stringResource(id = R.string.blaze_campaign_create_campaign_button))
        }
    }
}

@Composable
private fun ShowAllOrCreateCampaignFooter(
    onShowAllClicked: () -> Unit,
    onCreateCampaignClicked: () -> Unit,
) {
    Row {
        WCTextButton(
            modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_75)),
            onClick = onShowAllClicked
        ) {
            Text(stringResource(id = R.string.blaze_campaign_show_all_button))
        }
        WCTextButton(
            modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100)),
            onClick = onCreateCampaignClicked
        ) {
            Text(stringResource(id = R.string.blaze_campaign_create_campaign_button))
        }
    }
}

@Composable
private fun BlazeCampaignHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.ic_blaze),
            contentDescription = "", // Blaze icon, no relevant content desc
            modifier = Modifier
                .padding(end = dimensionResource(id = R.dimen.minor_100))
                .size(dimensionResource(id = R.dimen.major_125))
        )
        Text(
            text = stringResource(id = R.string.blaze_campaign_title),
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BlazeProductItem(
    product: BlazeProductUi,
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
            ProductThumbnail(imageUrl = product.imgUrl)
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

@Composable
private fun <T> BoxScope.WcOverflowMenu(
    items: Array<T>,
    onSelected: (T) -> Unit,
    mapper: (T) -> String = { it.toString() },
) {
    var showMenu by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = dimensionResource(id = dimen.minor_100))
    ) {
        IconButton(onClick = { showMenu = !showMenu }) {
            Icon(
                imageVector = Outlined.MoreVert,
                contentDescription = stringResource(string.more_menu),
            )
        }
        DropdownMenu(
            offset = DpOffset(
                x = dimensionResource(id = dimen.major_100),
                y = 0.dp
            ),
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    modifier = Modifier.height(dimensionResource(id = dimen.major_175)),
                    onClick = {
                        showMenu = false
                        onSelected(item)
                    }
                ) {
                    Text(mapper(item))
                }
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_100)))
                }
            }
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
fun MyStoreBlazeViewCampaignPreview() {
    val product = BlazeProductUi(
        name = "Product name",
        imgUrl = "",
    )
    MyStoreBlazeView(
        state = MyStoreBlazeCampaignState.Campaign(
            campaign = BlazeCampaignUi(
                product = product,
                status = CampaignStatusUi.Active,
                stats = listOf(
                    BlazeCampaignStat(
                        name = R.string.blaze_campaign_status_impressions,
                        value = 100.toString()
                    ),
                    BlazeCampaignStat(
                        name = R.string.blaze_campaign_status_clicks,
                        value = 10.toString()
                    ),
                    BlazeCampaignStat(
                        name = R.string.blaze_campaign_status_budget,
                        value = 1000.toString()
                    ),
                ),
            ),
            onCampaignClicked = {},
            onViewAllCampaignsClicked = {},
            onCreateCampaignClicked = {}
        ),
        onDismissBlazeView = {}
    )
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun MyStoreBlazeViewNoCampaignPreview() {
    MyStoreBlazeView(
        state = MyStoreBlazeCampaignState.NoCampaign(
            product = BlazeProductUi(
                name = "Product name",
                imgUrl = "",
            ),
            onProductClicked = {},
            onCreateCampaignClicked = {}
        ),
        onDismissBlazeView = {}
    )
}
