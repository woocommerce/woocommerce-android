package com.woocommerce.android.ui.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun ExpandableTopBanner(
    title: String,
    message: String,
    buttons: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RectangleShape,
        modifier = modifier
    ) {
        var isExpanded by rememberSaveable { mutableStateOf(false) }

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(start = 48.dp, end = 16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 16.dp)
            ) {
                buttons()
            }
        }
    }
}

@Composable
fun ExpandableTopBanner(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExpandableTopBanner(
        title = title,
        message = message,
        buttons = {
            WCTextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(id = R.string.dismiss))
            }
        },
        modifier = modifier
    )
}

@Composable
@LightDarkThemePreviews
private fun ExpandableTopBannerPreview() {
    WooThemeWithBackground {
        ExpandableTopBanner(
            title = "Title",
            message = "Message",
            onDismiss = {},
        )
    }
}
