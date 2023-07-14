package com.woocommerce.android.ui.blaze

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
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
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.WCTextButton

@Composable
fun BlazeBanner(
    onClose: () -> Unit,
    onTryBlazeClicked: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(id = dimen.major_100)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onClose
            ) {
                Icon(
                    painter = painterResource(drawable.ic_gridicons_cross_grey_24dp),
                    contentDescription = stringResource(string.blaze_banner_close_button_content_description),
                )
            }
            Image(
                painter = painterResource(drawable.ic_blaze_banner_flame),
                contentDescription = ""
            )
            Text(
                modifier = Modifier.padding(top = dimensionResource(id = dimen.major_100)),
                text = stringResource(id = string.blaze_banner_title),
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Text(
                modifier = Modifier.padding(top = dimensionResource(id = dimen.minor_100)),
                text = stringResource(id = string.blaze_banner_description),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.subtitle1,
            )
            WCTextButton(
                modifier = Modifier.padding(top = dimensionResource(id = dimen.major_100)),
                onClick = onTryBlazeClicked
            ) {
                Text(
                    text = stringResource(string.blaze_banner_button).uppercase(),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
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
