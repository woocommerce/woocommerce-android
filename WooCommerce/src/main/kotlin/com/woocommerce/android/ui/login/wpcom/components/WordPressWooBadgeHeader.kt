package com.woocommerce.android.ui.login.wpcom.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import com.woocommerce.android.R

@Composable
fun WordPressWooBadgeHeader(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val logoModifier = Modifier.size(dimensionResource(id = R.dimen.image_major_50))
        Image(
            painter = painterResource(id = R.drawable.ic_wordpress),
            contentDescription = null,
            modifier = logoModifier.padding(dimensionResource(id = R.dimen.minor_50))
        )
        Image(painter = painterResource(id = R.drawable.ic_connecting), contentDescription = null)
        Image(
            painter = painterResource(id = R.drawable.ic_woo),
            contentDescription = null,
            modifier = logoModifier.padding(dimensionResource(id = R.dimen.minor_50))
        )
    }
}
