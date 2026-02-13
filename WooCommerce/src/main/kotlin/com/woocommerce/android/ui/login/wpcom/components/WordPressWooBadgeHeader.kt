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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun WordPressWooBadgeHeader(
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp
) {
    val borderWidth = iconSize * (2f / 48f)
    val wooIconSize = iconSize * (52f / 48f)
    val overlapStart = iconSize * (38f / 48f)

    Box(modifier = modifier) {
        Icon(
            painter = painterResource(id = R.drawable.ic_wordpress),
            contentDescription = null,
            tint = colorResource(R.color.wp_blue_50),
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.CenterStart)
        )
        Image(
            painter = painterResource(id = R.drawable.ic_woo),
            contentDescription = null,
            modifier = Modifier
                .padding(start = overlapStart)
                .border(
                    width = borderWidth,
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
                .size(wooIconSize)
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

@Preview
@Composable
private fun WordPressWooBadgeHeaderSmallPreview() {
    WooThemeWithBackground {
        WordPressWooBadgeHeader(iconSize = 24.dp)
    }
}
