package com.woocommerce.android.ui.login.storecreation.mystoresummary

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.mystoresummary.MyStoreSummaryViewModel.MyStoreSummaryState

@Composable
fun MyStoreSummaryScreen(viewModel: MyStoreSummaryViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        Scaffold(topBar = {
            ToolbarWithHelpButton(
                onNavigationButtonClick = viewModel::onBackPressed,
                onHelpButtonClick = viewModel::onHelpPressed,
            )
        }) { padding ->
            MyStoreSummaryScreen(
                myStoreSummaryState = viewState,
                onContinueClicked = viewModel::onContinueClicked,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.surface)
                    .padding(padding)
            )
        }
    }
}

@Composable
private fun MyStoreSummaryScreen(
    myStoreSummaryState: MyStoreSummaryState,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_125))
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        ) {
            Text(
                text = stringResource(id = R.string.store_creation_summary_title),
                style = MaterialTheme.typography.h5,
            )
            Text(
                text = stringResource(id = R.string.store_creation_summary_subtitle),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
            StoreDataSummary(
                myStoreSummaryState,
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_150))
            )
        }
        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10)
        )
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(id = R.dimen.major_100),
                    start = dimensionResource(id = R.dimen.major_100),
                    end = dimensionResource(id = R.dimen.major_100),
                    bottom = dimensionResource(id = R.dimen.major_125)
                ),
            onClick = onContinueClicked,
        ) {
            Text(text = stringResource(id = R.string.store_creation_summary_primary_button))
        }
    }
}

@Composable
private fun StoreDataSummary(
    myStoreSummaryState: MyStoreSummaryState,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = 0.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(
            dimensionResource(id = R.dimen.minor_10),
            colorResource(id = R.color.divider_color)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CardHeader(
                modifier = Modifier
                    .background(colorResource(id = R.color.woo_gray_6))
                    .padding(top = dimensionResource(id = R.dimen.major_150))
            )
            Column(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
            ) {
                if (!myStoreSummaryState.name.isNullOrEmpty()) {
                    Column {
                        Text(
                            text = myStoreSummaryState.name,
                            style = MaterialTheme.typography.h6,
                        )
                        Text(
                            text = myStoreSummaryState.domain,
                            style = MaterialTheme.typography.subtitle1,
                        )
                    }
                }
                if (myStoreSummaryState.country != null) {
                    Row {
                        Text(
                            text = myStoreSummaryState.country.name,
                            style = MaterialTheme.typography.subtitle1,
                        )
                        Text(
                            text = " ${myStoreSummaryState.country.emojiFlag}",
                            style = MaterialTheme.typography.subtitle1,
                        )
                    }
                }
                if (!myStoreSummaryState.industry.isNullOrEmpty()) {
                    Text(
                        text = myStoreSummaryState.industry,
                        style = MaterialTheme.typography.subtitle1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardHeader(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.my_store_summary_header_image),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun MyStoreSummaryScreenPreview() {
    WooThemeWithBackground {
        MyStoreSummaryScreen(
            myStoreSummaryState = MyStoreSummaryState(
                name = "White Christmas Trees",
                domain = "whitechristmastrees.mywc.mysite",
                industry = "Arts and Crafts",
                country = MyStoreSummaryViewModel.Country(
                    name = "Canada",
                    emojiFlag = "\uD83C\uDDE8\uD83C\uDDE6"
                )
            ),
            onContinueClicked = {}
        )
    }
}
