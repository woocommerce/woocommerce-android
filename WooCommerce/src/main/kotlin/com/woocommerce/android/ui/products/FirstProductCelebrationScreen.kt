package com.woocommerce.android.ui.products

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun FirstProductCelebrationScreen(viewModel: FirstProductCelebrationViewModel) {
    FirstProductCelebrationScreen(
        onShareClick = viewModel::onShareButtonClicked,
        onDismissClick = viewModel::onDismissButtonClicked
    )
}

@Composable
fun FirstProductCelebrationScreen(
    onShareClick: () -> Unit = {},
    onDismissClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .padding(dimensionResource(id = R.dimen.major_150))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(id = R.string.first_product_celebration_title),
                style = MaterialTheme.typography.h5,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val imageSize = if (isLandscape) 100.dp else 250.dp

            Image(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(imageSize)
                    .padding(
                        top = dimensionResource(id = R.dimen.major_150),
                        bottom = dimensionResource(id = R.dimen.major_100)
                    ),
                contentScale = ContentScale.FillHeight,
                painter = painterResource(id = R.drawable.img_welcome_light),
                contentDescription = stringResource(R.string.first_product_celebration_title)
            )

            Text(
                text = stringResource(id = R.string.first_product_celebration_body_message),
                style = MaterialTheme.typography.subtitle1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))

            if (isLandscape) {
                Row {
                    WCOutlinedButton(onClick = onDismissClick) {
                        Text(text = stringResource(id = R.string.jetpack_benefits_modal_dismiss))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    WCColoredButton(onClick = onShareClick) {
                        Text(text = stringResource(R.string.share_product))
                    }
                }
            } else {
                WCColoredButton(onClick = onShareClick, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.share_product))
                }

                WCOutlinedButton(onClick = onDismissClick, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.jetpack_benefits_modal_dismiss))
                }
            }
        }
    }
}

@Composable
@Preview
private fun FirstProductCelebrationScreenPreview() {
    WooThemeWithBackground {
        FirstProductCelebrationScreen()
    }
}
