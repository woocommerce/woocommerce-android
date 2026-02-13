package com.woocommerce.android.ui.login.wpcom.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun WordPressWooBadgeHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_wordpress),
            contentDescription = null,
            tint = colorResource(R.color.wp_blue_50),
            modifier = Modifier.size(48.dp)
                .align(Alignment.CenterStart)
        )
        Image(
            painter = painterResource(id = R.drawable.ic_woo),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 38.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
                .size(52.dp)
                .align(Alignment.CenterEnd)
        )
    }
}

@Preview
@Composable
private fun WordPressWooBadgeHeaderPreview() {
    WooThemeWithBackground {
        WordPressWooBadgeHeader()
    }
}
