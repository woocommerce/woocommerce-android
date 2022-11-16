package com.woocommerce.android.ui.login.storecreation.mystoresummary

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.mystoresummary.MyStoreSummaryViewModel.MyStoreSummaryState

@Composable
fun MyStoreSummaryScreen(viewModel: MyStoreSummaryViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        Scaffold(topBar = {
            Toolbar(
                onArrowBackPressed = viewModel::onBackPressed,
            )
        }) {
            MyStoreSummaryScreen(
                myStoreSummaryState = viewState,
                onContinueClicked = viewModel::onContinueClicked,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.surface)
            )
        }
    }
}

@Composable
private fun Toolbar(
    onArrowBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = {},
        navigationIcon = {
            IconButton(onClick = onArrowBackPressed) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}

@Composable
private fun MyStoreSummaryScreen(
    myStoreSummaryState: MyStoreSummaryState,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_125)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        ) {
            Text(
                text = stringResource(id = R.string.store_creation_summary_title),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(id = R.string.store_creation_summary_subtitle),
                style = MaterialTheme.typography.body1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
            StoreDataSummary(myStoreSummaryState)
        }
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
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
                        bottom = dimensionResource(id = R.dimen.major_150)
                    ),
                onClick = onContinueClicked,
            ) {
                Text(text = stringResource(id = R.string.store_creation_summary_primary_button))
            }
        }
    }
}

@Composable
private fun StoreDataSummary(myStoreSummaryState: MyStoreSummaryState) {
    Card(
        elevation = 0.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            dimensionResource(id = R.dimen.minor_10),
            colorResource(id = R.color.divider_color)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_150))
        ) {
            CardHeader()
            if (!myStoreSummaryState.name.isNullOrEmpty()) {
                Column(
                    modifier = Modifier.padding(
                        start = dimensionResource(id = R.dimen.major_100)
                    )
                ) {
                    Text(
                        text = myStoreSummaryState.name,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = myStoreSummaryState.domain,
                        style = MaterialTheme.typography.body2,
                        color = colorResource(id = R.color.color_on_surface_medium_selector)
                    )
                }
            }
            if (!myStoreSummaryState.category.isNullOrEmpty()) {
                Text(
                    modifier = Modifier.padding(
                        start = dimensionResource(id = R.dimen.major_100)
                    ),
                    text = myStoreSummaryState.category,
                    style = MaterialTheme.typography.body1,
                )
            }

            Text(
                modifier = Modifier.padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    bottom = dimensionResource(id = R.dimen.major_150),
                ),
                text = myStoreSummaryState.country,
                style = MaterialTheme.typography.body1,
            )
        }
    }
}

@Composable
private fun CardHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.woo_gray_6))
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
                category = "Arts and Crafts",
                country = "Canada"
            ),
            onContinueClicked = {}
        )
    }
}
