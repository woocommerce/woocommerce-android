package com.woocommerce.android.ui.prefs.domain

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.prefs.domain.PurchaseSuccessfulViewModel.ViewState

@Composable
fun PurchaseSuccessfulScreen(viewModel: PurchaseSuccessfulViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        Crossfade(targetState = state) { viewState ->
            PurchaseSuccessful(viewState, viewModel::onDoneButtonClicked)
        }
    }
}

@Composable
private fun PurchaseSuccessful(viewState: ViewState, onDoneButtonClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colors.surface)
    ) {
        Image(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(250.dp)
                .padding(
                    top = dimensionResource(id = dimen.major_350),
                    bottom = dimensionResource(id = dimen.major_100)
                ),
            contentScale = ContentScale.FillHeight,
            painter = painterResource(id = drawable.img_domain_purchase),
            contentDescription = stringResource(string.domains_purchase_successful_heading)
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(
                    horizontal = dimensionResource(id = dimen.major_400),
                    vertical = dimensionResource(id = dimen.major_100)
                ),
            text = stringResource(id = string.domains_purchase_successful_heading),
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .padding(dimensionResource(id = dimen.major_100))
                .background(colorResource(id = color.color_default_image_background), RoundedCornerShape(8.dp))
        ) {
            Text(
                modifier = Modifier
                    .padding(dimensionResource(id = dimen.minor_100))
                    .fillMaxWidth(),
                text = viewState.domain,
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(dimensionResource(id = dimen.major_100)),
            text = stringResource(string.domains_purchase_successful_delay_notice),
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(dimensionResource(id = dimen.major_100)),
            text = stringResource(id = string.domains_purchase_successful_settings_notice),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = color.color_on_surface_medium),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .weight(1f)
        )
        Divider()
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = dimen.major_100)),
            onClick = onDoneButtonClicked
        ) {
            Text(text = stringResource(id = string.done))
        }
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun PurchaseSuccessfulPreview() {
    PurchaseSuccessful(viewState = ViewState(domain = "example.com"), {})
}
