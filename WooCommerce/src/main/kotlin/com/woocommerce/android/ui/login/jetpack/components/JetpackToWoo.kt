package com.woocommerce.android.ui.login.jetpack.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import com.woocommerce.android.R

@Composable
fun JetpackToWooHeader(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val logoModifier = Modifier.size(dimensionResource(id = R.dimen.image_minor_100))
        Image(
            painter = painterResource(id = R.drawable.ic_jetpack_logo),
            contentDescription = null,
            modifier = logoModifier
        )
        Image(painter = painterResource(id = R.drawable.ic_connecting), contentDescription = null)
        Image(
            painter = painterResource(id = R.drawable.ic_woo_bubble),
            contentDescription = null,
            modifier = logoModifier
        )
    }
}
