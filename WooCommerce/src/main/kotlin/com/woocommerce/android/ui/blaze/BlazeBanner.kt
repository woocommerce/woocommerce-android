package com.woocommerce.android.ui.blaze

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCTextButton

@Composable
fun BlazeBanner(
    onClose: () -> Unit,
    onTryBlazeClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.major_100))
            .background(MaterialTheme.colors.surface)
    ) {
        IconButton(
            modifier = Modifier.align(Alignment.TopEnd),
            onClick = onClose
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.blaze_banner_close_button_content_description),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_150)),
                painter = painterResource(R.drawable.ic_blaze_banner_flame),
                contentDescription = ""
            )
            Text(
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)),
                text = stringResource(id = R.string.blaze_banner_title),
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Text(
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.minor_100)),
                text = stringResource(id = R.string.blaze_banner_description),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.subtitle1,
            )
            WCTextButton(
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.minor_100)),
                onClick = onTryBlazeClicked
            ) {
                Text(
                    text = stringResource(R.string.blaze_banner_button),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Suppress("unused")
@Composable
private fun BlazeBannerPreview() {
    BlazeBanner(
        onClose = {},
        onTryBlazeClicked = {}
    )
}

