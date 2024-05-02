package com.woocommerce.android.ui.dashboard

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCOverflowMenu
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu

@Composable
fun WidgetCard(
    @StringRes titleResource: Int,
    modifier: Modifier = Modifier,
    menu: DashboardWidgetMenu,
    @DrawableRes iconResource: Int? = null,
    button: DashboardWidgetAction? = null,
    isError: Boolean,
    content: @Composable () -> Unit
) {
    val roundedShape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
    Column(
        modifier = modifier
            .border(
                width = dimensionResource(id = R.dimen.minor_10),
                color = colorResource(id = R.color.woo_gray_5),
                shape = roundedShape
            )
            .clip(roundedShape)
            .background(MaterialTheme.colors.surface)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isError) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tintable_info_outline_24dp),
                    contentDescription = "",
                    modifier = Modifier
                        .padding(start = dimensionResource(id = R.dimen.major_100))
                        .size(dimensionResource(id = R.dimen.major_125)),
                    tint = colorResource(id = R.color.color_icon)
                )
            } else if (iconResource != null) {
                Image(
                    painter = painterResource(id = iconResource),
                    contentDescription = "",
                    modifier = Modifier
                        .padding(start = dimensionResource(id = R.dimen.major_100))
                        .size(dimensionResource(id = R.dimen.major_125))
                )
            }
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(dimensionResource(id = R.dimen.major_100)),
                text = stringResource(id = titleResource),
                color = colorResource(id = R.color.color_on_surface_high),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            WCOverflowMenu(
                items = menu.items,
                onSelected = { item -> item.action() },
                mapper = { it.title.getText() },
                tint = colorResource(id = R.color.color_on_surface_high)
            )
        }

        content()

        if (button != null && !isError) {
            WCTextButton(
                modifier = Modifier
                    .padding(
                        start = dimensionResource(id = R.dimen.minor_100),
                        bottom = dimensionResource(id = R.dimen.minor_25)
                    ),
                onClick = button.action
            ) {
                Text(
                    text = button.title.getText(),
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewWidgetCard() {
    WooThemeWithBackground {
        WidgetCard(
            titleResource = R.string.blaze_campaign_title,
            iconResource = R.drawable.ic_blaze,
            menu = DashboardWidgetMenu(
                items = listOf(
                    DashboardWidgetAction(
                        titleResource = R.string.blaze_campaign_title,
                        action = {}
                    ),
                    DashboardWidgetAction(
                        titleResource = R.string.theme_preview_title,
                        action = {}
                    )
                )
            ),
            button = DashboardWidgetAction(
                titleResource = R.string.blaze_campaign_show_all_button,
                action = {}
            ),
            isError = false
        ) {
            Text(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)),
                text = "Content"
            )
        }
    }
}
