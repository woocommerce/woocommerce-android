package com.woocommerce.android.ui.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun AIProductDescriptionDialog(
    onTryNowButtonClick: () -> Unit,
    onMaybeLaterButtonClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = colorResource(id = R.color.color_surface)
            )
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(R.drawable.img_ai_dialog),
                contentDescription = "Generate AI Description",
            )
            Text(
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)),
                text = stringResource(id = R.string.ai_product_description_dialog_title),
                style = MaterialTheme.typography.h5,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)),
                text = stringResource(id = R.string.ai_product_description_dialog_message),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center
            )
            WCColoredButton(
                modifier = Modifier
                    .padding(top = dimensionResource(id = R.dimen.major_100))
                    .fillMaxWidth(),
                onClick = onTryNowButtonClick
            ) {
                Text(stringResource(id = R.string.try_it_now))
            }
            WCOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onMaybeLaterButtonClick
            ) {
                Text(stringResource(id = R.string.maybe_later))
            }
        }
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewAIProductDescriptionDialog() {
    WooThemeWithBackground {
        AIProductDescriptionDialog({}, {})
    }
}
