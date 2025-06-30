package com.woocommerce.android.ui.orders.wooshippinglabels.packages.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun ErrorMessageWithButton(
    modifier: Modifier = Modifier,
    @StringRes message: Int = R.string.woo_shipping_labels_loading_error,
    onRetryClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Image(
            modifier = Modifier.size(87.dp),
            painter = painterResource(id = R.drawable.ic_error),
            contentDescription = stringResource(id = R.string.woopos_error_icon_content_description),
        )
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface,
        )
        WCTextButton(onClick = onRetryClick) {
            Text(stringResource(id = R.string.retry))
        }
    }
}

@Preview
@Composable
private fun ErrorMessageWithButtonPreview() = WooThemeWithBackground { ErrorMessageWithButton() }
