package com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton

@Composable
fun EmptyPackages(
    modifier: Modifier = Modifier,
    @DrawableRes image: Int,
    @StringRes message: Int,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(id = image), contentDescription = null)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            stringResource(message),
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface,
        )
        Spacer(modifier = Modifier.height(37.dp))
        WCColoredButton(onClick = onButtonClick) {
            Text(stringResource(id = R.string.woo_shipping_labels_package_creation_empty_button))
        }
    }
}
