package com.woocommerce.android.ui.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun WCListItemWithInlineSubtitle(
    text: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    subtitleModifier: Modifier = Modifier,
    maxLines: Int = 1,
    showChevron: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle1,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = textModifier
                .padding(start = dimensionResource(id = R.dimen.major_100))
                .weight(1f)
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_on_surface_disabled),
            textAlign = TextAlign.End,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = subtitleModifier
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                .weight(0.75f),
        )

        if (showChevron) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null
            )
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ListItemPreviews() {
    WooThemeWithBackground {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            WCListItemWithInlineSubtitle(
                text = "List Item",
                subtitle = "List Item Subtitle"
            )

            WCListItemWithInlineSubtitle(
                text = "List Item with Chevron",
                subtitle = "List Item Subtitle",
                showChevron = true
            )
        }
    }
}
